#!/usr/bin/env python3
"""Offline knowledge-base consistency check; no API calls or personal data."""
import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
kb = root / 'knowledge'
index = (kb / 'INDEX.md').read_text(encoding='utf-8')
required = [
    '01_APK_EVIDENCE.md', '02_ARCHITECTURE.md', '03_API_RESEARCH.md',
    '04_FEATURE_MATRIX.md', '05_BUILD_AND_TEST.md', '06_DEFERRED_SECURITY_R8.md',
    '07_PROGRESS.md', '08_ROADMAP.md', '09_EXTERNAL_ANALYSIS_REVIEW.md',
    '10_TWO_REPORTS_RECONCILED.md', '11_VEHICLE_DATA_DICTIONARY.md',
    '12_STAGED_VALIDATION.md', 'evidence/apk-inventory.json',
    'evidence/apk-v2-signature.json', 'evidence/apk-vehicle-schema.json',
]
for rel in required:
    assert (kb / rel).is_file(), f'missing KB file: {rel}'
    assert f']({rel})' in index, f'index is missing: {rel}'
fields = json.loads((kb / 'evidence/apk-vehicle-schema.json').read_text(encoding='utf-8'))
assert fields['field_count'] == len(fields['fields']) == 33, 'vehicle model evidence mismatch'
assert len({r['name'] for r in fields['fields']}) == 33, 'duplicate field names'
dictionary = (kb / '11_VEHICLE_DATA_DICTIONARY.md').read_text(encoding='utf-8')
for item in fields['fields']:
    assert re.search(r'`' + re.escape(item['name']) + r'`', dictionary), f'undocumented vehicle field: {item["name"]}'
assert 'DEFERRED' in (kb / '06_DEFERRED_SECURITY_R8.md').read_text(encoding='utf-8')
signature = json.loads((kb / 'evidence/apk-v2-signature.json').read_text(encoding='utf-8'))
assert signature['apk_v2_signature'] == 'VERIFIED'
assert signature['apk_v2_content_digest'] == 'VERIFIED'
assert signature['file_sha256'] == json.loads((kb / 'evidence/apk-inventory.json').read_text(encoding='utf-8'))['sha256']
print('PASS: knowledge index, 33 field schema, signature evidence, deferred safety work')
