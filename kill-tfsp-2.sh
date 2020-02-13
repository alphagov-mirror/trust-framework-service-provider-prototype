#!/usr/bin/env bash
set -e

if [ -f "./tmp/pids/tfsp-2.pid" ]; then
  echo "Killing tfsp-2"
  kill "$(< ./tmp/pids/tfsp-2.pid)" || true
  rm -f ./tmp/pids/tfsp-2.pid
fi
