#!/bin/bash
cd -- "$(dirname -- "$0")" || exit 1
if ! command -v node >/dev/null 2>&1; then
  echo "Install Node.js 24.15+ (24.x), then run this launcher again."
  read -r -p "Press Return to close. "
  exit 1
fi
exec node server/index.mjs
