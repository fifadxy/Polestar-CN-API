#!/usr/bin/env python3
"""Privately package an already-CI-tested PsAssistant APK as standalone.

Inputs stay on the local build machine. Never commit any previous FULL APK,
keystore, client-signing.bin, account secrets, or the signed output to GitHub.
This packages the approved existing standalone client resource (not credentials)
and re-signs with the same key as the previously installed FULL APK.
"""
import argparse
import hashlib
import os
import re
import struct
import subprocess
import tempfile
import zipfile
from pathlib import Path

ASSET = "assets/client-signing.bin"
MAGIC = b"PSACFG2\x00"
LABEL = b"PsAssistant-Standalone-ClientConfig-v2"
EXPECTED_FINGERPRINT = (
    "59e1ca066e23a43e3976ded60c6691800dd19c4b173bd94b241bee42be5a7274"
)


def u16(data, at):
    return struct.unpack_from("<H", data, at)[0]


def u32(data, at):
    return struct.unpack_from("<I", data, at)[0]


def length8(data, at):
    value = data[at]
    at += 1
    if value & 128:
        value = ((value & 127) << 8) | data[at]
        at += 1
    return value, at


def length16(data, at):
    value = u16(data, at)
    at += 2
    if value & 0x8000:
        value = ((value & 0x7fff) << 16) | u16(data, at)
        at += 2
    return value, at


def string_pool(data, off):
    header_size = u16(data, off + 2)
    count = u32(data, off + 8)
    flags = u32(data, off + 16)
    start = u32(data, off + 20)
    result = []
    for i in range(count):
        p = off + start + u32(data, off + header_size + i * 4)
        if flags & 0x100:
            _, p = length8(data, p)
            size, p = length8(data, p)
            value = data[p:p + size].decode("utf-8")
        else:
            size, p = length16(data, p)
            value = data[p:p + size * 2].decode("utf-16-le")
        result.append(value)
    return result


def manifest_attributes(data):
    if u16(data, 0) != 3:
        raise ValueError("APK manifest is not binary Android XML")
    pos = u16(data, 2)
    strings = []
    attrs = []
    while pos < len(data):
        typ = u16(data, pos)
        total = u32(data, pos + 4)
        if total < 8 or pos + total > len(data):
            raise ValueError("Malformed Android XML chunk")
        if typ == 1:
            strings = string_pool(data, pos)
        elif typ == 0x0102:
            index = u32(data, pos + 20)
            element = strings[index] if index < len(strings) else ""
            start = u16(data, pos + 24)
            size = u16(data, pos + 26)
            count = u16(data, pos + 28)
            for i in range(count):
                offset = pos + 16 + start + size * i
                name_index = u32(data, offset + 4)
                name = strings[name_index] if name_index < len(strings) else ""
                dtype = data[offset + 15]
                value = u32(data, offset + 16)
                display = strings[value] if dtype == 3 and value < len(strings) else None
                attrs.append((element, name, dtype, value, display, offset + 16))
        pos += total
    return attrs


def check_version(manifest, code, name, package):
    attrs = manifest_attributes(manifest)
    expected = [
        ("manifest", "versionCode", 16, code),
        ("manifest", "versionName", 3, name),
        ("manifest", "package", 3, package),
    ]
    for element, key, typ, target in expected:
        matches = [row for row in attrs if row[0] == element and row[1] == key]
        if len(matches) != 1 or matches[0][2] != typ:
            raise ValueError("Missing or invalid manifest attribute: " + key)
        observed = matches[0][4] if typ == 3 else matches[0][3]
        if observed != target:
            raise ValueError("Manifest version/package differs: " + key)
    return attrs


def strip_debuggable(manifest, code, name, package):
    attrs = check_version(manifest, code, name, package)
    matches = [row for row in attrs if row[0] == "application" and row[1] == "debuggable"]
    if len(matches) != 1 or matches[0][2] != 18:
        raise ValueError("Cannot safely locate android:debuggable")
    content = bytearray(manifest)
    struct.pack_into("<I", content, matches[0][5], 0)
    return bytes(content)


def read_compatible_asset(previous):
    with zipfile.ZipFile(previous) as archive:
        if archive.testzip() is not None:
            raise ValueError("Previous FULL APK ZIP failed integrity check")
        encoded = archive.read(ASSET)
    if len(encoded) != 48 or encoded[:8] != MAGIC:
        raise ValueError("Previous FULL uses an unsupported asset container")
    mask = hashlib.sha256(LABEL).digest()
    plain = bytes(encoded[8 + i] ^ mask[i] for i in range(32))
    digest = hashlib.sha256(plain).digest()
    if (digest[:8] != encoded[40:48]
            or digest.hex() != EXPECTED_FINGERPRINT):
        raise ValueError("Previous FULL client asset fails approved fingerprint")
    # Never log or persist plaintext; only the previously approved encoded asset travels.
    return encoded


def align_stored_entry(archive, info):
    offset = archive.fp.tell() + 30 + len(info.filename.encode("utf-8")) + len(info.extra)
    pad = (-(offset + 4)) % 4
    info.extra += struct.pack("<HH", 0xd935, pad) + b"\0" * pad


