#!/usr/bin/env python3
"""Verify migrated source integrity and local guide links; not runtime quality."""
import hashlib
import json
from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parents[1]
manifest = json.loads((root / 'docs/source-manifest.json').read_text())
errors = []
sources = manifest['sources']
if len(sources) != 17 or len({s['page_id'] for s in sources}) != 17:
    errors.append('Expected 17 distinct source pages')
if len({s['path'] for s in sources}) != 17:
    errors.append('Duplicate source paths')
for source in sources:
    path = root / source['path']
    if not path.is_file():
        errors.append(f'Missing source: {path}')
        continue
    raw = path.read_bytes()
    if hashlib.sha256(raw).hexdigest() != source['sha256']:
        errors.append(f'Source hash mismatch: {path}')
    if len(raw) != source['size_bytes']:
        errors.append(f'Source size mismatch: {path}')
    if source['page_id'].replace('-', '') not in raw.decode():
        errors.append(f'Source identity missing: {path}')
for line in (root / 'CHECKSUMS.sha256').read_text().splitlines():
    digest, rel = line.split('  ', 1)
    path = root / rel
    if not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest() != digest:
        errors.append(f'File checksum mismatch: {rel}')
for path in root.rglob('*.md'):
    if 'sources' in path.parts:
        continue  # Native Notion markup is intentionally preserved.
    for target in re.findall(r'\]\(([^)]+)\)', path.read_text()):
        if '://' in target or target.startswith('#'):
            continue
        local = target.split('#')[0]
        if not (path.parent / local).is_file():
            errors.append(f'Broken link: {path.relative_to(root)} -> {target}')
if errors:
    print('\n'.join(errors))
    sys.exit(1)
print('PASS: 17 source identities/hashes, file checksums and local guide links.')
print('Scope: documentation integrity only; runtime NOT TESTED.')
