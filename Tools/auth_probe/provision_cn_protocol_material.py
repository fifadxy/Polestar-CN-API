#!/usr/bin/env python3
import argparse, re, struct, subprocess, sys, tempfile, zipfile
from pathlib import Path

SERVICE = "io.kheirallah.hisingen.polestar-cn.protocol"
ACCOUNT = "gateway-client-material-v1"
TARGET_CLASS = "Lcom/polestar/base/utils/a1;"
TARGET_METHOD = "clido"
REQUIRED_STRINGS = {
    "appId", "polestar-app-android", "timestamp", "nonce", "deviceId", "deviceModel",
    "clientType", "PHONE", "systemVer", "appVer", "sign"
}

def u16(b, o): return struct.unpack_from("<H", b, o)[0]
def u32(b, o): return struct.unpack_from("<I", b, o)[0]

def uleb(b, o):
    value = 0
    shift = 0
    while True:
        c = b[o]; o += 1
        value |= (c & 0x7f) << shift
        if not (c & 0x80): return value, o
        shift += 7
        if shift > 35: raise ValueError("invalid ULEB128")

def parse_dex(path):
    b = Path(path).read_bytes()
    if not b.startswith(b"dex\n"): raise ValueError("not a DEX file")
    ss, so = u32(b, 0x38), u32(b, 0x3c)
    ts, to = u32(b, 0x40), u32(b, 0x44)
    ps, po = u32(b, 0x48), u32(b, 0x4c)
    ms, mo = u32(b, 0x58), u32(b, 0x5c)
    cs, co = u32(b, 0x60), u32(b, 0x64)
    strings = []
    for i in range(ss):
        off = u32(b, so + 4*i); _, p = uleb(b, off); e = b.find(b"\0", p)
        strings.append(b[p:e].decode("utf-8", "replace"))
    types = [strings[u32(b, to + 4*i)] for i in range(ts)]
    protos = []
    for i in range(ps):
        ret = u32(b, po + 12*i + 4); params_off = u32(b, po + 12*i + 8)
        params = []
        if params_off:
            n = u32(b, params_off)
            params = [types[u16(b, params_off + 4 + 2*j)] for j in range(n)]
        protos.append((types[ret], params))
    methods = []
    for i in range(ms):
        cls = u16(b, mo + 8*i); proto = u16(b, mo + 8*i + 2); name = u32(b, mo + 8*i + 4)
        methods.append((types[cls], strings[name], protos[proto], i))
    code_by_method = {}
    for i in range(cs):
        p = co + 32*i; class_data_off = u32(b, p + 24)
        if not class_data_off: continue
        q = class_data_off
        sf, q = uleb(b, q); inf, q = uleb(b, q); dm, q = uleb(b, q); vm, q = uleb(b, q)
        idx = 0
        for _ in range(sf + inf):
            diff, q = uleb(b, q); _, q = uleb(b, q); idx += diff
        for count in (dm, vm):
            idx = 0
            for _ in range(count):
                diff, q = uleb(b, q); _, q = uleb(b, q); code_off, q = uleb(b, q); idx += diff
                if code_off: code_by_method[idx] = code_off
    return b, strings, methods, code_by_method

W = {}
for op in [0x00,0x01,0x04,0x07,0x0a,0x0b,0x0c,0x0d,0x0e,0x0f,0x10,0x11,0x12,0x1d,0x1e,0x21,0x27,0x28]: W[op]=1
for op in [0x02,0x05,0x08,0x13,0x15,0x16,0x19,0x1a,0x1c,0x1f,0x20,0x22,0x23,0x29]: W[op]=2
for op in [0x03,0x06,0x09,0x14,0x17,0x1b,0x24,0x25,0x26,0x2a,0x2b,0x2c]: W[op]=3
W[0x18]=5
for a,b in [(0x2d,0x32),(0x32,0x3e),(0x44,0x52),(0x52,0x60),(0x60,0x6e),(0x90,0xb0),(0xd0,0xd8),(0xd8,0xe3)]:
    for op in range(a,b): W[op]=2
