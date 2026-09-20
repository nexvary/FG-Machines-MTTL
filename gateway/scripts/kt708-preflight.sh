#!/bin/sh
set -u

echo "=== FG Link / KT708 preflight ==="
date 2>/dev/null || true
echo
echo "[system]"
uname -a 2>/dev/null || true
command -v ubus >/dev/null 2>&1 && ubus call system board 2>/dev/null || true
echo
echo "[memory]"
free -h 2>/dev/null || cat /proc/meminfo 2>/dev/null | head -n 8
echo
echo "[storage]"
df -h 2>/dev/null || true
echo
echo "[interfaces]"
ip -o link 2>/dev/null || ifconfig -a 2>/dev/null || true
echo
echo "[IPv4]"
ip -o -4 addr show 2>/dev/null || true
echo
echo "[routes]"
ip route 2>/dev/null || route -n 2>/dev/null || true
echo
echo "[ZeroTier]"
if command -v zerotier-cli >/dev/null 2>&1; then
  zerotier-cli status 2>/dev/null || true
  zerotier-cli listnetworks 2>/dev/null || true
else
  echo "zerotier-cli not found"
fi
echo
echo "[OpenWrt packages]"
command -v opkg >/dev/null 2>&1 && opkg list-installed 2>/dev/null | grep -E 'zerotier|firewall|luci|ca-bundle' || true
echo
echo "[listening ports]"
if command -v ss >/dev/null 2>&1; then
  ss -lntup 2>/dev/null | grep -E '(:10086|:18086|zerotier)' || true
elif command -v netstat >/dev/null 2>&1; then
  netstat -lntup 2>/dev/null | grep -E '(:10086|:18086|zerotier)' || true
fi
echo
echo "[suggested gateway target]"
ARCH="$(uname -m 2>/dev/null || echo unknown)"
case "$ARCH" in
  mipsel*|mipsle*) echo "binary: fg-link-gateway-linux-mipsle" ;;
  mips*)           echo "binary: fg-link-gateway-linux-mips" ;;
  armv7*|armv6*)   echo "binary: fg-link-gateway-linux-armv7" ;;
  aarch64*|arm64*) echo "binary: fg-link-gateway-linux-arm64" ;;
  x86_64*)         echo "binary: fg-link-gateway-linux-amd64" ;;
  *)               echo "binary target must be confirmed manually for: $ARCH" ;;
esac
echo
echo "Preflight is read-only. No firmware, firewall, routes or ZeroTier settings were changed."
