#!/usr/bin/env bash
set -e

CONFIG_FILE=./trust-framework-service-provider.yml

./gradlew installDist

LOCAL_IP="$(ipconfig getifaddr en0)"
export REDIS_URI="redis://${LOCAL_IP}:6380"

CID=$(docker ps -q -f status=running -f name=clientRedis)
if [ ! "${CID}" ]; then
    echo "Starting client redis"
    docker run --name clientRedis -d -p 6380:6379 --rm redis
fi


./build/install/trust-framework-service-provider-prototype/bin/trust-framework-service-provider-prototype server $CONFIG_FILE
