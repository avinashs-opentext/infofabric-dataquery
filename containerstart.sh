#!/bin/bash

if [ -z "$DM_ENV" ]; then
    ENV="dev"
else
    ENV=$DM_ENV
fi

if [ -z "$DEBUG_PORT" ]; then
    PORT=5005
else
    PORT=$DEBUG_PORT
fi

/opt/dm-dataquery/bin/deploy.sh 9443 9444 k8s-dm-dataquery.yml ${PORT}
