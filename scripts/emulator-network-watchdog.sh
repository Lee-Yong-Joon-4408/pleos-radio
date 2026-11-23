#!/usr/bin/env bash
# Emulator network watchdog for Pleos Radio development.
#
# Runs on the host PC. While the host is online, it checks the Android emulator's network every
# INTERVAL seconds. When the emulator has lost its network (no default network / cannot reach the
# internet) for two checks in a row, it repairs it so live broadcast audio can play again:
#   1) in-place reboot (adb reboot)          -- restores the virtual network in ~15-20 s
#   2) cold boot of the AVD (kill + restart) -- if the reboot did not help
# and then relaunches Pleos Radio if it was running.
#
# Why on the host: the app runs inside the emulator without system privileges (and the Pleos image
# has no root), so it cannot restart Android's network stack itself.
#
# Usage:
#   scripts/emulator-network-watchdog.sh [--avd NAME] [--interval SEC] [--start] [--once]
#     --avd NAME      AVD to cold-boot / start (default: Pleos_Connect_v2.1)
#     --interval SEC  seconds between checks (default: 15)
#     --start         start the emulator first if none is running
#     --once          check (and repair) once, then exit
#   Stop with Ctrl+C. Log: printed to stdout.

set -u

AVD="Pleos_Connect_v2.1"
INTERVAL=15
START=0
ONCE=0
while [ $# -gt 0 ]; do
  case "$1" in
    --avd) AVD="$2"; shift 2 ;;
    --interval) INTERVAL="$2"; shift 2 ;;
    --start) START=1; shift ;;
    --once) ONCE=1; shift ;;
    -h|--help) sed -n '2,22p' "$0"; exit 0 ;;
    *) echo "unknown option: $1" >&2; exit 2 ;;
  esac
done

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
ADB="$SDK/platform-tools/adb"
EMULATOR="$SDK/emulator/emulator"
PACKAGES="com.pleos.radio.debug com.pleos.radio"
ACTIVITY="com.pleos.radio.presentation.MainActivity"   # full class name: the .debug suffix applies to the package only
[ -x "$ADB" ] || { echo "adb not found at $ADB (set ANDROID_HOME)" >&2; exit 1; }

log() { echo "$(date '+%H:%M:%S') [watchdog] $*"; }

serial() { "$ADB" devices | awk '/^emulator-[0-9]+[[:space:]]+device$/ {print $1; exit}'; }
sh_() { "$ADB" -s "$SERIAL" shell "$@" 2>/dev/null | tr -d '\r'; }

host_online() {
  local code
  code=$(curl -s -o /dev/null -m 5 -w '%{http_code}' https://www.google.com/generate_204 2>/dev/null)
  [ "$code" = "204" ] || [ "$code" = "200" ] && return 0
  curl -s -o /dev/null -m 5 https://1.1.1.1 2>/dev/null
}

booted() { [ "$(sh_ getprop sys.boot_completed)" = "1" ]; }

emu_online() {
  sh_ dumpsys connectivity | grep -m1 "Active default network" | grep -qv "none" || return 1
  sh_ ping -c 1 -W 3 8.8.8.8 | grep -q "1 received"
}

wait_boot() {  # $1 = timeout seconds
  local t=0
  while [ $t -lt "$1" ]; do
    SERIAL=$(serial); [ -n "$SERIAL" ] && booted && return 0
    sleep 3; t=$((t + 3))
  done
  return 1
}

wait_network() {  # $1 = timeout seconds
  local t=0
  while [ $t -lt "$1" ]; do emu_online && return 0; sleep 3; t=$((t + 3)); done
  return 1
}

running_app() {
  for p in $PACKAGES; do [ -n "$(sh_ pidof "$p")" ] && { echo "$p"; return 0; }; done
  return 1
}

launch_app() {  # $1 = package
  [ -n "${1:-}" ] || return 0
  # Android Automotive boots into the headless system user 0 and switches to the driver's user
  # (10 on Pleos) a moment later. Launching before that user is running and unlocked lands in user 0
  # and fails, so wait for RUNNING_UNLOCKED, then launch for that user explicitly (with retries).
  local t=0 user="0"
  while [ $t -lt 120 ]; do
    user=$(sh_ am get-current-user)
    if [ -n "$user" ] && [ "$user" != "0" ] && [ "$(sh_ am get-started-user-state "$user")" = "RUNNING_UNLOCKED" ]; then break; fi
    sleep 3; t=$((t + 3))
  done
  # "Unlocked" comes before that user's screens are in front; launches before then still go to user 0.
  # Wait until an activity of the driver's user is resumed (home screen / driving view).
  t=0
  while [ $t -lt 120 ]; do
    sh_ dumpsys activity activities | grep -q "topResumedActivity=ActivityRecord{[0-9a-f]* u$user " && break
    sleep 3; t=$((t + 3))
  done
  sleep 5   # let the launcher / car services settle
  local attempt
  for attempt in 1 2 3 4 5 6; do
    sh_ am start --user "$user" -n "$1/$ACTIVITY" >/dev/null
    sleep 5
    if [ -n "$(sh_ pidof "$1")" ]; then log "relaunched $1 (user $user)"; return 0; fi
    sleep 5
  done
  log "relaunch of $1 did not start (user $user)"
}

start_emulator() {
  log "starting AVD $AVD (cold boot)"
  nohup "$EMULATOR" -avd "$AVD" -no-snapshot-load -no-snapshot-save -no-boot-anim >"${TMPDIR:-/tmp}/pleos-emulator-watchdog.log" 2>&1 &
}

repair() {
  local app; app=$(running_app || true)
  log "emulator network is down while the host is online -> repairing (app running: ${app:-no})"

  log "step 1: in-place reboot"
  "$ADB" -s "$SERIAL" reboot >/dev/null 2>&1
  sleep 5
  if wait_boot 240 && wait_network 60; then
    log "network restored after reboot"; launch_app "$app"; return 0
  fi

  log "step 2: cold boot of $AVD"
  "$ADB" -s "$SERIAL" emu kill >/dev/null 2>&1
  local t=0; while [ -n "$(serial)" ] && [ $t -lt 40 ]; do sleep 2; t=$((t + 2)); done
  start_emulator
  if wait_boot 420 && wait_network 90; then
    log "network restored after cold boot"; launch_app "$app"; return 0
  fi
  log "repair failed; will retry on the next checks"
  return 1
}

SERIAL=$(serial)
if [ -z "$SERIAL" ]; then
  if [ $START -eq 1 ]; then start_emulator; wait_boot 420 || { log "emulator did not boot"; exit 1; }
  else log "no running emulator (use --start to launch $AVD)"; fi
fi
log "watching ${SERIAL:-<none>} every ${INTERVAL}s (AVD $AVD)"

fails=0
while true; do
  SERIAL=$(serial)
  if [ -n "$SERIAL" ] && booted; then
    if emu_online; then
      fails=0
    elif host_online; then
      fails=$((fails + 1))
      log "emulator offline, host online (${fails}/2)"
      if [ $fails -ge 2 ]; then
        repair
        fails=0
        sleep 30   # cool-down after a repair
      fi
    else
      fails=0      # host itself is offline: nothing to repair
    fi
  fi
  [ $ONCE -eq 1 ] && [ $fails -eq 0 ] && break
  sleep "$INTERVAL"
done
