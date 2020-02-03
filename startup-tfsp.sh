#!/usr/bin/env bash
set -e

CONFIG_FILE=./trust-framework-service-provider-prototype.yml
LOCAL_IP="$(ipconfig getifaddr en0)"
export REDIS_URI="redis://${LOCAL_IP}:6380"
log="logs/tfsp_console.log"

cd "$(dirname "$0")"

./gradlew installDist

CID=$(docker ps -q -f status=running -f name=clientRedis)
if [ ! "${CID}" ]; then
    echo "Starting client redis"
    docker run --name clientRedis -d -p 6380:6379 --rm redis
fi

PID_DIR=./tmp/pids
if [ ! -d $PID_DIR ]; then
    echo -e 'Creating PIDs directory\n'
    mkdir -p $PID_DIR
fi

LOGS_DIR=./logs
if [ ! -d $LOGS_DIR ]; then
  echo -e 'Creating LOGs directory\n'
  mkdir -p $LOGS_DIR
fi

./build/install/trust-framework-service-provider-prototype/bin/trust-framework-service-provider-prototype server $CONFIG_FILE &
  echo $! > ./tmp/pids/tfsp.pid
