#!/bin/bash

DEPLOY_ENVIRONMENT=$1
ARTIFACT_URL=$(cat ./build/publish.log | grep '\.tgz$' | cut -f2 -d\ )
PORT=9000

if [ -z "$ARTIFACT_URL" ]; then
    echo "ERROR: ARTIFACT URL is null"
    exit 1
fi

if [ -z "$DEPLOY_ENVIRONMENT" ]; then
    echo "ERROR: DEPLOY ENVIRONMENT is null"
    exit 1
fi

if [ "$DEPLOY_ENVIRONMENT" == "at4d" ]; then
    scp -o StrictHostKeyChecking=no jenkins_deploy_dm_dataquery.sh svc_jenkins@<UNKNOWN_TBD>:/tmp
    scp -o StrictHostKeyChecking=no jenkins_deploy_dm_dataquery.sh svc_jenkins@<UNKNOWN_TBD>:/tmp

    ssh -o StrictHostKeyChecking=no svc_jenkins@at4d-lvdmcr01.liaison.dev "cd /tmp; sudo ./jenkins_deploy_dm_dataquery.sh $ARTIFACT_URL $DEPLOY_ENVIRONMENT $PORT"
    ssh -o StrictHostKeyChecking=no svc_jenkins@at4d-lvdmcr02.liaison.dev "cd /tmp; sudo ./jenkins_deploy_dm_dataquery.sh $ARTIFACT_URL $DEPLOY_ENVIRONMENT $PORT"
elif [ "$DEPLOY_ENVIRONMENT" == "at4q" ]; then
    scp -o StrictHostKeyChecking=no jenkins_deploy_dm_dataquery.sh svc_jenkins@<UNKNOWN_TBD>:/tmp
    scp -o StrictHostKeyChecking=no jenkins_deploy_dm_dataquery.sh svc_jenkins@<UNKNOWN_TBD>:/tmp

    ssh -o StrictHostKeyChecking=no svc_jenkins@at4q-lvdmcr01.liaison.dev "cd /tmp; sudo ./jenkins_deploy_dm_dataquery.sh $ARTIFACT_URL $DEPLOY_ENVIRONMENT $PORT"
    ssh -o StrictHostKeyChecking=no svc_jenkins@at4q-lvdmcr02.liaison.dev "cd /tmp; sudo ./jenkins_deploy_dm_dataquery.sh $ARTIFACT_URL $DEPLOY_ENVIRONMENT $PORT"
else
    echo "ERROR: Invalid environment"
    exit 1
fi

