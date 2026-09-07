import argparse
import os
from typing import Iterator

from google.oauth2 import service_account
from googleapiclient.discovery import build

SCOPES = ["https://www.googleapis.com/auth/admin.directory.user"]


def parse_args():
    parser = argparse.ArgumentParser(
        description="Delete Google Workspace users in a specific OU."
    )
    parser.add_argument(
        "--service-account-file",
        default=os.environ.get("GOOGLE_APPLICATION_CREDENTIALS"),
        help="Path to service account JSON file.",
    )
    parser.add_argument(
        "--delegated-admin",
        required=True,
        help="Admin account to impersonate, e.g. admin@edictum.nl",
    )
    parser.add_argument(
        "--org-unit",
        default="/users die verwijderd moeten worden",
        help='OU path to delete from, e.g. "/users die verwijderd moeten worden"',
    )
    parser.add_argument(
        "--domain",
        default="edictum.nl",
        help="Only delete users in this email domain.",
    )
    parser.add_argument(
        "--include-sub-ous",
        action="store_true",
        help="Also delete users in sub-OUs under --org-unit.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Show which users would be deleted, without deleting.",
    )
    parser.add_argument(
        "--confirm-delete",
        action="store_true",
        help="Required to actually delete users (unless --dry-run).",
    )
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


def is_in_or_below_ou(user_ou: str, target_ou: str) -> bool:
    user = normalize_ou_for_match(user_ou)
    target = normalize_ou_for_match(target_ou)
    return user == target or user.startswith(target + "/")


def get_directory_service(service_account_file: str, delegated_admin: str):
    if not service_account_file:
        raise ValueError(
            "No service account file found. Set GOOGLE_APPLICATION_CREDENTIALS or pass --service-account-file."
        )
    credentials = service_account.Credentials.from_service_account_file(
        service_account_file,
        scopes=SCOPES,
    )
    delegated_credentials = credentials.with_subject(delegated_admin)
    return build("admin", "directory_v1", credentials=delegated_credentials, cache_discovery=False)


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

    if not args.dry_run and not args.confirm_delete:
        raise ValueError("Use --confirm-delete to actually delete users, or run with --dry-run.")

    service = get_directory_service(args.service_account_file, args.delegated_admin)
    target_ou = normalize_ou(args.org_unit)
    domain_suffix = f"@{args.domain.lower()}"

    scanned = 0
    matched = 0
    deleted = 0

    for user in iter_users(service):
        scanned += 1
        email = (user.get("primaryEmail") or "").strip().lower()
        user_ou = user.get("orgUnitPath", "/")

        if not email.endswith(domain_suffix):
            continue

        if args.include_sub_ous:
            ou_match = is_in_or_below_ou(user_ou, target_ou)
        else:
            ou_match = normalize_ou_for_match(user_ou) == normalize_ou_for_match(target_ou)

        if not ou_match:
            continue

        matched += 1

        if args.dry_run:
            print(f"Would delete: {email} | OU: {user_ou}")
            continue

        service.users().delete(userKey=email).execute()
        print(f"Deleted: {email}")
        deleted += 1

    print(f"Done. Scanned={scanned}, Matched={matched}, Deleted={deleted}")


if __name__ == "__main__":
    main()