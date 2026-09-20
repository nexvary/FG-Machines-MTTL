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

main_activity_is_foreground() {
  adb shell dumpsys activity activities > /tmp/fg-link-activities.txt
  grep -E 'mResumedActivity|topResumedActivity' /tmp/fg-link-activities.txt | grep -q "$APP_ACTIVITY"
}

wait_for_main_activity() {
  local attempt=1
  while [ "$attempt" -le 20 ]; do
    if main_activity_is_foreground; then
      return 0
    fi
    wake_and_unlock
    sleep 2
    attempt=$((attempt + 1))
  done

  echo "UI gate failed: FG Link MainActivity is not foreground" >&2
  grep -E 'mResumedActivity|topResumedActivity' /tmp/fg-link-activities.txt || true
  adb shell dumpsys window windows | grep -E 'mCurrentFocus|mFocusedApp' || true
  adb shell pidof "$APP_PACKAGE" || true
  adb logcat -d -t 300 | grep -E "$APP_PACKAGE|FGLinkUiGate|AndroidRuntime|FATAL EXCEPTION|ANR" | tail -n 120 || true
  return 1
}

adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
  sleep 2
done

adb install -r apk-debug/app-debug.apk
adb install -r apk-sidecar/app-sidecar.apk
adb shell pm list packages | grep -E 'com.fgmachines.rck.debug|com.fgmachines.rck.sidecar161'

# Dashboard gate: launch the real sidecar app and require its MainActivity in foreground.
wake_and_unlock
adb shell am force-stop "$APP_PACKAGE"
adb shell am start -n "$APP_ACTIVITY"
wait_for_main_activity
sleep 3
adb exec-out screencap -p > FG-Link-1.6.1-dashboard.png
test -s FG-Link-1.6.1-dashboard.png

# Developer-page gate: use the sidecar-only test hook to select the actual About/developer page.
# The hook executes showPage(4) inside MainActivity and emits FGLinkUiGate only after doing so.
# Fresh CI emulator: no prior FGLinkUiGate marker exists before this launch.
# Avoid logcat -c because some emulator images reject clearing the main buffer.
adb shell am force-stop "$APP_PACKAGE"
adb shell am start -n "$APP_ACTIVITY" --es fg_ui_test_page about
wait_for_main_activity
sleep 3

adb logcat -d -s FGLinkUiGate:I '*:S' > /tmp/fg-link-about-log.txt || true
cat /tmp/fg-link-about-log.txt
grep -q 'about-page-visible' /tmp/fg-link-about-log.txt

adb exec-out screencap -p > FG-Link-1.6.1-about.png
test -s FG-Link-1.6.1-about.png

echo "FG Link UI branding and developer-page gate passed."
