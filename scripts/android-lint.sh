#!/bin/bash
# android-lint.sh — grep-based static audit for the Kotlin sources + manifest.
# Complements `./gradlew :app:lint` (our real Android Lint run) with checks
# the Gradle task misses: OPSEC rules, security-specific patterns.
#
# Usage:
#   scripts/android-lint.sh
#   scripts/android-lint.sh app/src/main
#
# Exit 0 pass, 1 on blockers.

set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TARGETS=("${@:-$ROOT/app/src}")
COLOR=""; GREEN=""; YELLOW=""; RED=""; RESET=""
if [ -t 1 ]; then
  GREEN=$'\033[0;32m'; YELLOW=$'\033[0;33m'; RED=$'\033[0;31m'; RESET=$'\033[0m'
fi

warn=0; err=0

emit() {
  local tier="$1"; local msg="$2"; local loc="$3"
  if [ "$tier" = "ERR" ]; then
    printf '%s[BLOCKER]%s %s\n  %s\n' "$RED" "$RESET" "$msg" "$loc"
    err=$((err + 1))
  else
    printf '%s[WARN]%s    %s\n  %s\n' "$YELLOW" "$RESET" "$msg" "$loc"
    warn=$((warn + 1))
  fi
}

scan() {
  local tier="$1"; local msg="$2"; local pattern="$3"; local glob="${4:-}"
  local cmd
  if command -v rg >/dev/null 2>&1; then
    cmd="rg --no-heading --line-number --color=never"
    [ -n "$glob" ] && cmd="$cmd -g '$glob'"
    cmd="$cmd \"$pattern\""
  else
    cmd="grep -rn"
    [ -n "$glob" ] && cmd="$cmd --include='$glob'"
    cmd="$cmd \"$pattern\""
  fi
  local matches
  matches=$(eval "$cmd" "${TARGETS[@]}" 2>/dev/null || true)
  [ -z "$matches" ] && return
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    emit "$tier" "$msg" "$line"
  done <<< "$matches"
}

echo "Android audit — ${TARGETS[*]}"
echo
echo "── Blockers ──"

# B1: plaintext http in code (should be https or configured via
# network_security_config).
scan "ERR" "hardcoded http:// URL — promote to https or move to network_security_config with debug override" \
  'http://[a-z]' '*.kt'

# B2: android:exported="true" on a non-MainActivity component leaks intents.
# Flag every exported=true; operator must manually confirm it's MainActivity.
scan "ERR" "android:exported=\"true\" — verify it's ONLY MainActivity; services/receivers must be exported=false" \
  'android:exported="true"' 'AndroidManifest.xml'

# B3: @JavascriptInterface — zero-tolerance unless explicitly reviewed.
scan "ERR" "@JavascriptInterface — WebView bridge is a known RCE vector; remove unless reviewed by OPSEC" \
  '@JavascriptInterface' '*.kt'

# B4: text-size in px/pt (breaks font-scale accessibility).
scan "ERR" 'android:textSize in px or pt — must use sp for user-scalable text' \
  'android:textSize="[0-9]+(px|pt)"' '*.xml'

# B5: usesCleartextTraffic="true" without explicit justification.
scan "ERR" "usesCleartextTraffic=\"true\" — plaintext HTTP disabled in prod; use debug-only network_security_config" \
  'usesCleartextTraffic="true"' 'AndroidManifest.xml'

echo
echo "── Warnings ──"

# W1: hardcoded strings in Kotlin (should move to strings.xml for i18n).
scan "WARN" "hardcoded user-facing string — move to res/values/strings.xml for i18n" \
  'setText\("[A-Z][^"]{4,}"\)' '*.kt'

# W2: blocking I/O from main thread (URL.readText, Thread.sleep on UI).
scan "WARN" "possible blocking I/O on main thread — confirm it's inside Dispatchers.IO" \
  'URL\(.*\)\.readText\(\)|Thread\.sleep\(' '*.kt'

# W3: tap targets < 48dp.
scan "WARN" "layout_width/height < 48dp on interactive component — Material minimum is 48×48dp" \
  'android:layout_(width|height)="(1[0-9]|[1-3][0-9])dp"' '*.xml'

# W4: ContentDescription missing on ImageButton/ImageView.
# (Heuristic — just scan for ImageButton without a content_description line)
if command -v rg >/dev/null 2>&1; then
  for layout in $(rg -l "ImageButton|ImageView" "${TARGETS[@]}" -g '*.xml' 2>/dev/null); do
    if ! grep -q "contentDescription" "$layout" 2>/dev/null; then
      emit "WARN" "ImageButton/View without contentDescription — TalkBack silent" "$layout"
    fi
  done
fi

# W5: Intent with implicit action — over-permissive.
scan "WARN" "Intent with implicit action string — prefer explicit Intent(Context, Class.class) where possible" \
  'Intent\("android\.intent\.action' '*.kt'

echo
echo "Summary: ${err} blocker(s), ${warn} warning(s)"
if [ "$err" -gt 0 ]; then
  echo "${RED}FAIL${RESET}"
  exit 1
fi
if [ "$warn" -gt 0 ]; then
  echo "${YELLOW}PASS with warnings${RESET}"
else
  echo "${GREEN}PASS${RESET}"
fi
exit 0
