"""Prepare an anonymous comparison from two successful evaluation runs."""
import argparse
import secrets
import re
from pathlib import Path
from uuid import UUID

from prepare_s3 import digest, read_json, write_json


def provider_receipts(evidence):
    if type(evidence["budget_usd_micros"]) is not int or evidence["budget_usd_micros"] <= 0:
        raise ValueError("A positive evaluation budget in USD micros is required")
    if not re.fullmatch(r"[0-9a-f]{64}", evidence["engine_policy_sha256"]):
        raise ValueError("Pin the deployed engine policy")
    receipts = {}
    provider_ids = set()
    cost = 0
    for item in evidence["receipts"]:
        run_id = str(UUID(item["run_id"]))
        provider_id = item["provider_request_id"]
        if (not isinstance(provider_id, str) or not provider_id.strip() or provider_id in provider_ids
                or run_id in receipts):
            raise ValueError("Provider and run identifiers must be nonempty and unique")
        for field in ("input_tokens", "output_tokens", "confirmed_cost_usd_micros"):
            if type(item[field]) is not int or item[field] < 0:
                raise ValueError(f"Invalid provider receipt field: {field}")
        cost += item["confirmed_cost_usd_micros"]
        provider_ids.add(provider_id)
        receipts[run_id] = item
    if cost > evidence["budget_usd_micros"]:
        raise ValueError("Confirmed execution costs exceeded the evaluation budget")
    return receipts


def prepare(manifest_path, candidate, baseline, shared, private, *, evidence_path=None):
    manifest = read_json(manifest_path)
    runs = {name: read_json(path / "run.json") for name, path in (("candidate", candidate), ("baseline", baseline))}
    for run in runs.values():
        UUID(run["workspace"])
        if not re.fullmatch(r"[0-9a-f]{40}", run["execution_sha"]) or not run["declared_model"].strip():
            raise ValueError("Pin the execution SHA and model for both arms")
    if runs["candidate"]["workspace"] == runs["baseline"]["workspace"]:
        raise ValueError("Use separate workspaces for the two evaluation arms")
    evidence = read_json(evidence_path) if evidence_path else None
    corroborating = provider_receipts(evidence) if evidence is not None else {}
    case_ids = runs["candidate"]["cases"]
    if (case_ids != runs["baseline"]["cases"] or not case_ids or len(case_ids) != len(set(case_ids))
            or any(run["manifest_sha256"] != digest(manifest_path) for run in runs.values())):
        raise ValueError("Both runs must use the same pinned manifest and case selection")
    cases = {case["id"]: case for case in manifest["cases"]}
    public_rows = []
    identities = []
    seen = set()
    for cid in case_ids:
        case = cases[cid]
        if not case["turns"]:
            raise ValueError(f"{cid}: no executable turns in the manifest")
        public_row = {"id": cid, "input_turns": case["turns"], "options": {}}
        identity = {"id": cid}
        arms = [("candidate", candidate), ("baseline", baseline)]
        if secrets.randbelow(2):
            arms.reverse()
        for label, (arm, directory) in zip(("A", "B"), arms, strict=True):
            outputs = []
            receipts = []
            decision_id = None
            previous_revision = 0
            for n, _ in enumerate(case["turns"], 1):
                path = directory / f"{cid}-{n}.json"
                row = read_json(path)
                output = row["api_output"]
                run_id = row["run_id"]
                if (row["id"] != cid or row["turn"] != n or row["state"] != "terminal"
                        or output["id"] != run_id or output["workspace_id"] != runs[arm]["workspace"]
                        or output["decision_id"] != row["decision_id"] or output["input_revision"] != row["revision"]
                        or output["status"] != "succeeded" or output["result"] is None or run_id in seen):
                    raise ValueError(f"{cid}/{n}: invalid or duplicate terminal receipt")
                if (decision_id is not None and decision_id != row["decision_id"]) or row["revision"] <= previous_revision:
                    raise ValueError(f"{cid}/{n}: decision or revision discontinuity")
                decision_id = row["decision_id"]
                previous_revision = row["revision"]
                seen.add(run_id)
                outputs.append(output["result"])
                receipt = {"run_id": run_id, "sha256": digest(path)}
                if evidence is not None:
                    provider = corroborating.pop(run_id, None)
                    expected = {
                        "workspace_id": runs[arm]["workspace"],
                        "model": runs[arm]["declared_model"],
                        "execution_sha": runs[arm]["execution_sha"],
                        "engine_policy_sha256": evidence["engine_policy_sha256"],
                        "input_tokens": output.get("input_tokens"),
                        "output_tokens": output.get("output_tokens"),
                    }
                    if provider is None or any(provider.get(k) != v for k, v in expected.items()):
                        raise ValueError(f"{cid}/{n}: provider evidence does not match the API receipt")
                    receipt["provider_evidence"] = provider
                receipts.append(receipt)
            public_row["options"][label] = outputs
            identity[label] = {"arm": arm, "receipts": receipts}
        public_rows.append(public_row)
        identities.append(identity)
    if corroborating:
        raise ValueError("Provider evidence includes runs outside the selected comparison")
    for path in (shared, private):
        if path.exists():
            raise ValueError(f"Output already exists: {path}")
    if shared.resolve() == private.resolve():
        raise ValueError("Use distinct paths for the comparison and its key")
    metadata = {"evidence_kind": "fixture" if evidence is None else "provider_reconciled"}
    if evidence is not None:
        metadata.update(evidence_sha256=digest(evidence_path), budget_usd_micros=evidence["budget_usd_micros"],
                        engine_policy_sha256=evidence["engine_policy_sha256"])
    for path, content, permissions in (
        (shared, {"cases": public_rows}, 0o644),
        (private, {**metadata, "runs": runs, "cases": identities}, 0o600),
    ):
        path.touch(mode=permissions, exist_ok=False)
        write_json(path, content)


if __name__ == "__main__":
    cli = argparse.ArgumentParser(description=__doc__)
    for name in ("manifest", "candidate", "baseline", "reviewer_packet", "private_key"):
        cli.add_argument(name, type=Path)
    source = cli.add_mutually_exclusive_group(required=True)
    source.add_argument("--provider-evidence", type=Path, help="Private receipt export for a real comparison")
    source.add_argument("--fixture", action="store_true", help="Prepare an offline dry-run packet")
    args = cli.parse_args()
    try:
        prepare(args.manifest, args.candidate, args.baseline, args.reviewer_packet, args.private_key,
                evidence_path=args.provider_evidence)
    except (ValueError, OSError, KeyError, TypeError) as err:
        cli.exit(2, f"Cannot prepare comparison: {err}\n")
