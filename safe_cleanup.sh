#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(pwd)"
TARGET_DIR="${REPO_ROOT}/WhatsAppMessenger"

log() {
  printf '%s\n' "$*"
}

print_structure() {
  local label="$1"
  log ""
  log "=== ${label} ==="
  # Show a concise project tree while skipping .git internals
  find . -path './.git' -prune -o -print | sed 's#^\./##' | sort
}

safe_move_from_target() {
  local moved_count=0
  local skipped_count=0
  shopt -s dotglob nullglob

  local items=("${TARGET_DIR}"/* "${TARGET_DIR}"/.[!.]* "${TARGET_DIR}"/..?*)
  shopt -u dotglob

  if (( ${#items[@]} == 0 )); then
    log "No files found inside WhatsAppMessenger."
    return 0
  fi

  for src in "${items[@]}"; do
    [[ -e "${src}" ]] || continue
    local base
    base="$(basename "${src}")"

    # Skip safety-sensitive names even if present in nested directory
    if [[ "${base}" == ".git" ]]; then
      log "SKIP move: ${src} (.git must never be moved/deleted)"
      ((skipped_count+=1))
      continue
    fi

    local dst="${REPO_ROOT}/${base}"
    if [[ -e "${dst}" ]]; then
      log "WARNING: Not moving ${src} -> ${dst} (destination exists; no overwrite)."
      ((skipped_count+=1))
      continue
    fi

    log "MOVE: ${src} -> ${dst}"
    mv -- "${src}" "${dst}"
    ((moved_count+=1))
  done

  log "Move summary: moved=${moved_count}, skipped=${skipped_count}"

  if find "${TARGET_DIR}" -mindepth 1 -print -quit | grep -q .; then
    log "WARNING: WhatsAppMessenger still contains files (likely conflicts)."
    return 1
  fi

  return 0
}

remove_nested_gradle_dirs() {
  log ""
  log "Scanning for nested .gradle folders to delete (excluding ./.git and root ./.gradle)..."
  while IFS= read -r gradle_dir; do
    [[ -n "${gradle_dir}" ]] || continue
    log "DELETE: ${gradle_dir}"
    rm -rf -- "${gradle_dir}"
  done < <(find . -path './.git' -prune -o -type d -name '.gradle' ! -path './.gradle' -print)
}

log "About to perform safe cleanup in: ${REPO_ROOT}"
log "Planned actions:"
log "1) Print directory structure before changes"
log "2) If ./WhatsAppMessenger exists, move its contents (including hidden files) to repo root"
log "3) Delete ./WhatsAppMessenger only if empty after move"
log "4) Delete nested .gradle cache folders"
log "5) Print directory structure after changes"

print_structure "Directory structure BEFORE changes"

if [[ -d "${TARGET_DIR}" ]]; then
  log ""
  log "Found folder: ${TARGET_DIR}"

  if safe_move_from_target; then
    log "DELETE: ${TARGET_DIR}"
    rmdir -- "${TARGET_DIR}"
  else
    log "WARNING: ${TARGET_DIR} not deleted because it is not empty."
  fi
else
  log ""
  log "Folder not found: ${TARGET_DIR}"
fi

remove_nested_gradle_dirs

print_structure "Directory structure AFTER changes"
log "Cleanup script finished."
