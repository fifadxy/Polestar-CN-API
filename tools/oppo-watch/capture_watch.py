#!/usr/bin/env python3
"""Read-only, small ADB capture. Never switches modes, sends car commands or saves serials/logcat."""
import argparse
import json
from pathlib import Path
import re
import shutil
import subprocess
import zipfile

PROPERTIES = ('ro.product.manufacturer', 'ro.product.model', 'ro.product.device',
              'ro.build.version.release', 'ro.build.version.sdk', 'ro.build.version.security_patch',
              'ro.build.display.id', 'ro.build.version.opporom')
PACKAGE = re.compile(r'^[A-Za-z0-9_]+(?:\.[A-Za-z0-9_]+)+$')
RELEVANT = re.compile(r'vehicle|carcontrol|carlink|speechassist|voice|wear|watch|mydevices|pantanal|rtos|lowpower|heytap.health', re.I)
VERSION = re.compile(r'\b(versionName|versionCode|minSdk|targetSdk)\s*[=:]\s*([^\s]+)')


def adb(serial, *args, timeout=8):
    result = subprocess.run(['adb', '-s', serial, *args], capture_output=True,
                            text=True, errors='replace', timeout=timeout)
    if result.returncode:
        raise RuntimeError('adb-command-failed')  # Raw stderr can include serials; never persist it.
    return result.stdout


def package_versions(text):
    return {key: value[:100] for key, value in VERSION.findall(text)}


def capture(serial):
    result = {'androidAdbReachable': False, 'properties': {}, 'packages': []}
    try:
        if adb(serial, 'get-state').strip() != 'device':
            return result
        result['androidAdbReachable'] = True
        for prop in PROPERTIES:
            try:
                result['properties'][prop] = adb(serial, 'shell', 'getprop', prop).strip()[:512]
            except (RuntimeError, subprocess.TimeoutExpired):
                result['properties'][prop] = 'unavailable'
        text = adb(serial, 'shell', 'pm', 'list', 'packages')
        packages = sorted({line.removeprefix('package:').strip() for line in text.splitlines()
                           if line.startswith('package:')})
        selected = [p for p in packages if PACKAGE.fullmatch(p) and RELEVANT.search(p)]
        result['matchingPackageCount'] = len(selected)
        result['packageListTruncated'] = len(selected) > 24
        for pkg in selected[:24]:
            item = {'package': pkg}
            try:
                item['versions'] = package_versions(adb(serial, 'shell', 'dumpsys', 'package', pkg))
            except (RuntimeError, subprocess.TimeoutExpired):
                item['status'] = 'unavailable'
            result['packages'].append(item)
    except (RuntimeError, subprocess.TimeoutExpired):
        result['status'] = 'adb-unavailable-in-this-stage'
    return result


def choose_device():
    result = subprocess.run(['adb', 'devices'], capture_output=True, text=True, check=True)
    devices = [line.split()[0] for line in result.stdout.splitlines()[1:]
               if len(line.split()) == 2 and line.split()[1] == 'device']
    if not devices:
        raise RuntimeError('未找到已授权的 ADB 设备。请在手表全智能模式下连接并授权。')
    if len(devices) == 1:
        return devices[0]
    print('请选择手表（不显示或保存设备序列号）：')
    for index, serial in enumerate(devices, 1):
        try:
            model = adb(serial, 'shell', 'getprop', 'ro.product.model').strip()
        except (RuntimeError, subprocess.TimeoutExpired):
            model = '型号不可读'
        print(f'{index}. {model[:100]}')
    choice = int(input('输入手表序号：'))
    if not 1 <= choice <= len(devices):
        raise ValueError('设备序号超出范围')
    return devices[choice - 1]


def write_report(report, destination):
    raw = json.dumps(report, ensure_ascii=False, indent=2).encode('utf-8')
    if len(raw) > 500_000:
        raise ValueError('Report exceeded metadata budget')
    with zipfile.ZipFile(destination, 'w', zipfile.ZIP_DEFLATED) as output:
        output.writestr('watch-report.json', raw)
        output.writestr('README.txt', '只读采集，无 APK、日志、序列号、蓝牙地址、账号和车控命令。\nADB/调试连接可能影响功耗；不能用此报告证明手表处于完全低功耗状态。\n')
    if destination.stat().st_size > 1_000_000:
        destination.unlink()
        raise ValueError('ZIP exceeded 1 MB limit')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, default=Path(__file__).with_name('OPPO-Watch-X2-light.zip'))
    args = parser.parse_args()
    if not shutil.which('adb'):
        raise RuntimeError('需要先安装 Android Platform Tools，让 adb 可在终端运行。')
    serial = choose_device()  # Used locally only; not included in report.
    print('仅采集版本和相关包名，不执行任何车辆操作。')
    input('确认手表处于全智能模式后按回车：')
    report = {'schemaVersion': 1, 'smartMode': capture(serial), 'enduranceMode': {},
              'caveat': 'ADB access can affect power state; unavailable ADB does not prove RTOS behavior.'}
    input('手动切到长续航模式后按回车（ADB 断开也可以继续）：')
    report['enduranceMode'] = capture(serial)
    observed = {}
    for key, question in [('vehicleEntry', '长续航模式下能否打开原生车辆入口'),
                          ('voiceEntry', '长续航模式下能否打开小布'),
                          ('phoneConnected', '该模式下手表是否显示与手机保持连接')]:
        answer = input(question + '？输入 y/n/u（不确定）：').strip().lower()
        observed[key] = answer if answer in ('y', 'n', 'u') else 'u'
    report['userReportedEnduranceObservations'] = observed
    write_report(report, args.output)
    print(f'已生成：{args.output.name}（{args.output.stat().st_size} 字节）')
    print('请上传该 ZIP；完成后可关闭手表调试。')


if __name__ == '__main__':
    try:
        main()
    except (RuntimeError, ValueError, OSError, subprocess.SubprocessError, IndexError) as error:
        print('采集未完成：', str(error) if isinstance(error, (RuntimeError, ValueError)) else type(error).__name__)
        raise SystemExit(1)
