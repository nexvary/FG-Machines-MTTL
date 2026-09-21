#!/usr/bin/env bash
set -euo pipefail

APP_PACKAGE="com.fgmachines.rck.sidecar162"
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

  # The hosted Linux runner has no KVM. Repeated wake/input/settings commands
  # during activity launch can starve the software-emulated guest and prevent
  # MainActivity from becoming resumed. Wake once before launch, then poll only.
  while [ "$attempt" -le 30 ]; do
    if main_activity_is_foreground; then
      return 0
    fi
    sleep 3
    attempt=$((attempt + 1))
  done

  echo "UI gate failed: FG Link MainActivity is not foreground" >&2
  grep -E 'mResumedActivity|topResumedActivity' /tmp/fg-link-activities.txt || true
  adb shell dumpsys window windows | grep -E 'mCurrentFocus|mFocusedApp' || true
  adb shell pidof "$APP_PACKAGE" || true
  adb logcat -d -t 2000 | grep -E "$APP_PACKAGE|FGLinkUiGate|ActivityTaskManager|ActivityManager|AndroidRuntime|FATAL EXCEPTION|Force finishing|ANR|am_crash|am_anr" | tail -n 400 || true
  return 1
}

adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
  sleep 2
done

adb install -r apk-debug/app-debug.apk
adb install -r apk-sidecar/app-sidecar.apk
adb shell pm list packages | grep -E 'com.fgmachines.rck.debug|com.fgmachines.rck.sidecar162'

# Dashboard gate: launch the real sidecar app and require its MainActivity in foreground.
wake_and_unlock
adb shell run-as "$APP_PACKAGE" rm -f files/fg_ui_gate_state >/dev/null 2>&1 || true
adb shell am force-stop "$APP_PACKAGE"
sleep 1
wake_and_unlock
adb logcat -c >/dev/null 2>&1 || true
adb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n "$APP_ACTIVITY" --es fg_ui_test_page dashboard
wait_for_main_activity

# MainActivity may become resumed a moment before onCreate() finishes writing
# the gate marker, especially on the software-emulated hosted runner.
DASHBOARD_MARKER=""
attempt=1
while [ "$attempt" -le 30 ]; do
  DASHBOARD_MARKER="$(adb shell run-as "$APP_PACKAGE" cat files/fg_ui_gate_state 2>/dev/null | tr -d '\r\n' || true)"
  if [ "$DASHBOARD_MARKER" = "dashboard-page-visible" ]; then
    break
  fi
  sleep 1
  attempt=$((attempt + 1))
done
if [ "$DASHBOARD_MARKER" != "dashboard-page-visible" ]; then
  echo "UI gate failed: dashboard-page marker was not written" >&2
  adb logcat -d -t 2000 | grep -E "$APP_PACKAGE|FGLinkUiGate|ActivityTaskManager|ActivityManager|AndroidRuntime|FATAL EXCEPTION|ANR" | tail -n 400 || true
  exit 1
fi
sleep 2
adb exec-out screencap -p > FG-Link-1.6.2-dashboard.png
test -s FG-Link-1.6.2-dashboard.png

# Developer-page gate: use the sidecar-only test hook to select the actual About/developer page.
# The hook executes showPage(4) inside MainActivity and emits FGLinkUiGate only after doing so.
# Fresh CI emulator: no prior FGLinkUiGate marker exists before this launch.
# Avoid logcat -c because some emulator images reject clearing the main buffer.
adb shell run-as "$APP_PACKAGE" rm -f files/fg_ui_gate_state >/dev/null 2>&1 || true
adb shell am force-stop "$APP_PACKAGE"
sleep 1
wake_and_unlock
adb logcat -c >/dev/null 2>&1 || true
adb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n "$APP_ACTIVITY" --es fg_ui_test_page about
wait_for_main_activity

ABOUT_MARKER=""
attempt=1
while [ "$attempt" -le 20 ]; do
  ABOUT_MARKER="$(adb shell run-as "$APP_PACKAGE" cat files/fg_ui_gate_state 2>/dev/null | tr -d '\r\n' || true)"
  if [ "$ABOUT_MARKER" = "about-page-visible" ]; then
    break
  fi
  sleep 2
  attempt=$((attempt + 1))
done
if [ "$ABOUT_MARKER" != "about-page-visible" ]; then
  echo "UI gate failed: developer-page marker was not written" >&2
  adb shell run-as "$APP_PACKAGE" ls -la files 2>/dev/null || true
  return_code=1
  adb logcat -d -t 300 | grep -E "$APP_PACKAGE|FGLinkUiGate|AndroidRuntime|FATAL EXCEPTION|ANR" | tail -n 120 || true
  exit "$return_code"
fi
sleep 2

adb exec-out screencap -p > FG-Link-1.6.2-about.png
test -s FG-Link-1.6.2-about.png

echo "FG Link UI branding and developer-page gate passed."

capture_main_page() {
  local page="$1"
  local file="$2"
  adb shell am force-stop "$APP_PACKAGE"
  sleep 1
  wake_and_unlock
  adb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER \
    -n "$APP_ACTIVITY" --es fg_ui_test_page "$page" >/dev/null
  wait_for_main_activity
  sleep 2
  adb exec-out screencap -p > "$file"
  test -s "$file"
}

wait_for_component() {
  local component="$1"
  local attempt=1
  while [ "$attempt" -le 40 ]; do
    adb shell dumpsys activity activities > /tmp/fg-link-components.txt
    if grep -E 'mResumedActivity|topResumedActivity' /tmp/fg-link-components.txt | grep -q "$component"; then
      return 0
    fi
    sleep 2
    attempt=$((attempt + 1))
  done
  echo "UI screenshot gate failed: $component is not foreground" >&2
  grep -E 'mResumedActivity|topResumedActivity' /tmp/fg-link-components.txt || true
  return 1
}

capture_child_page() {
  local page="$1"
  local component="$2"
  local file="$3"
  adb shell am force-stop "$APP_PACKAGE"
  sleep 1
  wake_and_unlock
  adb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER \
    -n "$APP_ACTIVITY" --es fg_ui_test_page "$page" >/dev/null
  wait_for_component "$component"
  sleep 3
  adb exec-out screencap -p > "$file"
  test -s "$file"
}

# Complete real-page gallery from the same 1.6.2 sidecar APK.
# Keep every capture in the release artifact so user-visible screenshots match the tested APK.
capture_main_page setup       FG-Link-1.6.2-setup.png
capture_main_page scan        FG-Link-1.6.2-scan.png
capture_main_page settings    FG-Link-1.6.2-settings.png
capture_main_page subscriber  FG-Link-1.6.2-subscriber.png

capture_child_page remote_ac \
  "$APP_PACKAGE/com.fgmachines.rck.RemoteActivity" \
  FG-Link-1.6.2-remote-ac.png

capture_child_page remote_fan \
  "$APP_PACKAGE/com.fgmachines.rck.RemoteActivity" \
  FG-Link-1.6.2-remote-fan.png

capture_child_page diagnostics \
  "$APP_PACKAGE/com.fgmachines.rck.DiagnosticsActivity" \
  FG-Link-1.6.2-diagnostics.png

capture_child_page network_doctor \
  "$APP_PACKAGE/com.fgmachines.rck.NetworkDoctorActivity" \
  FG-Link-1.6.2-network-doctor.png

echo "FG Link complete real-page screenshot gallery passed."
