# Migration / restore scope

The original default web test skipped the PostgreSQL rehearsal. Its two non-DB tests passed against a synthetic SQLite snapshot: two mapped accounts/decisions/history entries and one quarantined entry per group, with no passwords or sessions copied.

`scripts/test_legacy_cutover.sh` makes two independent temporary PostgreSQL clusters, applies the versioned schema, and activates the existing optional Node rehearsal. The assertions include transaction rollback after insert, preserved post-cutover revision after dump/restore, unchanged source snapshot and non-BYPASSRLS cross-workspace denial. The Kotlin backup test also requires `WH_TEST_PG_BIN_DIR`; CI is configured to supply PostgreSQL 16 tools.

Pending until exact-HEAD receipts: successful drill execution. Production retention/consent, official identity/workspace mapping, failover, RPO/RTO thresholds, post-write recovery policy and authorized staging remain BLOCKED. No customer data or real database was touched. Existing runs remain historical archive data, not a second operational writer or billable ledger.

