import argparse
import os
import re
import unicodedata

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError

from student_csv_parser import read_student_csv

SCOPES = ["https://www.googleapis.com/auth/admin.directory.user"]
EMAIL_DOMAIN = "edictum.nl"


def parse_args():
    parser = argparse.ArgumentParser(description="Create Google Workspace student accounts from CSV.")
    parser.add_argument("--csv", required=True, help="Path to the student CSV file.")
    parser.add_argument(
        "--service-account-file",
        default=os.environ.get("GOOGLE_APPLICATION_CREDENTIALS"),
        help="Path to your Google service account JSON file."
    )
    parser.add_argument(
        "--delegated-admin",
        required=True,
        help="Google Workspace admin account to impersonate, e.g. admin@edictum.nl"
    )
    parser.add_argument("--org-unit", default="/", help="Google Workspace org unit path.")
    parser.add_argument("--dry-run", action="store_true", help="Only print actions.")
    parser.add_argument("--update-existing", action="store_true", help="Update existing users if found.")
    return parser.parse_args()


def get_directory_service(service_account_file: str, delegated_admin: str):
    if not service_account_file:
        raise ValueError("No service account file found. Set GOOGLE_APPLICATION_CREDENTIALS or pass --service-account-file.")

    credentials = service_account.Credentials.from_service_account_file(
        service_account_file,
        scopes=SCOPES
    )
    delegated_credentials = credentials.with_subject(delegated_admin)
    return build("admin", "directory_v1", credentials=delegated_credentials, cache_discovery=False)


def user_exists(service, email: str) -> bool:
    try:
        service.users().get(userKey=email).execute()
        return True
    except HttpError as exc:
        if exc.resp.status == 404:
            return False
        raise


def normalize_login_part(value: str) -> str:
    ascii_value = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode("ascii")
    return re.sub(r"[^a-z0-9]", "", ascii_value.lower())


def build_login_email(student: dict) -> str:
    full_name = f"{student['firstName']} {student['familyName']}"
    raw_parts = full_name.replace("-", " ").replace("'", " ").split()
    login_parts = [normalize_login_part(part) for part in raw_parts]
    login_parts = [part for part in login_parts if part]

    if not login_parts:
        raise ValueError(f"Cannot build login email from name: {full_name}")

    return f"{'.'.join(login_parts)}@{EMAIL_DOMAIN}"


def build_user_payload(student: dict, org_unit: str, email: str) -> dict:
    return {
        "name": {
            "givenName": student["firstName"],
            "familyName": student["familyName"],
            "fullName": f"{student['firstName']} {student['familyName']}"
        },
        "primaryEmail": email,
        "password": student["password"],
        "changePasswordAtNextLogin": True,
        "orgUnitPath": org_unit
    }


def main():
    args = parse_args()
    service = get_directory_service(args.service_account_file, args.delegated_admin)

    for student in read_student_csv(args.csv):
        email = build_login_email(student)

        if args.dry_run:
            print(f"Would create/update user: {email} | {student['firstName']} {student['familyName']} | temp password = {student['password']}")
            continue

        if user_exists(service, email):
            if not args.update_existing:
                print(f"Skipping existing user: {email}")
                continue

            payload = {
                "name": {
                    "givenName": student["firstName"],
                    "familyName": student["familyName"],
                    "fullName": f"{student['firstName']} {student['familyName']}"
                },
                "password": student["password"],
                "changePasswordAtNextLogin": True,
                "orgUnitPath": args.org_unit
            }
            service.users().update(userKey=email, body=payload).execute()
            print(f"Updated user: {email}")
            continue

        payload = build_user_payload(student, args.org_unit, email)
        service.users().insert(body=payload).execute()
        print(f"Created user: {email}")

    print("Done.")


if __name__ == "__main__":
    main()