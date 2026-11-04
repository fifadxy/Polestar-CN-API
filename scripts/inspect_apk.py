#!/usr/bin/env python3
"""Offline, read-only APK inventory. Never prints token values or signing salts."""
from __future__ import annotations
import argparse, datetime, hashlib, json, re, struct
from collections import Counter
from pathlib import Path
from zipfile import ZipFile

KNOWN_FIRST_PARTY = 'Lapp/c3garage/companion/'
FILTER = re.compile(r'(access[_-]?token|refresh[_-]?token|id[_-]?token|password|cookie|secret|salt|signature|authorization|bearer)',re.I)


def dex_info(data: bytes) -> dict:
    if data[:4] != b'dex\n':
        raise ValueError('Not a dex file')
    str_n, str_at = struct.unpack_from('<II',data,56)
    type_n, type_at = struct.unpack_from('<II',data,64)
    method_n, _ = struct.unpack_from('<II',data,88)
    class_n, class_at = struct.unpack_from('<II',data,96)
    strings=[]
    for i in range(str_n):
        p = struct.unpack_from('<I',data,str_at+4*i)[0]
        for _ in range(6):
            b=data[p];p+=1
            if not b & 0x80:break
        end=data.find(b'\x00',p)
        strings.append(data[p:end].decode('utf-8','replace'))
    type_names=[]
    for i in range(type_n):
        sid=struct.unpack_from('<I',data,type_at+i*4)[0]
        type_names.append(strings[sid])
    first_party=[]
    for i in range(class_n):
        tid=struct.unpack_from('<I',data,class_at+i*32)[0]
        desc=type_names[tid]
        if desc.startswith(KNOWN_FIRST_PARTY): first_party.append(desc)
    hosts=sorted({s for s in strings if re.match(r'^https://[a-zA-Z0-9.-]+/?$',s) and not FILTER.search(s)})
    services=sorted({s for s in strings if re.match(r'^services\.vehiclestates\.[A-Za-z.]+$',s)})
    paths=sorted({s for s in strings if re.match(r'^/user/api/v\d+/[a-z0-9/_-]+$',s) and not FILTER.search(s)})
    return dict(dex_version=data[4:7].decode('ascii','replace'), bytes=len(data),
                strings=str_n,methods=method_n,classes=class_n,
                first_party_classes=len(first_party),
                first_party_core_classes=sorted(x for x in first_party if '$' not in x and '/R;' not in x and '/R$' not in x),
                observed_https_hosts=hosts,observed_vehicle_services=services,
                nonsensitive_auth_paths=paths)


def inspect(apk: Path) -> dict:
    with ZipFile(apk) as z:
        infos=z.infolist()
        dlist=[(n,z.read(n)) for n in z.namelist() if re.fullmatch(r'classes\d*\.dex',n)]
        gradle=''
        build_meta='META-INF/com/android/build/gradle/app-metadata.properties'
        if build_meta in z.namelist(): gradle=z.read(build_meta).decode('utf-8','replace')
        return {
            'analysis_date':'2026-10-26',
            'original_filename':apk.name,'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),
            'apk_size_bytes':apk.stat().st_size,'zip_entries':len(infos),
            'manifest_meta_from_reference_analysis':{
                'package':'app.c3garage.companion','versionName':'0.1.0',
                'versionCode':1,'minSdk':26,'targetSdk':34,'compileSdk':35,
                'note':'Manifest metadata independently cross-checked against provided AXML dump; see research docs.'
            },
            'gradle_app_metadata':gradle.strip().splitlines(),
            'dex':{name:dex_info(data) for name,data in dlist},
            'top_8_zip_members':[{'path':it.filename,'compressed_bytes':it.compress_size,'uncompressed_bytes':it.file_size}
                                 for it in sorted(infos,key=lambda x:x.compress_size,reverse=True)[:8]],
            'limitations':['Static inventory only; does not prove authentication/vehicle control success.',
                           'No original Java source recovered; class names do not imply complete behavior.',
                           'Sensitive constant values intentionally excluded.']
        }


if __name__ == '__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('apk',type=Path)
    parser.add_argument('--output',type=Path)
    args=parser.parse_args()
    result=inspect(args.apk)
    out=json.dumps(result,ensure_ascii=False,indent=2)+'\n'
    if args.output:
        args.output.parent.mkdir(parents=True,exist_ok=True)
        args.output.write_text(out,encoding='utf-8')
        print('Wrote',args.output)
    else:print(out)
