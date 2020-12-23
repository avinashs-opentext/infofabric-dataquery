#!/bin/bash

ARTIFACT_URL=$1
DEPLOY_ENVIRONMENT=$2
PORT=$3
VERSION=$(echo $ARTIFACT_URL | rev | cut -d\/ -f2 | rev)

mkdir -p /opt/bdmp/artifacts/com/liaison/dataquery/$VERSION
cd /opt/bdmp/artifacts/com/liaison/dataquery/$VERSION

if [ -z "$ARTIFACT_URL" ]; then
    echo "ERROR: ARTIFACT URL is missing"
    exit 1
fi;

if [ -z "$DEPLOY_ENVIRONMENT" ]; then
    echo "ERROR: DEPLOY ENVIRONMENT is missing"
    exit 1
fi

if [ -z "$PORT" ]; then
    echo "ERROR: PORT is missing"
    exit 1
fi

echo "Downloading artifact: $ARTIFACT_URL"
curl -O $ARTIFACT_URL

ARTIFACT_NAME=$(echo $ARTIFACT_URL | rev | cut -d\/ -f 1 | rev)

# stop dataquery runtime
PID=$(ps -ef | awk '/[c]om.liaison.dataquery.DataqueryApplication/{print $2}')
if [ ! -z "$PID" ]; then
    kill $PID
    sleep 5
fi

PID=$(ps -ef | awk '/[c]om.liaison.dataquery.DataqueryApplication/{print $2}')
if [ ! -z "$PID" ]; then
    kill -9 $PID
fi

find . -maxdepth 1 -name "dm-dataquery-*" -type d -exec rm -r {} \; | :

echo "Extracting artifact: $ARTIFACT_NAME"
tar xzf $ARTIFACT_NAME
rm -f dm-dataquery*.tgz
cd dm-dataquery*

mkdir -p /opt/bdmp/logs
su d2app -c "nohup ./bin/deploy.sh 9443 9444 $DEPLOY_ENVIRONMENT-dm-dataquery.yml > /opt/bdmp/logs/dm-dataquery.console.out 2>&1 &"

# Remove old dataquery deployments (keep 4 most recent versions)
cd /opt/bdmp/artifacts/com/liaison/dataquery
ls -tr /opt/bdmp/artifacts/com/liaison/dataquery | grep '^[0-9]\+\.[0-9]\+\.[0-9]\+\(-SNAPSHOT\)\?$' > /tmp/old_dm_dataquery_versions
COUNT=$(cat /tmp/old_dm_dataquery_versions | wc -l | tr -d '[:space:]')

if [ $COUNT -gt 4 ]; then
    echo $(head -n$(($COUNT - 4)) /tmp/old_dm_dataquery_versions) | xargs rm -rf --
fi
