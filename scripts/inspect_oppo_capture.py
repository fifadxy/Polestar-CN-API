#!/usr/bin/env python3
"""Verify exported OEM APK hashes and index specific card-routing string leads; no decompilation claim."""
import argparse,hashlib,io,json,re,zipfile
from pathlib import Path

def inspect(path):
    out={'scope':'string leads only; not proof of method behavior','packages':[]}
    with zipfile.ZipFile(path) as bundle:
        report=json.loads(bundle.read('report.json'))
        for package in report.get('packages',[]):
            item={'package':package.get('packageName'),'version':package.get('versionName'),'files':[]}
            for entry in package.get('files',[]):
                if not entry.get('copied'):continue
                raw=bundle.read(entry['entry'])
                if hashlib.sha256(raw).hexdigest()!=entry.get('sha256'):raise ValueError('APK hash mismatch')
                found=set()
                with zipfile.ZipFile(io.BytesIO(raw)) as apk:
                    for name in apk.namelist():
                        if not re.fullmatch(r'classes\d*\.dex',name):continue
                        if apk.getinfo(name).file_size>150_000_000:raise ValueError('DEX budget exceeded')
                        for value in re.findall(rb'[ -~]{8,220}',apk.read(name)):
                            if any(token in value for token in (b'QuickAppCardAction',b'CardsUtils',b'onShortcutClick',b'handleBindDeviceEvent',b'handleJumpDeviceDetailEvent')):
                                found.add(value.decode('ascii'))
                item['files'].append({'entry':entry['entry'],'sha256Verified':True,'routingLeads':sorted(found)})
            if item['files']:out['packages'].append(item)
    return out

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('archive',type=Path);args=p.parse_args()
    print(json.dumps(inspect(args.archive),ensure_ascii=False,indent=2))
