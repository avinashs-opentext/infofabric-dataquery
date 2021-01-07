#!/usr/bin/env bash

EXIT_SUCCESS="0";
EXIT_FAILURE="1";
EXIT_FAILURE_MEM="2";

APP_NAME="dm-dataquery"
APP_BASE_NAME=`basename "$0"`
BDMP_CONFIG_DIR="/opt/bdmp/config"
APP_NAME_SHORT="dm-dataquery";
export HOSTNAME=`hostname`
export app_https_port=$1
export admin_https_port=$2
export override_config_file=$3

# Logstash extra information
export HOST_IPS=$(ifconfig | grep -Eo 'inet (addr:)?([0-9]*\.){3}[0-9]*' | grep -Eo '([0-9]*\.){3}[0-9]*' | grep -v '127.0.0.1' | sed 'N;s/\n/, /;')
export LOCAL_HOSTNAME=$(hostname)
export DAEMON_USER=$(whoami)
export LOGIN_USER=$(who am i | awk '{print $1}')

if [[ -n $4 ]]
then
       DEBUG_PORT=$4
       DEBUG_SUSPEND_ON_START=n
       if [[ $5 = s* ]]
       then
               DEBUG_SUSPEND_ON_START=y
       fi
fi

echo "Launching ${APP_NAME_SHORT} with APP_PORT=${app_https_port}, ADMIN_PORT=${admin_https_port}, CONFIG_FILE=${override_config_file}, DEBUG_PORT=${DEBUG_PORT} SUSPEND=${DEBUG_SUSPEND_ON_START}"

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    msys=true
    ;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched.
if $cygwin ; then
    [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
fi

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/.." >&-
APP_HOME="`pwd -P`"
cd "$SAVED" >&-

APP_PATH_CONFIG_DEFAULT="${APP_HOME}/conf/at4p-dm-dataquery.yml";

# Set the path to the Dropwizard configuration. Use the 3rd parameter to the
# script to indicate said path, if present. Otherwise, default to
# ${APP_HOME}/conf/PROD-binary-object-storage.yml.
if [ -z "${override_config_file}" ]
then
  echo "Using Default Config Path"
  PATH_CONFIG="${APP_PATH_CONFIG_DEFAULT}";
else
  echo "Using Override Config Path"
  PATH_CONFIG="${APP_HOME}/conf/${override_config_file}";
fi

# Add JVM options
# Adding JVM parameters for K8s cgroup support
JVM_OPTS=" -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:+UseG1GC -XX:MaxRAMFraction=2 ";
# [JVM-option] Application port (dw.server.applicationConnectors[0].port)
JVM_OPTS="${JVM_OPTS} -Ddw.server.applicationConnectors[0].port=${app_https_port}";
# [JVM-option] Admin port (dw.server.adminConnectors[0].port)
JVM_OPTS="${JVM_OPTS} -Ddw.server.adminConnectors[0].port=${admin_https_port}";
# [JVM-option] Classpath: library JARs and BDMP configurations
JVM_OPTS="${JVM_OPTS} -cp ${BDMP_CONFIG_DIR}:${APP_HOME}/lib/*";

if [[ -n $DEBUG_PORT ]]
then
    JVM_OPTS="${JVM_OPTS} -Xrunjdwp:transport=dt_socket,address=${DEBUG_PORT},server=y,suspend=${DEBUG_SUSPEND_ON_START}"
fi

if [ -r "/opt/liaison/components/alloy-truststore/cacerts" ];
then
    JVM_OPTS="${JVM_OPTS} -Djavax.net.ssl.trustStore=/opt/liaison/components/alloy-truststore/cacerts \
    -Djavax.net.ssl.trustStorePassword=ch@ng3d1t \
    -Djavax.net.ssl.trustStoreType=JKS"
fi

MAINCLASS="DataqueryApplication";
MAINARGS="server ${PATH_CONFIG}";

# Force zookeeper not to use sasl client, as it would otherwise use it if java.security.auth.login.config is set
JVM_OPTS="${JVM_OPTS} -Dzookeeper.sasl.client=false"

export MAPR_TICKETFILE_LOCATION=/opt/bdmp/secure/mapr/d2app-mapr-service-ticket;

cd $APP_HOME
if [ "$NEWRELIC_AGENT_ENABLE" == "true" ]
then
echo "Starting ${APP_NAME_SHORT}: java $($NEWRELIC) java ${JVM_OPTS} ${MAINCLASS} ${MAINARGS}";
exec java $($NEWRELIC) ${JVM_OPTS} ${MAINCLASS} ${MAINARGS};
else
echo "Starting ${APP_NAME_SHORT}: java ${JVM_OPTS} ${MAINCLASS} ${MAINARGS}";
exec java ${JVM_OPTS} ${MAINCLASS} ${MAINARGS};
fi
