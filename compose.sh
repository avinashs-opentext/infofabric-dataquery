#!/bin/bash

export SERVICE_VERSION=$( awk '/^## \[([0-9])/{ print (substr($2, 2, length($2) - 2));exit; }' CHANGELOG.md )
export LOCAL_HOSTNAME=$( hostname )
EXTRA_NODES=${NODES:-0}

echo "distTar service..."
./gradlew installDist

echo "docker-compose up -d --scale dataquery-node=$EXTRA_NODES --build"
docker-compose up -d --scale dataquery-node=$EXTRA_NODES --build

echo "Done."
