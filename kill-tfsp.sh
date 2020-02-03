#!/usr/bin/env bash
set -e

if [ -f "./tmp/pids/tfsp.pid" ]; then
  echo "Killing tfsp"
  kill "$(< ./tmp/pids/tfsp.pid)" || true
  rm -f ./tmp/pids/tfsp.pid
fi