for a,b in [(0x3e,0x44),(0x73,0x74),(0x79,0x7b),(0x7b,0x90),(0xb0,0xd0),(0xe3,0xfa)]:
    for op in range(a,b): W[op]=1
for op in range(0x6e,0x73): W[op]=3
for op in range(0x74,0x79): W[op]=3
W[0xfa]=4; W[0xfb]=4; W[0xfc]=3; W[0xfd]=3; W[0xfe]=2; W[0xff]=2

def string_refs(b, strings, code_off):
    size = u32(b, code_off + 12); base = code_off + 16
    units = [u16(b, base + 2*i) for i in range(size)]
    out = []; i = 0
    while i < len(units):
        word = units[i]; op = word & 0xff; hi = word >> 8
        if op == 0 and hi in (1,2,3):
            if hi == 1:
                if i+1 >= len(units): break
                width = 4 + units[i+1]*2
            elif hi == 2:
                if i+1 >= len(units): break
                width = 2 + units[i+1]*4
            else:
                if i+3 >= len(units): break
                ew = units[i+1]; n = units[i+2] | (units[i+3] << 16)
                width = 4 + ((n*ew + 1)//2)
            i += max(1, width); continue
        if op == 0x1a and i+1 < len(units):
            idx = units[i+1]
            if idx < len(strings): out.append(strings[idx])
        elif op == 0x1b and i+2 < len(units):
            idx = units[i+1] | (units[i+2] << 16)
            if idx < len(strings): out.append(strings[idx])
        i += W.get(op, 1)
    return out

def locate_material(dex_path):
    b, strings, methods, codes = parse_dex(dex_path)
    for cls, name, proto, idx in methods:
        ret, params = proto
        if cls == TARGET_CLASS and name == TARGET_METHOD and ret == "Lokhttp3/HttpUrl;" and params == ["Lokhttp3/HttpUrl;"]:
            off = codes.get(idx)
            if not off: continue
            refs = string_refs(b, strings, off)
            if not REQUIRED_STRINGS.issubset(set(refs)): continue
            candidates = list(dict.fromkeys(x for x in refs if re.fullmatch(r"[0-9A-F]{32}", x)))
            if len(candidates) == 1: return candidates[0]
    return None

def candidate_dex_files(source):
    p = Path(source).expanduser().resolve()
    if p.is_dir():
        yield from sorted(p.rglob("*.dex")); return
    if p.suffix.lower() == ".dex":
        yield p; return
    if p.suffix.lower() == ".zip":
        with zipfile.ZipFile(p) as z, tempfile.TemporaryDirectory(prefix="hisingen-cn-dex-") as tmp:
            for name in z.namelist():
                if not name.lower().endswith(".dex"): continue
                target = Path(tmp) / Path(name).name
                target.write_bytes(z.read(name))
                yield target
        return
    raise SystemExit("Input must be a DEX, ZIP, or directory containing DEX files.")

def main():
    ap = argparse.ArgumentParser(description="Provision Hisingen China gateway protocol material from your own official Android DEX dump.")
    ap.add_argument("source", help="Polestar-China-Dex.zip, a .dex file, or a DEX directory")
    ap.add_argument("--dry-run", action="store_true", help="Locate and validate without writing Keychain")
    args = ap.parse_args()
    found = None
    for dex in candidate_dex_files(args.source):
        try: found = locate_material(dex)
        except Exception: continue
        if found: break
    if not found:
        raise SystemExit("Could not locate validated gateway protocol material in the supplied official-app DEX dump.")
    print("Validated official China gateway signing method and located protocol material (value not displayed).")
    if args.dry_run:
        print("Dry run complete; Keychain was not changed.")
        return
    if sys.platform != "darwin":
        raise SystemExit("Keychain provisioning must be run on macOS.")
    subprocess.run([
        "/usr/bin/security", "add-generic-password", "-U",
        "-s", SERVICE, "-a", ACCOUNT, "-w", found
    ], check=True, stdout=subprocess.DEVNULL)
    print("Stored protocol material in macOS Keychain for Hisingen China. No value was printed.")

if __name__ == "__main__":
    main()
