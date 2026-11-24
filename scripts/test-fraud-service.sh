#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../fraud-service"
python3 -m pytest -q
