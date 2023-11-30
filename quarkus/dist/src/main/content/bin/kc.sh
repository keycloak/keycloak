#!/bin/sh

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
    OpenBSD)
        RESOLVED_NAME="$(readlink -f "$0")"
        JAVA_HOME="$(/usr/local/bin/javaPathHelper -h keycloak)"
        ;;
    Linux)
        RESOLVED_NAME="$(readlink -f "$0")"
        ;;
esac

RESOLVED_NAME="${RESOLVED_NAME:-"$0"}"

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
CLASSPATH_OPTS="'$(abs_path "../lib/quarkus-run.jar")'"

DEBUG_MODE="${DEBUG:-false}"
DEBUG_PORT="${DEBUG_PORT:-8787}"
DEBUG_SUSPEND="${DEBUG_SUSPEND:-n}"

esceval() {
    printf '%s\n' "$1" | sed "s/'/'\\\\''/g; 1 s/^/'/; $ s/$/'/"
}

PRE_BUILD=true
while [ "$#" -gt 0 ]
do
    case "$1" in
      --debug)
          DEBUG_MODE=true
          if [ -n "$2" ] && expr "$2" : '[0-9]\+$' >/dev/null; then
              DEBUG_PORT=$2
              shift
          fi
          ;;
      --)
          shift
          break
          ;;
      *)
          OPT=$(esceval "$1")
          case "$1" in
            start-dev) CONFIG_ARGS="$CONFIG_ARGS --profile=dev $1";;
            -D*) SERVER_OPTS="$SERVER_OPTS ${OPT}";;
            *) case "$1" in
                 --optimized | --help | --help-all | -h) PRE_BUILD=false;;
                 build) if [ -z "$CONFIG_ARGS" ]; then PRE_BUILD=false; fi;;
               esac 
               CONFIG_ARGS="$CONFIG_ARGS ${OPT}"
               ;;
          esac
          ;;
    esac
    shift
done

if [ -z "$JAVA" ]; then
    if [ -n "$JAVA_HOME" ]; then
        JAVA="$JAVA_HOME/bin/java"
    else
        JAVA="java"
    fi
fi

#
# Specify options to pass to the Java VM.
#
if [ -z "$JAVA_OPTS" ]; then
   # The defaults set up Keycloak with '-XX:+UseParallelGC -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20 -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90' which proved to provide a good throughput and efficiency in the total memory allocation and CPU overhead.
   # If the memory is not used, it will be freed. See https://developers.redhat.com/blog/2017/04/04/openjdk-and-containers for details.
   # To optimize for large heap sizes or for throughput and better response time due to shorter GC pauses, consider ZGC and Shenandoah GC.
   # Both ZGC and Shenandoah GC seem to be more eager to claim the maximum heap size. Tests showed that ZGC might need additional tuning as as it is not as aggressive as ParallelGC in reclaiming dead objects.
   JAVA_OPTS="-Xms64m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.err.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/urandom -XX:+UseParallelGC -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20 -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -XX:FlightRecorderOptions=stackdepth=512"
else
   echo "JAVA_OPTS already set in environment; overriding default settings with values: $JAVA_OPTS"
fi

# See also https://github.com/wildfly/wildfly-core/blob/7e5624cf92ebe4b64a4793a8c0b2a340c0d6d363/core-feature-pack/common/src/main/resources/content/bin/common.sh#L57-L60
if [ -z "$JAVA_ADD_OPENS" ]; then
   JAVA_ADD_OPENS="--add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.security=ALL-UNNAMED"
else
   echo "JAVA_ADD_OPENS already set in environment; overriding default settings with values: $JAVA_ADD_OPENS"
fi
JAVA_OPTS="$JAVA_OPTS $JAVA_ADD_OPENS"

if [ -n "$JAVA_OPTS_APPEND" ]; then
  echo "Appending additional Java properties to JAVA_OPTS: $JAVA_OPTS_APPEND"
  JAVA_OPTS="$JAVA_OPTS $JAVA_OPTS_APPEND"
fi

# Set debug settings if not already set
if [ "$DEBUG_MODE" = "true" ]; then
    DEBUG_OPT="$(echo "$JAVA_OPTS" | $GREP "\-agentlib:jdwp")"
    if [ -z "$DEBUG_OPT" ]; then
        JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,address=$DEBUG_PORT,server=y,suspend=$DEBUG_SUSPEND"
    else
        echo "Debug already enabled in JAVA_OPTS, ignoring --debug argument"
    fi
fi

esceval_args() {
  while IFS= read -r entry; do
    result="$result $(esceval "$entry")"
  done
  echo $result
}

JAVA_RUN_OPTS=$(echo "$JAVA_OPTS" | xargs printf '%s\n' | esceval_args)

JAVA_RUN_OPTS="$JAVA_RUN_OPTS $SERVER_OPTS -cp $CLASSPATH_OPTS io.quarkus.bootstrap.runner.QuarkusEntryPoint ${CONFIG_ARGS#?}"

if [ "$PRINT_ENV" = "true" ]; then
  echo "Using JAVA_OPTS: $JAVA_OPTS"
  echo "Using JAVA_RUN_OPTS: $JAVA_RUN_OPTS"
fi

if [ "$PRE_BUILD" = "true" ]; then
  eval "'$JAVA'" -Dkc.config.build-and-exit=true $JAVA_RUN_OPTS || exit $?
  JAVA_RUN_OPTS="-Dkc.config.built=true $JAVA_RUN_OPTS"
fi

eval exec "'$JAVA'" $JAVA_RUN_OPTS
