#!/usr/bin/env python3
"""Repackage a CI APK with the approved standalone client asset, then sign locally.

Never prints private client bytes and never sends them to GitHub. Supply the
signing identity through PSA_SIGNING_KEYSTORE and PSA_SIGNING_STORE_PASSWORD.
This does not recover or impersonate a missing signing key.
"""
import argparse
import copy
import hashlib
import os
from pathlib import Path
import struct
import subprocess
import tempfile
import zipfile

EXPECTED = '59e1ca066e23a43e3976ded60c6691800dd19c4b173bd94b241bee42be5a7274'
ASSET = 'assets/client-signing.bin'

def approved_asset(prior):
    with zipfile.ZipFile(prior) as source:
        raw = source.read(ASSET)
    if len(raw) != 48 or raw[:8] != b'PSACFG2\0':
        raise ValueError('Prior client asset has an unsupported format')
    mask = hashlib.sha256(b'PsAssistant-Standalone-ClientConfig-v2').digest()
    plain = bytes(a ^ b for a, b in zip(raw[8:40], mask))
    digest = hashlib.sha256(plain).digest()
    if digest[:8] != raw[40:48] or digest.hex() != EXPECTED:
        raise ValueError('Prior client asset is not the approved configuration')
    return raw

def signature_entry(name):
    upper = name.upper()
    return upper == 'META-INF/MANIFEST.MF' or (
        upper.startswith('META-INF/') and '/' not in upper[len('META-INF/'):] and
        upper.endswith(('.SF', '.RSA', '.DSA', '.EC')))

def aligned_write(output, info, data):
    entry = copy.copy(info)
    entry.extra = b''
    if entry.compress_type == zipfile.ZIP_STORED:
        # ZIP alignment extra field. Keep resources.arsc and uncompressed dex
        # four-byte aligned before apksigner adds the new signing block.
        name_bytes = entry.filename.encode('ascii' if entry.filename.isascii() else 'utf-8')
        pad = (-(output.fp.tell() + 30 + len(name_bytes))) % 4
        if pad:
            padding = (-(output.fp.tell() + 30 + len(name_bytes) + 6)) % 4
            entry.extra = struct.pack('<HHH', 0xd935, 2 + padding, 4) + b'\0' * padding
    output.writestr(entry, data)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--ci-apk', type=Path, required=True)
    parser.add_argument('--prior-apk', type=Path, required=True)
    parser.add_argument('--apksigner', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    key = os.environ.get('PSA_SIGNING_KEYSTORE')
    password = os.environ.get('PSA_SIGNING_STORE_PASSWORD')
    if not key or not password:
        raise ValueError('An explicit signing identity is required')
    asset = approved_asset(args.prior_apk)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as work:
        unsigned = Path(work) / 'aligned.apk'
        with zipfile.ZipFile(args.ci_apk) as source, zipfile.ZipFile(unsigned, 'w') as output:
            assert source.testzip() is None
            for entry in source.infolist():
                if entry.filename == ASSET or signature_entry(entry.filename):
                    continue
                aligned_write(output, entry, source.read(entry.filename))
            info = zipfile.ZipInfo(ASSET, (1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_STORED
            aligned_write(output, info, asset)
        with zipfile.ZipFile(unsigned) as check:
            for entry in check.infolist():
                if entry.compress_type == zipfile.ZIP_STORED and not entry.is_dir():
                    assert (entry.header_offset + 30 + len(entry.filename.encode()) + len(entry.extra)) % 4 == 0
        subprocess.run(['java', '-jar', str(args.apksigner), 'sign', '--ks', key,
            '--ks-key-alias', os.environ.get('PSA_SIGNING_KEY_ALIAS', 'androiddebugkey'),
            '--ks-pass', 'env:PSA_SIGNING_STORE_PASSWORD',
            '--key-pass', 'env:PSA_SIGNING_STORE_PASSWORD',
            '--v1-signing-enabled', 'true', '--v2-signing-enabled', 'true',
            '--v3-signing-enabled', 'false', '--v4-signing-enabled', 'false',
            '--out', str(args.output), str(unsigned)], check=True)
    subprocess.run(['java', '-jar', str(args.apksigner), 'verify', '--verbose', str(args.output)], check=True)
    with zipfile.ZipFile(args.ci_apk) as original, zipfile.ZipFile(args.output) as signed:
        assert signed.testzip() is None
        assert signed.read(ASSET) == asset
        # No source, bytecode, resource or module metadata changes during packaging.
        for entry in original.infolist():
            if entry.filename != ASSET and not signature_entry(entry.filename):
                assert original.read(entry.filename) == signed.read(entry.filename)
    print('Private standalone APK signed and verified:', args.output.name)
    print('Bytes:', args.output.stat().st_size)
    print('SHA256:', hashlib.sha256(args.output.read_bytes()).hexdigest())

if __name__ == '__main__':
    main()
