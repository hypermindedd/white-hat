"""Check actual Ktor test responses against the published OpenAPI schemas."""
import json
from pathlib import Path
from jsonschema import Draft202012Validator, FormatChecker

root = Path(__file__).resolve().parents[1]
api = json.loads((root / "contracts/openapi/openapi.json").read_text())
schema = {"$ref": "#/components/schemas/Run", "components": api["components"]}
validator = Draft202012Validator(schema, format_checker=FormatChecker())
files = sorted((root / "server/api/build/contract-samples").glob("run-*.json"))
assert len(files) == 7, "Run the seven-mode Ktor/PostgreSQL integration test first"
for path in files:
    validator.validate(json.loads(path.read_text()))
print("7 actual Ktor run responses validate against OpenAPI; AI was fixture-only.")
