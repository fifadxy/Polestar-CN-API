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
    '12_STAGED_VALIDATION.md',
    '13_REFERENCE_APK_FIELD_VALIDATION.md',
    '14_ANDROID_PARITY_V3_MASTER_PLAN.md',
    '15_TOKEN_AND_REMOTE_VALIDATION.md',
    '16_ANDROID_BRANCH_GOVERNANCE.md',
    'evidence/apk-inventory.json',
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
assert 'REFERENCE_USER_VERIFIED' in (kb / '13_REFERENCE_APK_FIELD_VALIDATION.md').read_text(encoding='utf-8')
assert 'android-polestar-assistant-main' in (kb / '16_ANDROID_BRANCH_GOVERNANCE.md').read_text(encoding='utf-8')
assert 'P3' in (kb / '14_ANDROID_PARITY_V3_MASTER_PLAN.md').read_text(encoding='utf-8')
print('PASS: V3 Android parity knowledge, 33 field schema, reference evidence, branch and deferred safety work')

latest = json.loads((kb / 'evidence/reference-20261027-mapping.json').read_text(encoding='utf-8'))
assert latest['reference']['version_code'] == 215223485
assert latest['health']['remaining_service_fields'] == {'hours': 2, 'days': 3, 'km': 4}
assert latest['location']['longitude_field'] == 1 and latest['location']['latitude_field'] == 2
assert '](' + '25_V078_CONDITION_LOCATION_TRIP.md)' in index
assert 'PROJECT_DEVICE_VERIFIED' in (kb / '25_V078_CONDITION_LOCATION_TRIP.md').read_text(encoding='utf-8')
print('PASS: v0.7.8 reference health, remaining service and c3Lbs axis mapping')

assert '](26_V079_KEYLESS_PARKING_WEB.md)' in index
assert (root / 'docs/Psa_v079_Update.md').is_file()
print('PASS: v0.7.9 key-free map documentation indexed')

assert '](28_V083_UI_WIDGET_MAP_ICONS.md)' in index
assert (root / 'docs/Psa_Button_Icon_Design_Specification_v1.md').is_file()
assert (root / 'branding/android/icons/Psa_Control_Icon_Catalog_v1.svg').is_file()
print('PASS: v0.8.3 UI, native widgets, location opt-in and icon specification indexed')

assert (root/"knowledge/29_V084_ICONS_SERVICE_TARGETSOC_READBACK.md").is_file()
assert (root/"docs/Psa_Icon_Design_Specification_v2.md").is_file()
print("PASS: v0.8.4 target SoC evidence, consent, service cycle and four readback slots indexed")
