#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

if [ -n "$(git status --porcelain --untracked-files=no)" ]; then
  echo "Refusing to pull: tracked files have local changes"
  git status -sb
  exit 1
fi

git fetch origin
git checkout main
git branch --set-upstream-to=origin/main main >/dev/null 2>&1 || true
git pull --ff-only origin main

git status -sb
git log --oneline -n 5
