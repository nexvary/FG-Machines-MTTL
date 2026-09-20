#!/usr/bin/env bash
set -euo pipefail

APP_PACKAGE="com.fgmachines.rck.sidecar161"
APP_ACTIVITY="$APP_PACKAGE/com.fgmachines.rck.MainActivity"

wake_and_unlock() {
  adb shell input keyevent KEYCODE_WAKEUP || true
  adb shell wm dismiss-keyguard || true
  adb shell input keyevent 82 || true
  adb shell settings put system screen_off_timeout 2147483647 || true
  adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1 || true
}

dump_ui() {
  local name="$1"
  local attempt=1
  while [ "$attempt" -le 8 ]; do
    adb shell rm -f "/sdcard/$name.xml" || true
    : > /tmp/fg-link-uiautomator.log
    if adb shell uiautomator dump --compressed "/sdcard/$name.xml" >/tmp/fg-link-uiautomator.log 2>&1; then
      if adb exec-out cat "/sdcard/$name.xml" > "/tmp/$name.xml" 2>/tmp/fg-link-cat.log; then
        if grep -q '<hierarchy' "/tmp/$name.xml"; then
          return 0
        fi
      fi
    fi
    cat /tmp/fg-link-uiautomator.log || true
    cat /tmp/fg-link-cat.log 2>/dev/null || true
    wake_and_unlock
    attempt=$((attempt + 1))
    sleep 3
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

adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
  sleep 2
done

adb install -r apk-debug/app-debug.apk
adb install -r apk-sidecar/app-sidecar.apk
adb shell pm list packages | grep -E 'com.fgmachines.rck.debug|com.fgmachines.rck.sidecar161'

wake_and_unlock
adb shell am force-stop "$APP_PACKAGE"
adb shell am start -W -n "$APP_ACTIVITY"
sleep 10
wake_and_unlock

assert_main_activity
dump_ui fg-link-dashboard
grep -q "resource-id=\"$APP_PACKAGE:id/navAbout\"" /tmp/fg-link-dashboard.xml
adb exec-out screencap -p > FG-Link-1.6.1-dashboard.png
test -s FG-Link-1.6.1-dashboard.png

NAV_NODE="$(grep -o "<node[^>]*resource-id=\"$APP_PACKAGE:id/navAbout\"[^>]*>" /tmp/fg-link-dashboard.xml | head -n 1)"
BOUNDS="$(printf '%s\n' "$NAV_NODE" | sed -n 's/.*bounds="\[\([0-9][0-9]*\),\([0-9][0-9]*\)\]\[\([0-9][0-9]*\),\([0-9][0-9]*\)\]".*/\1 \2 \3 \4/p')"
test -n "$BOUNDS"
read -r X1 Y1 X2 Y2 <<< "$BOUNDS"
X=$(((X1 + X2) / 2))
Y=$(((Y1 + Y2) / 2))
adb shell input tap "$X" "$Y"
sleep 3
wake_and_unlock

assert_main_activity
dump_ui fg-link-about
grep -q "resource-id=\"$APP_PACKAGE:id/fgMachinesFacebookButton\"" /tmp/fg-link-about.xml
grep -q "resource-id=\"$APP_PACKAGE:id/alaaMohamedFacebookButton\"" /tmp/fg-link-about.xml
grep -q 'content-desc="FG MACHINES"' /tmp/fg-link-about.xml
grep -q 'text="FG MACHINES"' /tmp/fg-link-about.xml
grep -q 'text="About the developer"' /tmp/fg-link-about.xml
grep -q 'text="In the name of Allah, the Most Gracious, the Most Merciful"' /tmp/fg-link-about.xml

adb exec-out screencap -p > FG-Link-1.6.1-about.png
test -s FG-Link-1.6.1-about.png

echo "FG Link UI branding and developer-page gate passed."
