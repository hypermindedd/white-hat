import json
from pathlib import Path
import shutil
import tempfile
import unittest
from verify_architecture import verify


class BoundaryMutationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        actual = Path(__file__).resolve().parents[1]
        contract = actual / "contracts/architecture/module-boundaries.json"
        target = self.root / "contracts/architecture/module-boundaries.json"
        target.parent.mkdir(parents=True)
        shutil.copyfile(contract, target)
        self.contract = json.loads(target.read_text())
        for module, spec in self.contract["modules"].items():
            folder = self.root / module
            folder.mkdir(parents=True)
            (folder / "build.gradle.kts").write_text("")
            source = folder / "src/main/kotlin/Example.kt"
            source.parent.mkdir(parents=True)
            source.write_text(f'package {spec["namespace"]}\n')
        for path in self.contract["bff_runtime"]:
            file = self.root / path
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_text("")
        schema = self.root / self.contract["bff_session_schema"]
        schema.parent.mkdir(parents=True)
        schema.write_text("\n".join(f"CREATE TABLE {name} (id text);" for name in self.contract["bff_session_tables"]))

    def test_domain_cannot_import_or_directly_reference_adapter(self):
        self.assertEqual([], verify(self.root))
        source = self.root / "server/domain/src/main/kotlin/Example.kt"
        for reference in ("import whitehat.adapters.PostgresStore", "fun leak() = whitehat.adapters.PostgresStore()"):
            source.write_text("package whitehat.domain\n" + reference)
            self.assertTrue(any("Forbidden Kotlin reference" in x for x in verify(self.root)))

    def test_infrastructure_and_cyclic_build_dependencies_are_blocked(self):
        (self.root / "server/domain/src/main/kotlin/Example.kt").write_text("import java.sql.Connection")
        (self.root / "server/domain/build.gradle.kts").write_text('implementation(project(":server:api"))')
        (self.root / "server/api/build.gradle.kts").write_text('implementation(project(":server:domain"))')
        errors = verify(self.root)
        for marker in ("Infrastructure leak", "Forbidden Gradle dependency", "Dependency cycle"):
            self.assertTrue(any(marker in x for x in errors), errors)

    def test_clients_and_bff_cannot_add_a_second_business_authority(self):
        client = self.root / "apps/desktop/Fake.kt"
        client.parent.mkdir(parents=True)
        client.write_text("import whitehat.domain.Decision")
        (self.root / self.contract["bff_runtime"][0]).write_text("import {DatabaseSync} from 'node:sqlite';")
        with (self.root / self.contract["bff_session_schema"]).open("a") as stream:
            stream.write("CREATE TABLE decisions (id text);")
        errors = verify(self.root)
        for marker in ("Client imports", "second business runtime", "storage contract drift"):
            self.assertTrue(any(marker in x for x in errors), errors)

    def test_unknown_core_modules_require_an_explicit_contract(self):
        extra = self.root / "server/parallel/build.gradle.kts"
        extra.parent.mkdir(parents=True)
        extra.write_text("")
        self.assertTrue(any("Unregistered module" in x for x in verify(self.root)))


if __name__ == "__main__":
    unittest.main()
