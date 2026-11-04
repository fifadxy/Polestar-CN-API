#!/usr/bin/env python3
import argparse, hashlib, zipfile
from pathlib import Path
ASSET="assets/client-signing.bin"
LEGACY="assets/standalone/account_compat.bin"
MAGIC=b"PSACFG2\0"
LABEL=b"PsAssistant-Standalone-ClientConfig-v2"
EXPECTED="59e1ca066e23a43e3976ded60c6691800dd19c4b173bd94b241bee42be5a7274"

def validate(raw):
    if len(raw)!=48 or raw[:8]!=MAGIC: raise ValueError("invalid standalone client asset")
    mask=hashlib.sha256(LABEL).digest()
    plain=bytes(raw[8+i]^mask[i] for i in range(32))
    digest=hashlib.sha256(plain).digest()
    if digest[:8]!=raw[40:48] or digest.hex()!=EXPECTED:
        raise ValueError("standalone client asset fingerprint mismatch")

def main():
    p=argparse.ArgumentParser()
    p.add_argument("apk",type=Path); p.add_argument("--expect",choices=["present","absent"],required=True)
    a=p.parse_args()
    with zipfile.ZipFile(a.apk) as z:
        if z.testzip() is not None: raise ValueError("APK ZIP integrity failed")
        names=set(z.namelist()); modern=ASSET in names; legacy=LEGACY in names
        if modern and legacy: raise ValueError("duplicate standalone client assets")
        if a.expect=="present":
            if not modern: raise ValueError("FULL APK missing standalone client asset")
            validate(z.read(ASSET))
        elif modern or legacy: raise ValueError("VALIDATION APK must not contain private login material")
    print("standalone-login packaging:",a.expect,"PASS",a.apk.name)
if __name__=="__main__": main()
