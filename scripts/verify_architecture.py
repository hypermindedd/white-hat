"""Enforce the versioned Kotlin module graph and BFF transport-storage boundary.

Static source/build checks, not proof against reflection or dynamic SQL. Historical
apps/second-mind is an intentionally preserved migration source, not a new writer.
"""
import argparse
import json
from pathlib import Path
import re


def verify(root: Path) -> list[str]:
    contract = json.loads((root / "contracts/architecture/module-boundaries.json").read_text())
    modules = contract["modules"]
    errors = []
    graph = {name: set() for name in modules}
    for name, spec in modules.items():
        folder = root / name
        build = folder / "build.gradle.kts"
        if not build.is_file():
            errors.append(f"Missing declared module: {name}")
            continue
        graph[name] = set(x.lstrip(":").replace(":", "/") for x in
                          re.findall(r'project\(["\'](:[\w:]+)["\']\)', build.read_text()))
        for target in sorted(graph[name] - set(spec["dependencies"])):
            errors.append(f"Forbidden Gradle dependency: {name} -> {target}")
        allowed = {modules[x]["namespace"] for x in [name, *spec["dependencies"]]}
        for source in folder.glob("src/**/*.kt"):
            if any(part == "test" or part.endswith("Test") for part in source.relative_to(folder).parts):
                continue
            text = re.sub(r'"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|//[^\n]*|/\*[\s\S]*?\*/', "", source.read_text())
            # Include fully qualified project references, not just import statements.
            refs = set(re.findall(r"\bwhitehat\.[A-Za-z_]\w*(?:\.[A-Za-z_*]\w*)*", text))
            for reference in refs:
                if not any(reference == prefix or reference.startswith(prefix + ".") for prefix in allowed):
                    errors.append(f"Forbidden Kotlin reference: {source.relative_to(root)} -> {reference}")
            imports = re.findall(r"^\s*import\s+([\w.]+)", text, flags=re.MULTILINE)
            for reference in imports:
                if any(reference == prefix or reference.startswith(prefix + ".") for prefix in spec["forbidden"]):
                    errors.append(f"Infrastructure leak: {source.relative_to(root)} -> {reference}")
    for base in ("server", "shared"):
        for build in (root / base).glob("*/build.gradle.kts"):
            if build.parent.relative_to(root).as_posix() not in modules:
                errors.append(f"Unregistered module: {build.parent.relative_to(root)}")
    active, done = set(), set()

    def walk(node: str) -> None:
        if node in active:
            errors.append(f"Dependency cycle through {node}")
            return
        if node in done:
            return
        active.add(node)
        for target in sorted(graph.get(node, set())):
            walk(target)
        active.remove(node)
        done.add(node)

    for node in graph:
        walk(node)
    for base in contract["client_roots"]:
        for source in (root / base).rglob("*.kt"):
            if "build" not in source.parts and re.search(r"\bwhitehat\.(api|adapters|application|domain)\b", source.read_text()):
                errors.append(f"Client imports server authority: {source.relative_to(root)}")
    for path in contract["bff_runtime"]:
        if re.search(r"(?:node:sqlite|better-sqlite3|second-mind/server|new\s+DatabaseSync)", (root / path).read_text()):
            errors.append(f"BFF imports a second business runtime: {path}")
    schema = (root / contract["bff_session_schema"]).read_text()
    tables = set(re.findall(r"CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?(\w+)", schema, re.IGNORECASE))
    if tables != set(contract["bff_session_tables"]):
        errors.append("BFF storage contract drift: only the declared authentication transport tables are allowed")
    return sorted(set(errors))


if __name__ == "__main__":
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    args = cli.parse_args()
    problems = verify(args.root)
    print("\n".join(problems) if problems else "PASS: Kotlin source/build boundaries, acyclic graph, client/server authority and BFF session-storage contract")
    raise SystemExit(1 if problems else 0)
