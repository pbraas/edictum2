import csv
from typing import Dict, Iterable, Iterator, List, Optional


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


def normalize_student_record(raw_row: Dict[str, object]) -> Dict[str, str]:
    row = {
        normalize_key(str(key)): str(value).strip() if value is not None else ""
        for key, value in raw_row.items()
        if key is not None
    }

    student_number = first_present(row, ["leerlingnummer", "studentnummer", "nummer", "id"])
    if not student_number:
        raise ValueError(f"Missing student number in row: {raw_row}")

    first_name = clean_name(
        first_present(row, ["roepnaam", "naam", "voornaam", "firstname", "givenname"])
    ) or clean_name(first_present(row, ["naam", "voornaam"]))
    if not first_name:
        raise ValueError(f"Missing first name in row: {raw_row}")

    family_name = normalize_family_name(
        first_present(row, ["achternaam", "familienaam", "lastname", "surname", "familyname"])
    )
    if not family_name:
        raise ValueError(f"Missing family name in row: {raw_row}")

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
    with open(path, "r", encoding="utf-8-sig", newline="") as csv_file:
        sample = csv_file.read(4096)
        csv_file.seek(0)
        dialect = detect_dialect(sample)
        reader = csv.DictReader(csv_file, dialect=dialect)

        for row in reader:
            yield normalize_student_record(row)