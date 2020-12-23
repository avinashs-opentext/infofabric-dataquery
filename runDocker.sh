#!/bin/bash -x

# Usage: ./runDocker.sh

SERVICE_NAME=dataquery

SERVICE_VERSION=$( awk '/^## \[([0-9])/{ print (substr($2, 2, length($2) - 2));exit; }' CHANGELOG.md )

export SERVICE_VERSION=$SERVICE_VERSION

if [ -z "$DM_ENV" ]; then
    DM_ENV=dev
fi

if [ -d "/opt/bdmp" ]; then
    VOLUME_MOUNT="-v /opt/bdmp:/opt/bdmp"
fi

if [ -f ~/.d2app_passwd ]; then
    PASSWD=$(cat ~/.d2app_passwd)
fi

docker network create dataquery

./gradlew installDist

docker stop $SERVICE_NAME

docker rm $SERVICE_NAME

docker build -t dm-${SERVICE_NAME} .

docker run --name $SERVICE_NAME --net=dataquery ${VOLUME_MOUNT} -e DM_ENV=${DM_ENV} -e SERVICE_VERSION=${SERVICE_VERSION} -p 9443:9443 -p 9444:9444 -p ${SERVICE_DEBUG_PORT}:5005 -d dm-${SERVICE_NAME}
