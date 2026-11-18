"""The contract gate must fail closed even when Python optimization is enabled."""
import json
from pathlib import Path
import shutil
import subprocess
import sys
from tempfile import TemporaryDirectory
import unittest


class ApiSampleGateTest(unittest.TestCase):
    def test_missing_mode_samples_are_rejected_under_optimization(self):
        with TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "scripts").mkdir()
            (root / "contracts/openapi").mkdir(parents=True)
            samples = root / "server/api/build/contract-samples"
            samples.mkdir(parents=True)
            # A valid pilot sample must not conceal missing seven-mode receipts.
            (samples / "pilot-usage.json").write_text("{}")
            (root / "contracts/openapi/openapi.json").write_text(json.dumps({
                "components": {"schemas": {"Run": {}, "PilotUsage": {}}},
            }))
            script = root / "scripts/verify_api_samples.py"
            shutil.copyfile(Path(__file__).with_name("verify_api_samples.py"), script)
            result = subprocess.run([sys.executable, "-O", str(script)],
                                    capture_output=True, text=True, check=False)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("seven-mode", result.stderr)
