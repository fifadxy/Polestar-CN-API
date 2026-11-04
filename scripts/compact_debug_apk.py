#!/usr/bin/env python3
"""Lossless ZIP recompression and debug re-signing; leaves every non-signature entry unchanged."""
import argparse
from pathlib import Path
import subprocess
import tempfile
import zipfile
from package_private_client import aligned_write, signature_entry

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('apk',type=Path);p.add_argument('--apksigner',type=Path,required=True);p.add_argument('--keystore',type=Path,required=True)
    a=p.parse_args()
    if not a.keystore.is_file():
        # Fresh GitHub runners may not contain ~/.android/debug.keystore, even after
        # Gradle successfully builds a signed validation APK. This is a DEBUG-only
        # ephemeral key; it is never used for a standalone/release upgrade.
        a.keystore.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run([
            'keytool','-genkeypair','-noprompt',
            '-keystore',str(a.keystore),
            '-alias','androiddebugkey',
            '-storepass','android','-keypass','android',
            '-keyalg','RSA','-keysize','2048','-validity','10000',
            '-dname','CN=Android Debug,O=Android,C=US'
        ], check=True)
    with tempfile.TemporaryDirectory() as work:
        unsigned=Path(work)/'aligned.apk';signed=Path(work)/'signed.apk'
        with zipfile.ZipFile(a.apk) as source,zipfile.ZipFile(unsigned,'w') as out:
            for entry in source.infolist():
                if signature_entry(entry.filename):continue
                entry._compresslevel=9
                aligned_write(out,entry,source.read(entry.filename))
        subprocess.run(['java','-jar',str(a.apksigner),'sign','--ks',str(a.keystore),'--ks-key-alias','androiddebugkey','--ks-pass','pass:android','--key-pass','pass:android','--out',str(signed),str(unsigned)],check=True)
        subprocess.run(['java','-jar',str(a.apksigner),'verify',str(signed)],check=True)
        with zipfile.ZipFile(a.apk) as original,zipfile.ZipFile(signed) as result:
            assert result.testzip() is None
            for entry in original.infolist():
                if not signature_entry(entry.filename):assert original.read(entry.filename)==result.read(entry.filename)
        a.apk.write_bytes(signed.read_bytes())
    print('Lossless APK recompression and signing verified:',a.apk.stat().st_size)
if __name__=='__main__':main()
