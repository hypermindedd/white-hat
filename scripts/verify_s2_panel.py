"""Verify the adopted, existing White Hat DOM/BFF source pin without changing the historical alpha."""
import hashlib
import json
from pathlib import Path

REQUIRED = {
    "apps/web-bff/public/index.html", "apps/web-bff/public/panel.js", "apps/web-bff/public/panel.css",
    "apps/second-mind/public/style.css", "apps/second-mind/public/icon.svg", "apps/web-bff/server.mjs",
}


def verify(root: Path) -> None:
    manifest = json.loads((root / "contracts/acceptance/s2-panel-source.json").read_text())
    if manifest["version"] != 1 or manifest["source_repository"] != "hypermindedd/white-hat":
        raise ValueError("PANEL_SOURCE_IDENTITY_INVALID")
    rows = manifest["files"]
    if len(rows) != len(REQUIRED) or {row["path"] for row in rows} != REQUIRED:
        raise ValueError("PANEL_SOURCE_INVENTORY_DRIFT")
    for row in rows:
        actual = hashlib.sha256((root / row["path"]).read_bytes()).hexdigest()
        if actual != row["sha256"]:
            raise ValueError(f"PANEL_SOURCE_HASH_DRIFT: {row['path']}")


if __name__ == "__main__":
    verify(Path(__file__).resolve().parents[1])
    print("PASS: adopted White Hat DOM/BFF source pin and preserved historical visual assets")
