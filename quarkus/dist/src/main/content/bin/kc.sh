#!/bin/bash

case "$(uname)" in
    CYGWIN*)
        IS_CYGWIN="true"
        CFILE="$(cygpath "$0")"
        RESOLVED_NAME="$(readlink -f "$CFILE")"
        ;;
    Darwin*)
        RESOLVED_NAME="$(readlink "$0")"
        ;;
    FreeBSD)
        RESOLVED_NAME="$(readlink -f "$0")"
        ;;
    Linux)
        RESOLVED_NAME="$(readlink -f "$0")"
        ;;
esac

if [ "x$RESOLVED_NAME" = "x" ]; then
    RESOLVED_NAME="$0"
fi

GREP="grep"
DIRNAME="$(dirname "$RESOLVED_NAME")"

abs_path () {
  if [ -z $IS_CYGWIN ] ; then
    echo "$DIRNAME/$1"
  else
    cygpath -w "$DIRNAME/$1"
  fi
}

SERVER_OPTS="-Dkc.home.dir='$(abs_path '..')'"
SERVER_OPTS="$SERVER_OPTS -Djboss.server.config.dir='$(abs_path '../conf')'"
SERVER_OPTS="$SERVER_OPTS -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
SERVER_OPTS="$SERVER_OPTS -Dquarkus-log-max-startup-records=10000"
CLASSPATH_OPTS="'$(abs_path "../lib/quarkus-run.jar"):$(abs_path "../lib/bootstrap/*")'"

DEBUG_MODE="${DEBUG:-false}"
DEBUG_PORT="${DEBUG_PORT:-8787}"
DEBUG_SUSPEND="${DEBUG_SUSPEND:-n}"

CONFIG_ARGS=${CONFIG_ARGS:-""}

while [ "$#" -gt 0 ]
do
    case "$1" in
      --debug)
          DEBUG_MODE=true
          if [ -n "$2" ] && [[ "$2" =~ ^[0-9]+$ ]]; then
              DEBUG_PORT=$2
              shift
          fi
          ;;
      --)
          shift
          break
          ;;
      *)
          if [[ $1 = --* || ! $1 =~ ^-D.* ]]; then
            if [[ "$1" = "start-dev" ]]; then
              CONFIG_ARGS="$CONFIG_ARGS --profile=dev $1"
            else
              CONFIG_ARGS="$CONFIG_ARGS $1"
            fi
          else
            SERVER_OPTS="$SERVER_OPTS $1"
          fi
          ;;
    esac
    shift
done

if [ "x$JAVA" = "x" ]; then
    if [ "x$JAVA_HOME" != "x" ]; then
        JAVA="$JAVA_HOME/bin/java"
    else
        JAVA="java"
    fi
fi

#
# Specify options to pass to the Java VM.
#
if [ "x$JAVA_OPTS" = "x" ]; then
   JAVA_OPTS="-Xms64m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8"
else
   echo "JAVA_OPTS already set in environment; overriding default settings with values: $JAVA_OPTS"
fi

if [ "x$JAVA_OPTS_APPEND" != "x" ]; then
  echo "Appending additional Java properties to JAVA_OPTS: $JAVA_OPTS_APPEND"
  JAVA_OPTS="$JAVA_OPTS $JAVA_OPTS_APPEND"
fi

# Set debug settings if not already set
if [ "$DEBUG_MODE" = "true" ]; then
    DEBUG_OPT="$(echo "$JAVA_OPTS" | $GREP "\-agentlib:jdwp")"
    if [ "x$DEBUG_OPT" = "x" ]; then
        JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,address=$DEBUG_PORT,server=y,suspend=$DEBUG_SUSPEND"
    else
        echo "Debug already enabled in JAVA_OPTS, ignoring --debug argument"
    fi
fi

JAVA_RUN_OPTS="$JAVA_OPTS $SERVER_OPTS -cp $CLASSPATH_OPTS io.quarkus.bootstrap.runner.QuarkusEntryPoint ${CONFIG_ARGS#?}"

if [[ (! $CONFIG_ARGS = *"--optimized"*) ]] && [[ ! "$CONFIG_ARGS" == " build"* ]] && [[ ! "$CONFIG_ARGS" == *"-h" ]] && [[ ! "$CONFIG_ARGS" == *"--help"* ]]; then
    eval "'$JAVA'" -Dkc.config.build-and-exit=true $JAVA_RUN_OPTS
    EXIT_CODE=$?
    JAVA_RUN_OPTS="-Dkc.config.built=true $JAVA_RUN_OPTS"
    if [ $EXIT_CODE != 0 ]; then
      exit $EXIT_CODE
    fi
fi

eval exec "'$JAVA'" $JAVA_RUN_OPTS
