"""Compare normalized settled captures and refunds for the same UTC window."""

import argparse
from collections import defaultdict
from datetime import date
import json
from pathlib import Path
import re
import sys

WINDOW_FIELDS = {"period_start_utc", "period_end_utc", "transactions"}
ROW_FIELDS = {"transaction_id", "order_id", "kind", "currency", "amount_minor", "settled_day_utc"}
ID = re.compile(r"[A-Za-z0-9._:-]{1,120}")
CURRENCY = re.compile(r"[A-Z]{3}")


def unique_keys(entries):
    result = {}
    for key, value in entries:
        if key in result:
            raise ValueError("duplicate JSON field")
        result[key] = value
    return result


def iso_date(value):
    if not isinstance(value, str):
        raise ValueError("UTC date must use YYYY-MM-DD")
    parsed = date.fromisoformat(value)
    if parsed.isoformat() != value:
        raise ValueError("UTC date must use YYYY-MM-DD")
    return parsed


def read_export(path):
    with path.open(encoding="utf-8") as stream:
        data = json.load(stream, object_pairs_hook=unique_keys)
    if not isinstance(data, dict) or set(data) != WINDOW_FIELDS:
        raise ValueError("invalid export fields")
    start, end = (iso_date(data[key]) for key in ("period_start_utc", "period_end_utc"))
    if start >= end or not isinstance(data["transactions"], list):
        raise ValueError("invalid export window or transactions")
    rows = {}
    for row in data["transactions"]:
        if not isinstance(row, dict) or set(row) != ROW_FIELDS:
            raise ValueError("invalid transaction fields")
        transaction, order, kind, currency, amount, day = (row[key] for key in (
            "transaction_id", "order_id", "kind", "currency", "amount_minor", "settled_day_utc"
        ))
        if not all(isinstance(value, str) and ID.fullmatch(value) for value in (transaction, order)):
            raise ValueError("invalid transaction or order identifier")
        if kind not in ("capture", "refund") or not isinstance(currency, str) or not CURRENCY.fullmatch(currency):
            raise ValueError("invalid transaction kind or currency")
        if type(amount) is not int or amount <= 0 or not start <= iso_date(day) < end:
            raise ValueError("invalid amount or settlement date outside the window")
        if transaction in rows:
            raise ValueError("duplicate transaction identifier")
        rows[transaction] = row
    return (start.isoformat(), end.isoformat()), rows


def totals(rows):
    amounts = defaultdict(lambda: {"captured_minor": 0, "refunded_minor": 0})
    for row in rows.values():
        key = "captured_minor" if row["kind"] == "capture" else "refunded_minor"
        amounts[row["currency"]][key] += row["amount_minor"]
    return {
        currency: {**values, "net_minor": values["captured_minor"] - values["refunded_minor"]}
        for currency, values in sorted(amounts.items())
    }


def reconcile(ledger_export, provider_export):
    window, ledger = ledger_export
    provider_window, provider = provider_export
    if window != provider_window:
        raise ValueError("export windows differ")
    ledger_only = sorted(ledger.keys() - provider.keys())
    provider_only = sorted(provider.keys() - ledger.keys())
    common = ledger.keys() & provider.keys()
    mismatches = [
        {"transaction_id": key, "fields": sorted(field for field in ROW_FIELDS if ledger[key][field] != provider[key][field])}
        for key in sorted(common) if ledger[key] != provider[key]
    ]
    return {
        "period_start_utc": window[0],
        "period_end_utc": window[1],
        "status": "mismatch" if ledger_only or provider_only or mismatches else "empty" if not common else "matched",
        "matched_transactions": len(common) - len(mismatches),
        "ledger_only": ledger_only,
        "provider_only": provider_only,
        "mismatches": mismatches,
        "ledger_totals": totals(ledger),
        "provider_totals": totals(provider),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("ledger", type=Path)
    parser.add_argument("provider", type=Path)
    args = parser.parse_args()
    try:
        result = reconcile(read_export(args.ledger), read_export(args.provider))
    except (ValueError, OSError) as failure:
        print(f"Export validation failed: {failure}", file=sys.stderr)
        return 2
    print(json.dumps(result, indent=2))
    return 0 if result["status"] == "matched" else 1


if __name__ == "__main__":
    sys.exit(main())
