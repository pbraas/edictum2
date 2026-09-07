import argparse
import os
from typing import Iterator, Set

from google.oauth2 import service_account
from googleapiclient.discovery import build

from student_csv_parser import read_student_csv

SCOPES = ["https://www.googleapis.com/auth/admin.directory.user"]


def parse_args():
    parser = argparse.ArgumentParser(
        description="Suspend and move Google Workspace users not present in all_students.csv."
    )
    parser.add_argument("--csv", required=True, help="Path to all_students.csv")
    parser.add_argument(
        "--service-account-file",
        default=os.environ.get("GOOGLE_APPLICATION_CREDENTIALS"),
        help="Path to Google service account JSON file.",
    )
    parser.add_argument(
        "--delegated-admin",
        required=True,
        help="Google Workspace admin account to impersonate, e.g. admin@edictum.nl",
    )
    parser.add_argument(
        "--target-org-unit",
        default="/users die verwijderd moeten worden",
        help='OU path where users should be moved, e.g. "/users die verwijderd moeten worden"',
    )
    parser.add_argument(
        "--exclude-org-unit",
        action="append",
        default=["/medewerkers en docenten", "/uitzonderingen"],
        help="OU path to exclude (can be specified multiple times).",
    )
    parser.add_argument(
        "--domain",
        default="edictum.nl",
        help="Only process users in this primary email domain.",
    )
    parser.add_argument("--dry-run", action="store_true", help="Print actions only.")
    return parser.parse_args()


def normalize_ou(path: str) -> str:
    text = (path or "").strip()
    if not text:
        return "/"
    if not text.startswith("/"):
        text = f"/{text}"
    if len(text) > 1 and text.endswith("/"):
        text = text.rstrip("/")
    return text


def normalize_ou_for_match(path: str) -> str:
    return normalize_ou(path).lower()


def is_in_or_below_ou(user_ou: str, root_ou: str) -> bool:
    user = normalize_ou_for_match(user_ou)
    root = normalize_ou_for_match(root_ou)
    return user == root or user.startswith(root + "/")


def get_directory_service(service_account_file: str, delegated_admin: str):
    if not service_account_file:
        raise ValueError(
            "No service account file found. Set GOOGLE_APPLICATION_CREDENTIALS or pass --service-account-file."
        )

    credentials = service_account.Credentials.from_service_account_file(
        service_account_file, scopes=SCOPES
    )
    delegated_credentials = credentials.with_subject(delegated_admin)
    return build(
        "admin", "directory_v1", credentials=delegated_credentials, cache_discovery=False
    )


def read_active_student_emails(csv_path: str, domain: str) -> Set[str]:
    suffix = f"@{domain.lower()}"
    emails: Set[str] = set()
    for student in read_student_csv(csv_path):
        email = student["email"].strip().lower()
        if email.endswith(suffix):
            emails.add(email)
    return emails


def iter_users(service) -> Iterator[dict]:
    page_token = None
    while True:
        response = (
            service.users()
            .list(
                customer="my_customer",
                maxResults=500,
                orderBy="email",
                showDeleted=False,
                pageToken=page_token,
            )
            .execute()
        )
        for user in response.get("users", []):
            yield user

        page_token = response.get("nextPageToken")
        if not page_token:
            break


def main():
    args = parse_args()

    service = get_directory_service(args.service_account_file, args.delegated_admin)
    target_ou = normalize_ou(args.target_org_unit)
    excluded_ous = [normalize_ou(x) for x in args.exclude_org_unit]
    active_student_emails = read_active_student_emails(args.csv, args.domain)

    domain_suffix = f"@{args.domain.lower()}"

    scanned = 0
    skipped_excluded = 0
    skipped_in_csv = 0
    changed = 0

    for user in iter_users(service):
        scanned += 1
        email = (user.get("primaryEmail") or "").strip().lower()
        org_unit = user.get("orgUnitPath", "/")
        suspended = bool(user.get("suspended", False))

        if not email.endswith(domain_suffix):
            continue

        if any(is_in_or_below_ou(org_unit, ou) for ou in excluded_ous):
            skipped_excluded += 1
            continue

        if email in active_student_emails:
            skipped_in_csv += 1
            continue

        needs_move = normalize_ou(org_unit) != target_ou
        needs_suspend = not suspended

        if not needs_move and not needs_suspend:
            print(f"Already suspended in target OU: {email}")
            continue

        if args.dry_run:
            print(f"Would suspend+move: {email} | from OU: {org_unit} -> {target_ou}")
            changed += 1
            continue

        body = {"suspended": True, "orgUnitPath": target_ou}
        service.users().update(userKey=email, body=body).execute()
        print(f"Suspended+moved: {email}")
        changed += 1

    print(
        f"Done. Scanned={scanned}, ExcludedOU={skipped_excluded}, "
        f"InCSV={skipped_in_csv}, Changed={changed}"
    )


if __name__ == "__main__":
    main()