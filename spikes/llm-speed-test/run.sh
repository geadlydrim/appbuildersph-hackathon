#!/usr/bin/env sh
# PROTOTYPE runner for the LLM speed test. Not the product.
# Usage: ./run.sh <path/to/model> [cpu|gpu] [true|false constrained] [iters] [extra `am start` args...]
# Example: ./run.sh ~/models/gemma3-1b-it-int4.litertlm cpu true 20
# Needs: adb with the phone connected, and the APK installed:
#   ./gradlew :app:installRelease  (or: gradle :app:installRelease)
set -e
export MSYS_NO_PATHCONV=1 # keep Git Bash from rewriting /sdcard paths

MODEL_PATH=$1
BACKEND=${2:-cpu}
CONSTRAINED=${3:-true}
ITERS=${4:-20}
shift $(( $# < 4 ? $# : 4 ))
NAME=$(basename "$MODEL_PATH")
PKG=ph.commutenity.spike
DIR=/sdcard/Android/data/$PKG/files

MODELS=/data/local/tmp/llm
adb shell mkdir -p $MODELS
if ! adb shell ls "$MODELS/$NAME" >/dev/null 2>&1; then
  adb push "$MODEL_PATH" "$MODELS/$NAME"
fi
adb shell am force-stop $PKG
adb logcat -c
adb shell am start -n $PKG/.MainActivity --es model "$NAME" --es backend "$BACKEND" \
  --ez constrained "$CONSTRAINED" --ei iters "$ITERS" "$@"
# Poll the log instead of piping a live `logcat` (it outlives the loop and hangs).
SEEN=0
while :; do
  sleep 5
  LOG=$(adb logcat -d -v raw -s SPIKE:I AndroidRuntime:E libc:F DEBUG:F)
  printf '%s\n' "$LOG" | tail -n +$((SEEN + 1))
  SEEN=$(printf '%s\n' "$LOG" | wc -l)
  case "$LOG" in *SPIKE_DONE*|*SPIKE_ERROR*|*"FATAL EXCEPTION"*|*"Fatal signal"*) break ;; esac
  if [ -z "$(adb shell pidof $PKG)" ]; then echo "app process died"; break; fi
done
adb pull "$DIR/runs/." results/
adb shell rm -rf "$DIR/runs" # so the next pull brings only the next run
