import json
from pathlib import Path
import shutil
import tempfile
import unittest
from verify_s2_panel import REQUIRED, verify


class PanelPinTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        source = Path(__file__).resolve().parents[1]
        for path in [*REQUIRED, "contracts/acceptance/s2-panel-source.json"]:
            target = self.root / path
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source / path, target)

    def test_code_drift_requires_an_explicit_new_source_pin(self):
        verify(self.root)
        with (self.root / "apps/web-bff/public/panel.js").open("a") as stream:
            stream.write("\n// unreviewed delta\n")
        with self.assertRaisesRegex(ValueError, "PANEL_SOURCE_HASH_DRIFT"):
            verify(self.root)

    def test_missing_or_duplicate_assets_cannot_make_the_pin_pass(self):
        path = self.root / "contracts/acceptance/s2-panel-source.json"
        data = json.loads(path.read_text())
        data["files"][-1] = data["files"][0]
        path.write_text(json.dumps(data))
        with self.assertRaisesRegex(ValueError, "PANEL_SOURCE_INVENTORY_DRIFT"):
            verify(self.root)


if __name__ == "__main__":
    unittest.main()
