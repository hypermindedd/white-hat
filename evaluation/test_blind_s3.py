"""Regression for integrity of receipts before blind human review."""
import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from uuid import uuid4

from blind_s3 import prepare
from prepare_s3 import digest


class BlindReceiptsTest(unittest.TestCase):
    def setUp(self):
        temp = TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        self.manifest = self.root / "manifest.json"
        self.manifest.write_text(json.dumps({"cases": [{"id": "G001", "turns": [{"prompt": "synthetic"}]}]}))
        self.arms = [self.root / "candidate", self.root / "baseline"]
        for arm in self.arms:
            arm.mkdir()
            workspace, decision, run = (str(uuid4()) for _ in range(3))
            (arm / "run.json").write_text(json.dumps({"workspace": workspace, "declared_model": arm.name,
                "execution_sha": "a" * 40, "cases": ["G001"], "manifest_sha256": digest(self.manifest)}))
            (arm / "G001-1.json").write_text(json.dumps({"id": "G001", "state": "terminal", "turn": 1,
                "decision_id": decision, "revision": 1, "run_id": run, "api_output": {"id": run,
                "workspace_id": workspace, "decision_id": decision, "input_revision": 1,
                "status": "succeeded", "result": {"answer": "synthetic"}}}))
        self.shared = self.root / "packet.json"
        self.private = self.root / "key.json"

    def run_prepare(self):
        prepare(self.manifest, *self.arms, self.shared, self.private)

    def test_anonymous_packet_excludes_identity_and_pins_receipt_hash(self):
        self.run_prepare()
        self.assertNotIn("execution_sha", self.shared.read_text())
        key = json.loads(self.private.read_text())
        for option in ("A", "B"):
            row = key["cases"][0][option]
            expected = self.root / row["arm"] / "G001-1.json"
            self.assertEqual(row["receipts"][0]["sha256"], digest(expected))
        self.assertEqual(self.private.stat().st_mode & 0o777, 0o600)
        self.assertEqual(key["evidence_kind"], "fixture")

    def test_cross_workspace_and_wrong_revision_do_not_enter_review(self):
        path = self.arms[0] / "G001-1.json"
        original = json.loads(path.read_text())
        for field, value in (("workspace_id", str(uuid4())), ("input_revision", 2)):
            row = json.loads(json.dumps(original))
            row["api_output"][field] = value
            path.write_text(json.dumps(row))
            with self.assertRaisesRegex(ValueError, "invalid or duplicate terminal receipt"):
                self.run_prepare()
            self.assertFalse(self.shared.exists())

    def test_same_workspace_does_not_count_as_separate_arms(self):
        path = self.arms[1] / "run.json"
        row = json.loads(path.read_text())
        row["workspace"] = json.loads((self.arms[0] / "run.json").read_text())["workspace"]
        path.write_text(json.dumps(row))
        with self.assertRaisesRegex(ValueError, "separate workspaces"):
            self.run_prepare()

    def test_paid_evidence_must_match_both_arms_and_the_confirmed_budget(self):
        evidence_path = self.root / "provider.json"
        policy = "b" * 64
        receipts = []
        for arm in self.arms:
            run = json.loads((arm / "run.json").read_text())
            path = arm / "G001-1.json"
            recorded = json.loads(path.read_text())
            recorded["api_output"].update(input_tokens=120, output_tokens=80)
            path.write_text(json.dumps(recorded))
            receipts.append({
                "run_id": recorded["run_id"], "workspace_id": run["workspace"],
                "model": run["declared_model"], "provider_request_id": f"receipt-{arm.name}",
                "execution_sha": run["execution_sha"], "engine_policy_sha256": policy,
                "input_tokens": 120, "output_tokens": 80, "confirmed_cost_usd_micros": 125000,
            })

        evidence = {"budget_usd_micros": 250000, "engine_policy_sha256": policy, "receipts": receipts}

        def prepare_packet():
            evidence_path.write_text(json.dumps(evidence))
            prepare(self.manifest, *self.arms, self.shared, self.private, evidence_path=evidence_path)

        for field, wrong, error in (
            ("model", "wrong-model", "does not match"),
            ("execution_sha", "c" * 40, "does not match"),
            ("engine_policy_sha256", "c" * 64, "does not match"),
            ("input_tokens", 121, "does not match"),
            ("confirmed_cost_usd_micros", 125001, "exceeded"),
            ("provider_request_id", receipts[1]["provider_request_id"], "unique"),
        ):
            with self.subTest(field=field):
                original = receipts[0][field]
                receipts[0][field] = wrong
                with self.assertRaisesRegex(ValueError, error):
                    prepare_packet()
                self.assertFalse(self.shared.exists())
                receipts[0][field] = original

        prepare_packet()
        key = json.loads(self.private.read_text())
        self.assertEqual(key["evidence_kind"], "provider_reconciled")
        self.assertEqual(key["evidence_sha256"], digest(evidence_path))
        self.assertNotIn("receipt-candidate", self.shared.read_text())
