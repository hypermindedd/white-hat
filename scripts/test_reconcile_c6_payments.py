import json
from pathlib import Path
import tempfile
import unittest

from reconcile_c6_payments import read_export, reconcile


class SettlementReconciliationTest(unittest.TestCase):
    def export(self, rows, end="2026-11-10"):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "export.json"
            path.write_text(json.dumps({
                "period_start_utc": "2026-11-01", "period_end_utc": end, "transactions": rows
            }))
            return read_export(path)

    def row(self, key, **changes):
        return {
            "transaction_id": key, "order_id": "synthetic-order", "kind": "capture", "currency": "USD",
            "amount_minor": 1000, "settled_day_utc": "2026-11-02", **changes,
        }

    def test_exact_integer_totals_per_currency_with_refund(self):
        rows = [self.row("cap"), self.row("refund", kind="refund", amount_minor=300), self.row("eur", currency="EUR")]
        report = reconcile(self.export(rows), self.export(list(reversed(rows))))
        self.assertEqual("matched", report["status"])
        self.assertEqual(3, report["matched_transactions"])
        self.assertEqual(700, report["ledger_totals"]["USD"]["net_minor"])
        self.assertEqual(1000, report["ledger_totals"]["EUR"]["net_minor"])

    def test_same_total_cannot_hide_transaction_mismatches(self):
        report = reconcile(
            self.export([self.row("a"), self.row("b"), self.row("x")]),
            self.export([self.row("a", amount_minor=900, order_id="different"),
                         self.row("b", amount_minor=1100), self.row("y")]),
        )
        self.assertEqual(report["ledger_totals"], report["provider_totals"])
        self.assertEqual("mismatch", report["status"])
        self.assertEqual(["x"], report["ledger_only"])
        self.assertEqual(["y"], report["provider_only"])
        self.assertEqual(["amount_minor", "order_id"], report["mismatches"][0]["fields"])

    def test_empty_or_misaligned_windows_do_not_pass(self):
        self.assertEqual("empty", reconcile(self.export([]), self.export([]))["status"])
        with self.assertRaises(ValueError):
            reconcile(self.export([]), self.export([], end="2026-11-11"))

    def test_rejects_duplicate_ids_invalid_amount_and_out_of_window_rows(self):
        for rows in (
            [self.row("x"), self.row("x")], [self.row("x", amount_minor=True)],
            [self.row("x", settled_day_utc="2026-11-10")], [self.row("x", card_number="forbidden")],
        ):
            with self.subTest(rows=rows), self.assertRaises(ValueError):
                self.export(rows)
