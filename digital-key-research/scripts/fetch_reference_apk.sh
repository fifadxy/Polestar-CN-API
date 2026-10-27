#!/bin/sh
set -u

EXPECTED_MD5="d66b01f6b6cf8cdbda0d226ae0926c60"
EXPECTED_SHA256="99070211e99b374090ffbc3702ff7888b3d8d2cc811f4e52d1a6de4ded9074ef"
OUT="/tmp/polestar-china-4.25.0.apk"
RUNTIME="digital-key-research/runtime"
mkdir -p "$RUNTIME"
LOG="$RUNTIME/apk-fetch.txt"
: > "$LOG"
UA="Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/156 Safari/537.36"

note() { printf '%s\n' "$*" | tee -a "$LOG"; }

verify_file() {
  f="$1"
  [ -s "$f" ] || return 1
  size=$(wc -c < "$f" | tr -d ' ')
  md5=$(md5sum "$f" | awk '{print $1}')
  sha=$(sha256sum "$f" | awk '{print $1}')
  note "candidate size=$size md5=$md5 sha256=$sha"
  if [ "$md5" = "$EXPECTED_MD5" ] && [ "$sha" = "$EXPECTED_SHA256" ]; then
    mv "$f" "$OUT"
    note "EXACT_MATCH=yes"
    return 0
  fi
  rm -f "$f"
  return 1
}

try_url() {
  url="$1"
  tmp=$(mktemp)
  note "trying $url"
  if curl -fL --retry 2 --connect-timeout 15 --max-time 240 -A "$UA" -o "$tmp" "$url" >>"$LOG" 2>&1; then
    verify_file "$tmp" && { note "source=$url"; return 0; }
  else
    rm -f "$tmp"
  fi
  return 1
}

# Known storefront routes / common direct-link forms.
for url in \
  "https://app.mi.com/download/1042285?id=com.polestar.china.android" \
  "https://app.mi.com/download/1042285" \
  "http://app.mi.com/download/1042285?id=com.polestar.china.android"
do
  try_url "$url" && exit 0
done

# Scrape only public listing pages. We never trust a listing until the bytes match both hashes.
for page in \
  "https://www.kxdw.com/android/129980.html" \
  "https://app.meizu.com/apps/public/detail?package_name=com.polestar.china.android" \
  "https://app.mi.com/details?id=com.polestar.china.android"
do
  note "scraping $page"
  html=$(mktemp)
  if curl -fL --retry 2 --connect-timeout 15 --max-time 60 -A "$UA" -o "$html" "$page" >>"$LOG" 2>&1; then
    grep -Eo 'https?://[^"'"'"' <>]+([.]apk|download)[^"'"'"' <>]*' "$html" | sed 's/&amp;/\&/g' | head -n 30 > "$html.urls" || true
    while IFS= read -r url; do
      [ -n "$url" ] || continue
      try_url "$url" && { rm -f "$html" "$html.urls"; exit 0; }
    done < "$html.urls"
    note "page candidates:"
    sed -n '1,30p' "$html.urls" | tee -a "$LOG" || true
  fi
  rm -f "$html" "$html.urls"
done

note "EXACT_MATCH=no"
note "The emulator job will continue without the proprietary APK; no unverified bytes are installed."
exit 0
