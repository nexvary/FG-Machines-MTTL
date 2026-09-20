#!/usr/bin/env bash
set -euo pipefail

APP_PACKAGE="com.fgmachines.rck.sidecar161"
APP_ACTIVITY="$APP_PACKAGE/com.fgmachines.rck.MainActivity"

dump_ui() {
  local name="$1"
  local attempt=1
  while [ "$attempt" -le 4 ]; do
    adb shell rm -f "/sdcard/$name.xml" || true
    if adb shell uiautomator dump "/sdcard/$name.xml" >/tmp/fg-link-uiautomator.log 2>&1 \
        && adb shell test -s "/sdcard/$name.xml"; then
      adb shell cat "/sdcard/$name.xml" > "/tmp/$name.xml"
      if grep -q '<hierarchy' "/tmp/$name.xml"; then
        return 0
      fi
    else
      cat /tmp/fg-link-uiautomator.log || true
    fi
    attempt=$((attempt + 1))
    sleep 2
  done
  echo "UI gate failed: could not obtain $name hierarchy" >&2
  return 1
}

assert_main_activity() {
  adb shell dumpsys activity activities > /tmp/fg-link-activities.txt
  if ! grep -E 'mResumedActivity|topResumedActivity' /tmp/fg-link-activities.txt | grep -q "$APP_ACTIVITY"; then
    echo "UI gate failed: FG Link MainActivity is not foreground" >&2
    grep -E 'mResumedActivity|topResumedActivity' /tmp/fg-link-activities.txt || true
    return 1
  fi
}

adb install apk-debug/app-debug.apk
adb install apk-sidecar/app-sidecar.apk
adb shell pm list packages | grep -E 'com.fgmachines.rck.debug|com.fgmachines.rck.sidecar161'

adb shell am force-stop "$APP_PACKAGE"
adb shell am start -n "$APP_ACTIVITY"
sleep 20

assert_main_activity
dump_ui fg-link-dashboard
grep -q "resource-id=\"$APP_PACKAGE:id/navAbout\"" /tmp/fg-link-dashboard.xml
adb exec-out screencap -p > FG-Link-1.6.1-dashboard.png
test -s FG-Link-1.6.1-dashboard.png

NAV_NODE="$(tr '>' '>\n' < /tmp/fg-link-dashboard.xml | grep "resource-id=\"$APP_PACKAGE:id/navAbout\"" | head -n 1)"
BOUNDS="$(printf '%s\n' "$NAV_NODE" | sed -n 's/.*bounds="\[\([0-9][0-9]*\),\([0-9][0-9]*\)\]\[\([0-9][0-9]*\),\([0-9][0-9]*\)\]".*/\1 \2 \3 \4/p')"
test -n "$BOUNDS"
read -r X1 Y1 X2 Y2 <<< "$BOUNDS"
X=$(((X1 + X2) / 2))
Y=$(((Y1 + Y2) / 2))
adb shell input tap "$X" "$Y"
sleep 3

assert_main_activity
dump_ui fg-link-about
grep -q "resource-id=\"$APP_PACKAGE:id/fgMachinesFacebookButton\"" /tmp/fg-link-about.xml
grep -q 'content-desc="FG MACHINES"' /tmp/fg-link-about.xml
grep -q 'text="FG MACHINES"' /tmp/fg-link-about.xml

adb exec-out screencap -p > FG-Link-1.6.1-about.png
test -s FG-Link-1.6.1-about.png

echo "FG Link UI branding gate passed."
