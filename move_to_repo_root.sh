#!/usr/bin/env bash
set -euo pipefail

# Usage: ./move_to_repo_root.sh "WhatsApp Messenger"
if [[ $# -ne 1 ]]; then
  echo "Usage: $0 \"<nested-folder-name>\"" >&2
  exit 1
fi

src_dir="$1"
repo_root="$(pwd)"

if [[ ! -d "$src_dir" ]]; then
  echo "Error: directory not found: $src_dir" >&2
  exit 1
fi

# Include both visible and hidden entries; nullglob avoids errors when no matches exist.
shopt -s dotglob nullglob
entries=("$src_dir"/*)
shopt -u dotglob

if (( ${#entries[@]} > 0 )); then
  mv -- "${entries[@]}" "$repo_root"/
fi

# Remove the original folder once everything is moved.
rmdir -- "$src_dir"

echo "Moved contents of '$src_dir' to '$repo_root' and removed '$src_dir'."
