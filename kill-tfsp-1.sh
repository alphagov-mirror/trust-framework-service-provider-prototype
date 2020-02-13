#!/usr/bin/env bash
set -e

if [ -f "./tmp/pids/tfsp-1.pid" ]; then
  echo "Killing tfsp-1"
  kill "$(< ./tmp/pids/tfsp-1.pid)" || true
  rm -f ./tmp/pids/tfsp-1.pid
fi
