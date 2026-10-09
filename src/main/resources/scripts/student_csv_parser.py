import csv
import re
import unicodedata
from typing import Dict, Iterator, List, Optional, Tuple

PARTICLES = {
    "van", "de", "der", "den", "ter", "ten", "op", "over", "uit", "aan",
    "binnen", "boven", "onder", "door", "met", "voor", "na", "bij", "la",
    "le", "du", "del", "el", "al"
}


def normalize_key(value: Optional[str]) -> str:
    if value is None:
        return ""
    return value.strip().lower().replace(" ", "").replace("-", "").replace("_", "")


def first_present(row: Dict[str, str], keys: List[str]) -> str:
    for key in keys:
        value = row.get(key)
        if value is not None and str(value).strip():
            return str(value).strip()
    return ""


def clean_name(value: Optional[str]) -> str:
    if value is None:
        return ""
    return " ".join(str(value).strip().split())


def normalize_email_local_part(value: str) -> str:
    ascii_value = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode("ascii")
    return re.sub(r"[^a-z0-9]", "", ascii_value.lower())


def normalize_family_name(value: Optional[str]) -> str:
    if value is None:
        return ""

    text = " ".join(str(value).strip().split())

    if "," in text:
        left, right = [part.strip() for part in text.split(",", 1)]
        text = f"{right} {left}"

        tokens = text.split()
        if len(tokens) > 1:
            for index, token in enumerate(tokens[:-1]):
                if token.lower() in PARTICLES:
                    tokens[index] = token.lower()
            text = " ".join(tokens)

    return text


def split_full_name(value: str) -> Tuple[str, str]:
    text = clean_name(value)
    if not text:
        return "", ""

    parts = text.split()
    if len(parts) == 1:
        return parts[0], ""

    return parts[0], " ".join(parts[1:])


def normalize_student_record(raw_row: Dict[str, object]) -> Dict[str, str]:
    row = {
        normalize_key(str(key)): str(value).strip() if value is not None else ""
        for key, value in raw_row.items()
        if key is not None
    }

    full_name = clean_name(first_present(row, ["naamleerling", "volledigenaam", "fullname", "naam"]))
    first_name = clean_name(
        first_present(row, ["roepnaam", "voornaam", "firstname", "givenname"])
    )
    family_name = normalize_family_name(
        first_present(row, ["achternaam", "familienaam", "lastname", "surname", "familyname"])
    )

    if full_name and (not first_name or not family_name):
        derived_first, derived_family = split_full_name(full_name)
        if not first_name:
            first_name = derived_first
        if not family_name:
            family_name = derived_family

    if not first_name:
        raise ValueError(f"Missing first name in row: {raw_row}")

    if not family_name:
        family_name = "van Geenachternaam"

    student_number = first_present(row, ["leerlingnummer", "studentnummer", "nummer", "id"])

    if not student_number:
        student_number = ".".join(
            part for part in [
                normalize_email_local_part(first_name),
                *[normalize_email_local_part(part) for part in family_name.split() if part]
            ] if part
        )

    return {
        "studentNumber": student_number,
        "firstName": first_name,
        "familyName": family_name,
        "email": f"{student_number}@edictum.nl",
        "password": "Leerling01",
    }


def detect_dialect(sample: str) -> csv.Dialect:
    try:
        return csv.Sniffer().sniff(sample, delimiters=",;\t")
    except csv.Error:
        return csv.excel_tab


def read_student_csv(path: str) -> Iterator[Dict[str, str]]:
    encodings = ["utf-8-sig", "cp1252", "latin-1"]
    last_error = None

    for encoding in encodings:
        try:
            with open(path, "r", encoding=encoding, newline="") as csv_file:
                sample = csv_file.read(4096)
                csv_file.seek(0)

                first_nonempty = ""
                for line in sample.splitlines():
                    candidate = line.strip()
                    if candidate:
                        first_nonempty = candidate
                        break

                if first_nonempty and "," not in first_nonempty and ";" not in first_nonempty and "\t" not in first_nonempty:
                    csv_file.seek(0)
                    for line in csv_file:
                        name = line.strip()
                        if not name:
                            continue
                        if name.lower() in {"naam leerling", "naam", "leerlingnummer"}:
                            continue
                        yield normalize_student_record({
                            "naam leerling": name,
                            "leerlingnummer": ""
                        })
                    return

                dialect = detect_dialect(sample)
                reader = csv.DictReader(csv_file, dialect=dialect)
                for row in reader:
                    yield normalize_student_record(row)
            return
        except UnicodeDecodeError as exc:
            last_error = exc

    raise last_error or UnicodeError(f"Could not decode CSV file: {path}")