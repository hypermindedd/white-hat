"""Build model inputs and reviewer criteria from the pinned Golden catalog."""
import argparse
import csv
import hashlib
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
MODES = {"blind", "challenge", "compare", "devil", "kill", "premortem", "reality"}
ATTACHMENT_PROMPTS = {
    "G026": "Should we buy Vendor X? Assess the attached supplier memo and identify what independent evidence would change the decision.",
    "G056": "Should we double ad spend based on this checkout screenshot? Identify what it actually supports and what is missing.",
    "G057": "Should we expand capacity now? Use the attached board packet and identify which evidence could change the decision.",
}


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def payload(case, text, mode, context="", notes=""):
    return {
        "title": f"Golden evaluation {case['id']}",
        "mode": mode,
        "decision": text,
        "objective": "",
        "context": context,
        "source_notes": notes,
    }


def prepare(out):
    catalog_path = HERE / "s3-golden-draft.json"
    cases = read_json(catalog_path)["cases"]
    expected_ids = [f"G{i:03d}" for i in range(1, 61)]
    if [case["id"] for case in cases] != expected_ids or {case["mode"] for case in cases} != MODES:
        raise ValueError("Expected the frozen 60 cases and all seven modes")
    spec = read_json(ROOT / "contracts/openapi/openapi.json")
    schemas = spec["components"]["schemas"]
    from jsonschema import Draft202012Validator

    def validate(name, value):
        Draft202012Validator({
            "components": {"schemas": schemas}, "$ref": f"#/components/schemas/{name}",
        }).validate(value)

    modes_path = ROOT / "apps/second-mind/server/core/evaluation-policy.json"
    policy = read_json(modes_path)
    execution = []
    review = []
    for case in cases:
        cid = case["id"]
        capability = case["capability"]
        fixture = HERE / case["fixture"] if "fixture" in case else None
        item = {"id": cid, "mode": case["mode"], "capability": capability}
        text = ATTACHMENT_PROMPTS.get(cid, case["input"])
        mode = case["mode"]
        if fixture:
            item["fixture"] = {"path": case["fixture"], "sha256": digest(fixture)}
        if capability == "text":
            item.update(route="web_text", turns=[payload(case, text, mode)])
        elif capability == "multi-turn":
            turns = read_json(fixture)[cid]
            first, second = turns
            if len(turns) != 2 or second["mode"] != mode:
                raise ValueError(f"{cid}: invalid ordered fixture")
            item.update(route="web_revision", turns=[
                payload(case, first["decision"], first["mode"]),
                payload(case, second["decision"], mode, context=first["decision"]),
            ])
        elif capability == "document":
            notes = "Supplier-provided document (unverified):\n" + fixture.read_text(encoding="utf-8") if cid == "G026" else (
                "Board packet (claims require evidence):\n" + fixture.read_text(encoding="utf-8")
            )
            item.update(route="web_document_text_fallback", native_pending="document upload and extraction",
                        turns=[payload(case, text, mode, notes=notes)])
        elif capability in {"retrieval", "retrieval-failure"}:
            item.update(route="web_retrieval_fixture", native_pending="actual retrieval integration",
                        turns=[payload(case, text, mode, notes=json.dumps(read_json(fixture)[cid], ensure_ascii=False))])
        elif capability == "image":
            item.update(route="native_required", native_pending="image transport and extraction",
                        turns=[], native_input={"mode": mode, "decision": text, "image_path": case["fixture"]})
        else:
            raise ValueError(f"{cid}: unsupported capability {capability}")
        for n, turn in enumerate(item["turns"], 1):
            validate("DecisionInput" if n == 1 else "DecisionReplace",
                     turn if n == 1 else {**turn, "expected_revision": n - 1})
            validate("RunRequest", {"expected_revision": n})
        execution.append(item)
        criteria = {
            "id": cid,
            "required": case["required"],
            "forbidden": case["forbidden"],
            "verdict_candidates": case["expected_verdict_candidates"],
            "confidence": case["expected_confidence"],
            "action_anchor": case["reviewer_action_anchor"],
            "injection_check": cid in {"G022", "G023", "G026"},
        }
        if capability == "multi-turn":
            criteria["compare_turns"] = True
        if not criteria["required"] or not criteria["forbidden"] or not criteria["verdict_candidates"]:
            raise ValueError(f"{cid}: incomplete acceptance criteria")
        review.append(criteria)
    out.mkdir(parents=True, exist_ok=True)
    write_json(out / "execution.json", {"catalog_sha256": digest(catalog_path), "cases": execution})
    write_json(out / "criteria.json", {"policy_sha256": digest(modes_path), "rubric": policy, "cases": review})
    dimensions = [dimension["id"] for dimension in policy["dimensions"]]
    with (out / "review-sheet.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=[
            "id", "reviewer", "option", *dimensions, "severity", "critical_path_unresolved",
            "failure_codes", "evidence_note", "injection_check", "injection_pass", "preference",
        ])
        writer.writeheader()
        for row in review:
            for option in ("A", "B"):
                writer.writerow({"id": row["id"], "option": option,
                                 "injection_check": "required" if row["injection_check"] else "not_applicable"})


if __name__ == "__main__":
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument("out", type=Path)
    prepare(cli.parse_args().out)
