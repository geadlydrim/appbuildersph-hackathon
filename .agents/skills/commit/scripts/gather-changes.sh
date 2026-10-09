#!/bin/sh
# Compact, single-pass snapshot of all working-tree changes for the /commit skill.
# Prints status, a size overview, and a bounded per-file diff so the caller never
# needs to make more than one Bash call (or full-file dumps) to see what changed.
set -e

if git rev-parse HEAD >/dev/null 2>&1; then
  BASE=HEAD
else
  # No commits yet: diff against the empty tree instead.
  BASE=$(git hash-object -t tree /dev/null)
fi

is_skippable() {
  case "$1" in
    *package-lock.json|*pnpm-lock.yaml|*yarn.lock|*.lock|*.min.js|*.min.css|*.map|dist/*|build/*|node_modules/*|vendor/*)
      return 0 ;;
    *)
      return 1 ;;
  esac
}

echo "=== STATUS ==="
git status --porcelain=v1 -uall

echo
echo "=== DIFF STAT ==="
git diff --stat "$BASE" --

echo
echo "=== PER-FILE DIFFS ==="

git diff "$BASE" --name-only -- | while IFS= read -r f; do
  [ -z "$f" ] && continue
  if is_skippable "$f"; then
    echo "--- $f (generated/lock file, skipped) ---"
    continue
  fi
  echo "--- $f ---"
  diff_out=$(git diff "$BASE" -U2 -- "$f")
  lines=$(printf '%s\n' "$diff_out" | wc -l)
  if [ "$lines" -gt 150 ]; then
    printf '%s\n' "$diff_out" | head -150
    echo "... [truncated, $lines total lines - request full diff for this file if still ambiguous]"
  else
    printf '%s\n' "$diff_out"
  fi
done

git status --porcelain=v1 -uall | awk '/^\?\? /{print substr($0,4)}' | while IFS= read -r f; do
  [ -z "$f" ] && continue
  if is_skippable "$f"; then
    echo "--- $f (new, generated/lock file, skipped) ---"
    continue
  fi
  echo "--- $f (new file) ---"
  if [ -f "$f" ]; then
    lines=$(wc -l < "$f")
    if [ "$lines" -gt 100 ]; then
      head -100 -- "$f"
      echo "... [truncated, $lines total lines]"
    else
      cat -- "$f"
    fi
  else
    echo "[not a regular file, e.g. directory or binary]"
  fi
done
