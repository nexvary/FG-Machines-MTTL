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
sleep 8
wake_and_unlock
assert_main_activity
adb exec-out screencap -p > FG-Link-1.6.1-dashboard.png
test -s FG-Link-1.6.1-dashboard.png

adb logcat -c
adb shell am force-stop "$APP_PACKAGE"
adb shell am start -W -n "$APP_ACTIVITY" --es fg_ui_test_page about
sleep 8
wake_and_unlock
assert_main_activity

adb logcat -d -s FGLinkUiGate:I '*:S' > /tmp/fg-link-about-log.txt || true
cat /tmp/fg-link-about-log.txt
grep -q 'about-page-visible' /tmp/fg-link-about-log.txt

adb exec-out screencap -p > FG-Link-1.6.1-about.png
test -s FG-Link-1.6.1-about.png

echo "FG Link UI branding and developer-page gate passed."
