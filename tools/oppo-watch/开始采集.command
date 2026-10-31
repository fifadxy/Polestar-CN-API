#!/bin/bash
cd -- "$(dirname -- "$0")"
python3 capture_watch.py
read -r -p "按回车关闭窗口…"