def make_unsigned(source, encoded, destination, version_code, version_name, package):
    with zipfile.ZipFile(source) as old, zipfile.ZipFile(destination, "w", allowZip64=False) as new:
        if old.testzip() is not None or ASSET in old.namelist():
            raise ValueError("Source must be an intact, asset-free CI validation APK")
        for entry in old.infolist():
            data = old.read(entry.filename)
            if entry.filename == "AndroidManifest.xml":
                data = strip_debuggable(data, version_code, version_name, package)
            info = zipfile.ZipInfo(entry.filename, entry.date_time)
            info.compress_type = entry.compress_type
            info.external_attr = entry.external_attr
            info.comment = entry.comment
            info.create_system = entry.create_system
            info.extra = entry.extra
            if info.compress_type == zipfile.ZIP_STORED:
                align_stored_entry(new, info)
            new.writestr(info, data, compresslevel=6)
        info = zipfile.ZipInfo(ASSET)
        info.compress_type = zipfile.ZIP_STORED
        align_stored_entry(new, info)
        new.writestr(info, encoded)


def certificate_fingerprint(apksigner_jar, file):
    proc = subprocess.run(
        ["java", "-jar", str(apksigner_jar), "verify", "--verbose",
         "--print-certs", str(file)], check=True, text=True,
        stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if not ("Verified using v2 scheme (APK Signature Scheme v2): true" in proc.stdout
            and "Verified using v3 scheme (APK Signature Scheme v3): true" in proc.stdout):
        raise ValueError("APK v2/v3 signature validation failed")
    match = re.search(r"Signer #1 certificate SHA-256 digest: ([0-9a-f]{64})", proc.stdout)
    if not match:
        raise ValueError("Signing certificate fingerprint unavailable")
    return match.group(1)


def verify_payload(source, output, encoded, version_code, version_name, package, limit):
    with zipfile.ZipFile(source) as old, zipfile.ZipFile(output) as signed:
        if signed.testzip() is not None or signed.read(ASSET) != encoded:
            raise ValueError("Signed APK corrupt or missing bundled standalone asset")
        before = set(old.namelist())
        after = set(signed.namelist())
        if before - after:
            raise ValueError("Signed APK lost an original application file")
        changes = [name for name in before if signed.read(name) != old.read(name)]
        if changes != ["AndroidManifest.xml"]:
            raise ValueError("Unexpected source changes: " + ", ".join(changes))
        attrs = check_version(signed.read("AndroidManifest.xml"), version_code, version_name, package)
        debuggable = [row for row in attrs if row[0] == "application" and row[1] == "debuggable"]
        if len(debuggable) != 1 or debuggable[0][3] != 0:
            raise ValueError("Final standalone APK must be nondebuggable")
        for entry in signed.infolist():
            if entry.compress_type == zipfile.ZIP_STORED:
                data_start = (entry.header_offset + 30
                              + len(entry.filename.encode("utf-8"))
                              + len(entry.extra))
                if data_start % 4:
                    raise ValueError("Stored APK entry is not 4-byte aligned: " + entry.filename)
    if output.stat().st_size >= limit:
        raise ValueError("Signed FULL APK exceeds the selected size limit")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--validation-apk", type=Path, required=True)
    parser.add_argument("--previous-full", type=Path, required=True)
    parser.add_argument("--keystore", type=Path, required=True)
    parser.add_argument("--apksigner-jar", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--key-alias", default="androiddebugkey")
    parser.add_argument("--version-code", type=int, default=157)
    parser.add_argument("--version-name", default="1.0.0-beta43")
    parser.add_argument("--package-name", default="io.github.polestarcn.companion")
    parser.add_argument("--size-limit", type=int, default=10_000_000)
    args = parser.parse_args()
    password = os.environ.get("PSA_SIGNING_STORE_PASSWORD")
    if not password:
        parser.error("PSA_SIGNING_STORE_PASSWORD must be supplied privately")
    if args.validation_apk.resolve() == args.output.resolve():
        parser.error("Output APK must not overwrite the CI validation input")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    encoded = read_compatible_asset(args.previous_full)
    previous_cert = certificate_fingerprint(args.apksigner_jar, args.previous_full)
    with tempfile.TemporaryDirectory(prefix="psa-standalone-") as tmp:
        unsigned = Path(tmp) / "unsigned.apk"
        make_unsigned(args.validation_apk, encoded, unsigned, args.version_code,
                      args.version_name, args.package_name)
        env = os.environ.copy()
        env["PSA_SIGNING_KEY_PASSWORD"] = (
            env.get("PSA_SIGNING_KEY_PASSWORD") or password)
        subprocess.run(
            ["java", "-jar", str(args.apksigner_jar), "sign",
             "--ks", str(args.keystore),
             "--ks-key-alias", args.key_alias,
             "--ks-pass", "env:PSA_SIGNING_STORE_PASSWORD",
             "--key-pass", "env:PSA_SIGNING_KEY_PASSWORD",
             "--v2-signing-enabled", "true", "--v3-signing-enabled", "true",
             "--v4-signing-enabled", "false", "--out", str(args.output),
             str(unsigned)],
            check=True, env=env)
    signed_cert = certificate_fingerprint(args.apksigner_jar, args.output)
    if signed_cert != previous_cert:
        args.output.unlink(missing_ok=True)
        raise ValueError("Signed FULL certificate differs from previous FULL: cannot upgrade")
    verify_payload(args.validation_apk, args.output, encoded, args.version_code,
                   args.version_name, args.package_name, args.size_limit)
    print("FULL asset: verified approved legacy PsAssistant client configuration")
    print("Native Android debuggable flag: disabled")
    print("Signing certificate: identical to previous FULL")
    print("APK v2/v3: PASS")
    print("Version: " + args.version_name + " / " + str(args.version_code))
    print("Bytes: " + str(args.output.stat().st_size))
    print("SHA-256: " + hashlib.sha256(args.output.read_bytes()).hexdigest())
    print("Real China account login and real-car command results: NOT TESTED")


if __name__ == "__main__":
    main()
