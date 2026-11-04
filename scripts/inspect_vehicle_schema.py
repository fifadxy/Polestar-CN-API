#!/usr/bin/env python3
"""Extract only *field signatures* of the reference APK vehicle model (no values)."""
import argparse, json, struct
from pathlib import Path
from zipfile import ZipFile
TARGET = 'Lapp/c3garage/companion/PolestarApi$Vehicle;'

def get_strings(data):
    count, offset = struct.unpack_from('<II', data, 56)
    out = []
    for i in range(count):
        p = struct.unpack_from('<I', data, offset+4*i)[0]
        while True:
            v = data[p];p+=1
            if not v & 128: break
        out.append(data[p:data.index(b'\0',p)].decode('utf-8','replace'))
    return out

def inspect(path):
    with ZipFile(path) as zip_file: data=zip_file.read('classes.dex')
    strings=get_strings(data)
    n, off=struct.unpack_from('<II', data, 64)
    types=[strings[struct.unpack_from('<I',data,off+4*i)[0]] for i in range(n)]
    n, off=struct.unpack_from('<II',data,80)
    fields=[]
    for i in range(n):
        c,t,name=struct.unpack_from('<HHI',data,off+8*i)
        if types[c] == TARGET:
            fields.append({'name':strings[name], 'dex_type':types[t]})
    return {'apk_file':path.name,'class':TARGET,'field_count':len(fields),'fields':sorted(fields,key=lambda x:x['name']), 'note':'Field names and declared types are static signatures, not live response mappings or proof of feature support.'}
if __name__ == '__main__':
    p=argparse.ArgumentParser();p.add_argument('apk',type=Path);p.add_argument('--output',type=Path)
    args=p.parse_args();s=json.dumps(inspect(args.apk),ensure_ascii=False,indent=2)+'\n'
    if args.output: args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_text(s,encoding='utf-8');print('Wrote',args.output)
    else:print(s)
