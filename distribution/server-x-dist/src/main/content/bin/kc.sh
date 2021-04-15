#!/bin/bash

case "`uname`" in
    CYGWIN*)
        CFILE = `cygpath "$0"`
        RESOLVED_NAME=`readlink -f "$CFILE"`
        ;;
    Darwin*)
        RESOLVED_NAME=`readlink "$0"`
        ;;
    FreeBSD)
        RESOLVED_NAME=`readlink -f "$0"`
        ;;
    Linux)
        RESOLVED_NAME=`readlink -f "$0"`
        ;;
esac

if [ "x$RESOLVED_NAME" = "x" ]; then
    RESOLVED_NAME="$0"
fi

GREP="grep"
DIRNAME=`dirname "$RESOLVED_NAME"`

SERVER_OPTS="-Dkc.home.dir=$DIRNAME/../ -Djboss.server.config.dir=$DIRNAME/../conf -Dkeycloak.theme.dir=$DIRNAME/../themes -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

DEBUG_MODE="${DEBUG:-false}"
DEBUG_PORT="${DEBUG_PORT:-8787}"

CONFIG_ARGS=${CONFIG_ARGS:-""}
IS_CONFIGURE="false"

while [ "$#" -gt 0 ]
do
    case "$1" in
      --debug)
          DEBUG_MODE=true
          if [ -n "$2" ] && [ "$2" = `echo "$2" | sed 's/-//'` ]; then
              DEBUG_PORT=$2
              shift
          fi
          ;;
      --)
          shift
          break
          ;;
      *)
          if [[ $1 = --* || ! $1 =~ ^-.* ]]; then
            CONFIG_ARGS="$CONFIG_ARGS $1"
          else
            SERVER_OPTS="$SERVER_OPTS $1"
          fi
          ;;
    esac
    shift
done

#
# Specify options to pass to the Java VM.
#
if [ "x$JAVA_OPTS" = "x" ]; then
   JAVA_OPTS="-Xms64m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m -Djava.net.preferIPv4Stack=true"
else
   echo "JAVA_OPTS already set in environment; overriding default settings with values: $JAVA_OPTS"
fi

# Set debug settings if not already set
if [ "$DEBUG_MODE" = "true" ]; then
    DEBUG_OPT=`echo $JAVA_OPTS | $GREP "\-agentlib:jdwp"`
    if [ "x$DEBUG_OPT" = "x" ]; then
        JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,address=$DEBUG_PORT,server=y,suspend=n"
    else
        echo "Debug already enabled in JAVA_OPTS, ignoring --debug argument"
    fi
fi

CLASSPATH_OPTS="$DIRNAME/../lib/quarkus-run.jar"

exec java $JAVA_OPTS $SERVER_OPTS -cp $CLASSPATH_OPTS io.quarkus.bootstrap.runner.QuarkusEntryPoint ${CONFIG_ARGS#?}