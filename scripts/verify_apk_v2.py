#!/usr/bin/env python3
"""Read-only APK v2 signature verification for a locally supplied reference sample.

Uses cryptography for signature/certificate parsing; never exports credentials, private
keys, token values or opaque app constants. This is *sample provenance evidence*,
not Android signing/anti-tamper hardening for the new app.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import struct
from pathlib import Path

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa

APK_SIG_BLOCK_MAGIC = b'APK Sig Block 42'
V2_ID = 0x7109871A
CHUNK_SIZE = 1024 * 1024


def piece(data: bytes, pos: int) -> tuple[bytes, int]:
    if pos + 4 > len(data): raise ValueError('truncated length')
    n = struct.unpack_from('<I', data, pos)[0]
    pos += 4
    if n > len(data) - pos: raise ValueError('invalid length')
    return data[pos:pos + n], pos + n


def entries(data: bytes):
    offset = 0
    while offset < len(data):
        payload, offset = piece(data, offset)
        yield payload


def sha256_content_digest(parts: list[bytes]) -> bytes:
    hashes_by_chunk = []
    for part in parts:
        for start in range(0, len(part), CHUNK_SIZE):
            chunk = part[start:start + CHUNK_SIZE]
            hashes_by_chunk.append(hashlib.sha256(b'\xa5' + struct.pack('<I', len(chunk)) + chunk).digest())
    return hashlib.sha256(b'\x5a' + struct.pack('<I', len(hashes_by_chunk)) + b''.join(hashes_by_chunk)).digest()


def inspect(path: Path) -> dict:
    data = path.read_bytes()
    eocd = data.rfind(b'PK\x05\x06')
    if eocd < 0 or eocd + 22 > len(data): raise ValueError('ZIP EOCD not found')
    eocd_size = 22 + struct.unpack_from('<H', data, eocd + 20)[0]
    if eocd + eocd_size != len(data): raise ValueError('unexpected trailing ZIP content')
    central_offset = struct.unpack_from('<I', data, eocd + 16)[0]
    if data[central_offset - 16:central_offset] != APK_SIG_BLOCK_MAGIC:
        raise ValueError('APK signature block not found')
    size = struct.unpack_from('<Q', data, central_offset - 24)[0]
    block_start = central_offset - (size + 8)
    if struct.unpack_from('<Q', data, block_start)[0] != size:
        raise ValueError('APK signature block size mismatch')

    v2_payload = None
    pos = block_start + 8
    while pos < central_offset - 24:
        pair_size = struct.unpack_from('<Q', data, pos)[0]
        pair_id = struct.unpack_from('<I', data, pos + 8)[0]
        if pair_id == V2_ID:
            v2_payload = data[pos+12:pos+8+pair_size]
        pos += 8 + pair_size
    if v2_payload is None: raise ValueError('No v2 signature scheme payload')

    signers, _ = piece(v2_payload, 0)
    all_signers = list(entries(signers))
    if len(all_signers) != 1: raise ValueError('expected one signer in reference APK')
    signed_data, p = piece(all_signers[0], 0)
    signatures, p = piece(all_signers[0], p)
    public_key_der, p = piece(all_signers[0], p)

    digest_rows, p = piece(signed_data, 0)
    cert_rows, p = piece(signed_data, p)
    certs = list(entries(cert_rows))
    if not certs: raise ValueError('No signer certificate')
    cert = x509.load_der_x509_certificate(certs[0])
    signer_key = cert.public_key()
    declared_public_key = serialization.load_der_public_key(public_key_der)
    if declared_public_key.public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo) != signer_key.public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo):
        raise ValueError('Public key does not match certificate')

    sig_map = {}
    for signature_record in entries(signatures):
        algo = struct.unpack_from('<I', signature_record, 0)[0]
        sig, _ = piece(signature_record, 4)
        sig_map[algo] = sig
    digest_map = {}
    for digest_record in entries(digest_rows):
        algo = struct.unpack_from('<I', digest_record, 0)[0]
        digest, _ = piece(digest_record, 4)
        digest_map[algo] = digest

    # v2 signature alg 0x0103 = RSA PKCS1 v1.5 SHA-256
    if 0x0103 not in sig_map or 0x0103 not in digest_map: raise ValueError('Unexpected v2 signature algorithm')
    if not isinstance(signer_key, rsa.RSAPublicKey): raise ValueError('Expected RSA reference certificate')
    signer_key.verify(sig_map[0x0103], signed_data, padding.PKCS1v15(), hashes.SHA256())

    patched_eocd = bytearray(data[eocd:eocd+eocd_size])
    struct.pack_into('<I', patched_eocd, 16, block_start)
    computed_digest = sha256_content_digest([
        data[:block_start], data[central_offset:eocd], bytes(patched_eocd),
    ])
    if computed_digest != digest_map[0x0103]:
        raise ValueError('APK v2 content digest mismatch')

    return {
        'file': path.name,
        'file_sha256': hashlib.sha256(data).hexdigest(),
        'apk_v2_signature': 'VERIFIED',
        'apk_v2_content_digest': 'VERIFIED',
        'signing_algorithm': 'RSA-3072 / PKCS1v1.5 / SHA-256',
        'certificate_subject': cert.subject.rfc4514_string(),
        'certificate_issuer': cert.issuer.rfc4514_string(),
        'certificate_sha256': hashlib.sha256(certs[0]).hexdigest(),
        'notes': ['Signature integrity does not identify the real-world app publisher.', 'No original app credentials or signing keys are included.'],
    }


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('apk', type=Path)
    p.add_argument('--output', type=Path)
    args = p.parse_args()
    result = json.dumps(inspect(args.apk), ensure_ascii=False, indent=2) + '\n'
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(result, encoding='utf-8')
        print('Wrote', args.output)
    else:
        print(result)
