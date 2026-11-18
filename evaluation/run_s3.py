"""Collect White Hat API outputs in an approved synthetic workspace."""
import argparse
import json
import os
import time
from pathlib import Path
from urllib.error import HTTPError
from urllib.parse import urlsplit
from urllib.request import HTTPRedirectHandler, Request, build_opener
from uuid import UUID

from prepare_s3 import digest, read_json, write_json

TERMINAL = {"succeeded", "failed", "cancelled", "reconciliation_required"}


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def run(manifest, args):
    token = os.environ.get("WH_S3_ACCESS_TOKEN")
    url = urlsplit(args.origin)
    if not token:
        raise ValueError("WH_S3_ACCESS_TOKEN is required")
    if (not url.hostname or url.username or url.password or url.path not in ("", "/")
            or url.query or url.fragment or not (url.scheme == "https" or (
                url.scheme == "http" and url.hostname in {"localhost", "127.0.0.1", "::1"}))):
        raise ValueError("Supply an HTTPS origin or local HTTP origin")
    workspace = str(UUID(args.workspace))
    api = f"{args.origin.rstrip('/')}/api/v1/workspaces/{workspace}"
    opener = build_opener(NoRedirect())

    def request(method, path, body=None, key=None):
        headers = {"Authorization": f"Bearer {token}"}
        if body is not None:
            headers["Content-Type"] = "application/json"
        if key:
            headers["Idempotency-Key"] = key
        payload = None if body is None else json.dumps(body).encode()
        try:
            with opener.open(Request(api + path, data=payload, method=method, headers=headers), timeout=25) as response:
                return json.load(response)
        except HTTPError as err:
            raise RuntimeError(f"{method} {path} returned HTTP {err.code}") from None

    args.out.mkdir(parents=True, exist_ok=False, mode=0o700)
    write_json(args.out / "run.json", {
        "manifest_sha256": digest(args.manifest),
        "execution_sha": args.execution_sha,
        "declared_model": args.model,
        "workspace": workspace,
        "cases": [c["id"] for c in manifest],
        "origin": args.origin,
        "max_submissions": args.max_submissions,
    })
    for case in manifest:
        cid = case["id"]
        for n, turn in enumerate(case["turns"], 1):
            if n == 1:
                decision = request("POST", "/decisions", turn)
            else:
                decision = request("PUT", f"/decisions/{decision['id']}", {
                    **turn, "expected_revision": decision["revision"],
                })
            decision_id = str(UUID(decision["id"]))
            key = f"golden-{cid}-{n}-{decision_id}"
            receipt_path = args.out / f"{cid}-{n}.json"
            receipt = {"id": cid, "turn": n, "decision_id": decision_id, "key": key,
                       "revision": decision["revision"], "state": "submission_pending"}
            write_json(receipt_path, receipt)
            start = time.monotonic()
            queued = request("POST", f"/decisions/{decision_id}/runs",
                             {"expected_revision": decision["revision"]}, key)
            run_id = str(UUID(queued["id"]))
            receipt.update(run_id=run_id, state="submitted")
            write_json(receipt_path, receipt)
            deadline = start + args.poll_timeout
            while time.monotonic() < deadline:
                result = request("GET", f"/runs/{run_id}")
                if result["status"] in TERMINAL:
                    break
                time.sleep(1)
            else:
                raise RuntimeError(f"{cid}/{n}: poll deadline elapsed for {run_id}")
            expected = {"id": run_id, "workspace_id": workspace, "decision_id": decision_id,
                        "input_revision": receipt["revision"]}
            if any(result.get(field) != value for field, value in expected.items()):
                raise RuntimeError(f"{cid}/{n}: terminal receipt identity mismatch for {run_id}")
            receipt.update(state="terminal", api_output=result,
                           decision_latency_ms=round((time.monotonic() - start) * 1000),
                           cost_usd_micros=None)
            write_json(receipt_path, receipt)
            if result["status"] != "succeeded":
                raise RuntimeError(f"{cid}/{n}: terminal state {result['status']}; inspect the saved receipt")
            print(f"{cid}/{n}: {run_id} succeeded", flush=True)


def main():
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument("manifest", type=Path)
    cli.add_argument("--case", action="append")
    cli.add_argument("--plan", action="store_true")
    cli.add_argument("--origin")
    cli.add_argument("--workspace")
    cli.add_argument("--model")
    cli.add_argument("--execution-sha")
    cli.add_argument("--out", type=Path)
    cli.add_argument("--max-submissions", type=int)
    cli.add_argument("--poll-timeout", default=120, type=int)
    args = cli.parse_args()
    cases = read_json(args.manifest)["cases"]
    wanted = set(args.case or [])
    if wanted - {case["id"] for case in cases}:
        cli.error("Unknown case ID")
    selected = [case for case in cases if not wanted or case["id"] in wanted]
    blocked = [case["id"] for case in selected if not case["turns"]]
    selected = [case for case in selected if case["turns"]]
    submissions = sum(len(case["turns"]) for case in selected)
    if args.plan:
        print(json.dumps({"cases": len(selected), "submissions_per_model": submissions,
                          "native_pending": blocked + [case["id"] for case in selected if "native_pending" in case]},
                         indent=2))
        return
    if (not all((args.origin, args.workspace, args.model, args.execution_sha, args.out, args.max_submissions))
            or not 0 < submissions <= args.max_submissions or args.poll_timeout <= 0):
        cli.error("Provide target, isolated workspace, model, SHA, new output directory, and a sufficient submission limit")
    if len(args.execution_sha) != 40 or any(ch not in "0123456789abcdef" for ch in args.execution_sha):
        cli.error("Expected a full Git SHA")
    try:
        run(selected, args)
    except (ValueError, RuntimeError, OSError) as err:
        cli.exit(2, f"Stopped: {err}\n")


if __name__ == "__main__":
    main()
