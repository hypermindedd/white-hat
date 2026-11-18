"""Supplement the unchanged project quality contract with review coverage.
Usage: PYTHONPATH=/path/to/pinned/engine/src python3 scripts/run_quality_review.py
This does not alter .qa/pqe.toml or turn incomplete acceptance gates into PASS.
"""
from pathlib import Path
import argparse
import sys
from pqe.config import CommandSpec, load_config
from pqe.orchestrator import RunOptions, run_pipeline

cli = argparse.ArgumentParser(description=__doc__)
cli.add_argument('--out', type=Path, default=Path('.qa/out/extended-review'))
args = cli.parse_args()
root = Path(__file__).resolve().parents[1]
output = (root / args.out).resolve()
output.mkdir(parents=True, exist_ok=True)
config = load_config(root)
config.architecture.python_roots = ['evaluation', 'scripts']
config.security.bandit_paths = [str(p.relative_to(root)) for folder in ['evaluation', 'scripts']
                               for p in (root / folder).glob('*.py') if not p.name.startswith('test_')]
config.security.npm_audit_dirs = ['apps/web-bff']
config.checksum_files = ['CHECKSUMS.sha256']
config.docs_check.files = ['KOTLIN-START-HERE-FA.md', 'apps/web-bff/README-FA.md',
                          'server/README-FA.md', 'evaluation/README-C3.md', 'docs/stages/S2-CLOSURE-FA.md']
for command in config.commands:
    command.timeout_s = max(command.timeout_s, 900)
config.commands.extend([
    CommandSpec(id='recovered-alpha', title='Recovered alpha regression', argv=['npm', 'test'], cwd='apps/second-mind'),
    CommandSpec(id='financial-and-contract-tools', title='Accounting and optimized contract-gate regressions',
                argv=[sys.executable, '-B', '-m', 'unittest', 'discover', '-s', 'scripts', '-p', 'test_*.py']),
    CommandSpec(id='golden-input-preparation', title='Validate 60-case golden inputs without any model call',
                argv=[sys.executable, 'evaluation/prepare_s3.py', str(output / 'golden-inputs')]),
])
report = run_pipeline(config, RunOptions(out_dir=output,
    fail_on='conditional', extra_report={'review_scope': 'supplemental; original quality contract unchanged',
    'engine_sha': '67f3693c5ca3593a1a1eb52f3de54aac40be1cb2'}))
print(report['decision'])
raise SystemExit(0 if report['decision']['decision'] == 'GO' else 1)
