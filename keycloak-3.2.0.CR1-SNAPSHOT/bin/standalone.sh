#!/bin/sh

# Use --debug to activate debug mode with an optional argument to specify the port.
# Usage : standalone.sh --debug
#         standalone.sh --debug 9797

# By default debug mode is disabled.
DEBUG_MODE="${DEBUG:-false}"
DEBUG_PORT="${DEBUG_PORT:-8787}"
GC_LOG="$GC_LOG"
SERVER_OPTS=""
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
      -Djava.security.manager*)
          echo "ERROR: The use of -Djava.security.manager has been removed. Please use the -secmgr command line argument or SECMGR=true environment variable."
          exit 1
          ;;
      -secmgr)
          SECMGR="true"
          ;;
      --)
          shift
          break;;
      *)
          SERVER_OPTS="$SERVER_OPTS '$1'"
          ;;
    esac
    shift
done

DIRNAME=`dirname "$0"`
PROGNAME=`basename "$0"`
GREP="grep"

# Use the maximum available, or set MAX_FD != -1 to use that
MAX_FD="maximum"

# OS specific support (must be 'true' or 'false').
cygwin=false;
darwin=false;
linux=false;
solaris=false;
freebsd=false;
other=false
case "`uname`" in
    CYGWIN*)
        cygwin=true
        ;;

    Darwin*)
        darwin=true
        ;;
    FreeBSD)
        freebsd=true
        ;;
    Linux)
        linux=true
        ;;
    SunOS*)
        solaris=true
        ;;
    *)
        other=true
        ;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
    [ -n "$JBOSS_HOME" ] &&
        JBOSS_HOME=`cygpath --unix "$JBOSS_HOME"`
    [ -n "$JAVA_HOME" ] &&
        JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
    [ -n "$JAVAC_JAR" ] &&
        JAVAC_JAR=`cygpath --unix "$JAVAC_JAR"`
fi

# Setup JBOSS_HOME
RESOLVED_JBOSS_HOME=`cd "$DIRNAME/.." >/dev/null; pwd`
if [ "x$JBOSS_HOME" = "x" ]; then
    # get the full path (without any relative bits)
    JBOSS_HOME=$RESOLVED_JBOSS_HOME
else
 SANITIZED_JBOSS_HOME=`cd "$JBOSS_HOME"; pwd`
 if [ "$RESOLVED_JBOSS_HOME" != "$SANITIZED_JBOSS_HOME" ]; then
   echo ""
   echo "   WARNING:  JBOSS_HOME may be pointing to a different installation - unpredictable results may occur."
   echo ""
   echo "             JBOSS_HOME: $JBOSS_HOME"
   echo ""
   sleep 2s
 fi
fi
export JBOSS_HOME

# Read an optional running configuration file
if [ "x$RUN_CONF" = "x" ]; then
    RUN_CONF="$DIRNAME/standalone.conf"
fi
if [ -r "$RUN_CONF" ]; then
    . "$RUN_CONF"
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

# Setup the JVM
if [ "x$JAVA" = "x" ]; then
    if [ "x$JAVA_HOME" != "x" ]; then
        JAVA="$JAVA_HOME/bin/java"
    else
        JAVA="java"
    fi
fi

if $linux; then
    # consolidate the server and command line opts
    CONSOLIDATED_OPTS="$JAVA_OPTS $SERVER_OPTS"
    # process the standalone options
    for var in $CONSOLIDATED_OPTS
    do
       # Remove quotes
       p=`echo $var | tr -d "'"`
       case $p in
         -Djboss.server.base.dir=*)
              JBOSS_BASE_DIR=`readlink -m ${p#*=}`
              ;;
         -Djboss.server.log.dir=*)
              JBOSS_LOG_DIR=`readlink -m ${p#*=}`
              ;;
         -Djboss.server.config.dir=*)
              JBOSS_CONFIG_DIR=`readlink -m ${p#*=}`
              ;;
       esac
    done
fi

if $solaris; then
    # consolidate the server and command line opts
    CONSOLIDATED_OPTS="$JAVA_OPTS $SERVER_OPTS"
    # process the standalone options
    for var in $CONSOLIDATED_OPTS
    do
       # Remove quotes
       p=`echo $var | tr -d "'"`
      case $p in
        -Djboss.server.base.dir=*)
             JBOSS_BASE_DIR=`echo $p | awk -F= '{print $2}'`
             ;;
        -Djboss.server.log.dir=*)
             JBOSS_LOG_DIR=`echo $p | awk -F= '{print $2}'`
             ;;
        -Djboss.server.config.dir=*)
             JBOSS_CONFIG_DIR=`echo $p | awk -F= '{print $2}'`
             ;;
      esac
    done
fi

# No readlink -m on BSD
if $darwin || $freebsd || $other ; then
    # consolidate the server and command line opts
    CONSOLIDATED_OPTS="$JAVA_OPTS $SERVER_OPTS"
    # process the standalone options
    for var in $CONSOLIDATED_OPTS
    do
       # Remove quotes
       p=`echo $var | tr -d "'"`
       case $p in
         -Djboss.server.base.dir=*)
              JBOSS_BASE_DIR=`cd ${p#*=} ; pwd -P`
              ;;
         -Djboss.server.log.dir=*)
              if [ -d "${p#*=}" ]; then
                JBOSS_LOG_DIR=`cd ${p#*=} ; pwd -P`
             else
                #since the specified directory doesn't exist we don't validate it
                JBOSS_LOG_DIR=${p#*=}
             fi
             ;;
         -Djboss.server.config.dir=*)
              JBOSS_CONFIG_DIR=`cd ${p#*=} ; pwd -P`
              ;;
       esac
    done
fi

# determine the default base dir, if not set
if [ "x$JBOSS_BASE_DIR" = "x" ]; then
   JBOSS_BASE_DIR="$JBOSS_HOME/standalone"
fi
# determine the default log dir, if not set
if [ "x$JBOSS_LOG_DIR" = "x" ]; then
   JBOSS_LOG_DIR="$JBOSS_BASE_DIR/log"
fi
# determine the default configuration dir, if not set
if [ "x$JBOSS_CONFIG_DIR" = "x" ]; then
   JBOSS_CONFIG_DIR="$JBOSS_BASE_DIR/configuration"
fi

if [ "x$JBOSS_MODULEPATH" = "x" ]; then
    JBOSS_MODULEPATH="$JBOSS_HOME/modules"
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
    JBOSS_HOME=`cygpath --path --windows "$JBOSS_HOME"`
    JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
    JBOSS_MODULEPATH=`cygpath --path --windows "$JBOSS_MODULEPATH"`
    JBOSS_BASE_DIR=`cygpath --path --windows "$JBOSS_BASE_DIR"`
    JBOSS_LOG_DIR=`cygpath --path --windows "$JBOSS_LOG_DIR"`
    JBOSS_CONFIG_DIR=`cygpath --path --windows "$JBOSS_CONFIG_DIR"`
fi



if [ "$PRESERVE_JAVA_OPTS" != "true" ]; then
    # Check for -d32/-d64 in JAVA_OPTS
    JVM_D64_OPTION=`echo $JAVA_OPTS | $GREP "\-d64"`
    JVM_D32_OPTION=`echo $JAVA_OPTS | $GREP "\-d32"`

    # Check If server or client is specified
    SERVER_SET=`echo $JAVA_OPTS | $GREP "\-server"`
    CLIENT_SET=`echo $JAVA_OPTS | $GREP "\-client"`

    if [ "x$JVM_D32_OPTION" != "x" ]; then
        JVM_OPTVERSION="-d32"
    elif [ "x$JVM_D64_OPTION" != "x" ]; then
        JVM_OPTVERSION="-d64"
    elif $darwin && [ "x$SERVER_SET" = "x" ]; then
        # Use 32-bit on Mac, unless server has been specified or the user opts are incompatible
        "$JAVA" -d32 $JAVA_OPTS -version > /dev/null 2>&1 && PREPEND_JAVA_OPTS="-d32" && JVM_OPTVERSION="-d32"
    fi

    if [ "x$CLIENT_SET" = "x" -a "x$SERVER_SET" = "x" ]; then
        # neither -client nor -server is specified
        if $darwin && [ "$JVM_OPTVERSION" = "-d32" ]; then
            # Prefer client for Macs, since they are primarily used for development
            PREPEND_JAVA_OPTS="$PREPEND_JAVA_OPTS -client"
        else
            PREPEND_JAVA_OPTS="$PREPEND_JAVA_OPTS -server"
        fi
    fi

    if [ "$GC_LOG" = "true" ]; then
        # Enable rotating GC logs if the JVM supports it and GC logs are not already enabled
        NO_GC_LOG_ROTATE=`echo $JAVA_OPTS | $GREP "\-verbose:gc"`
        if [ "x$NO_GC_LOG_ROTATE" = "x" ]; then
            # backup prior gc logs
            mv -f "$JBOSS_LOG_DIR/gc.log.0" "$JBOSS_LOG_DIR/backupgc.log.0" >/dev/null 2>&1
            mv -f "$JBOSS_LOG_DIR/gc.log.1" "$JBOSS_LOG_DIR/backupgc.log.1" >/dev/null 2>&1
            mv -f "$JBOSS_LOG_DIR/gc.log.2" "$JBOSS_LOG_DIR/backupgc.log.2" >/dev/null 2>&1
            mv -f "$JBOSS_LOG_DIR/gc.log.3" "$JBOSS_LOG_DIR/backupgc.log.3" >/dev/null 2>&1
            mv -f "$JBOSS_LOG_DIR/gc.log.4" "$JBOSS_LOG_DIR/backupgc.log.4" >/dev/null 2>&1
            mv -f "$JBOSS_LOG_DIR"/gc.log.*.current "$JBOSS_LOG_DIR/backupgc.log.current" >/dev/null 2>&1
            "$JAVA" $JVM_OPTVERSION -verbose:gc -Xloggc:"$JBOSS_LOG_DIR/gc.log" -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=3M -XX:-TraceClassUnloading -version >/dev/null 2>&1 && mkdir -p $JBOSS_LOG_DIR && PREPEND_JAVA_OPTS="$PREPEND_JAVA_OPTS -verbose:gc -Xloggc:\"$JBOSS_LOG_DIR/gc.log\" -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=3M -XX:-TraceClassUnloading"
        fi
    fi

    JAVA_OPTS="$PREPEND_JAVA_OPTS $JAVA_OPTS"
fi

# Process the JAVA_OPTS and fail the script of a java.security.manager was found
SECURITY_MANAGER_SET=`echo $JAVA_OPTS | $GREP "java\.security\.manager"`
if [ "x$SECURITY_MANAGER_SET" != "x" ]; then
    echo "ERROR: The use of -Djava.security.manager has been removed. Please use the -secmgr command line argument or SECMGR=true environment variable."
    exit 1
fi

# Set up the module arguments
MODULE_OPTS=""
if [ "$SECMGR" = "true" ]; then
    MODULE_OPTS="$MODULE_OPTS -secmgr";
fi

# Display our environment
echo "========================================================================="
echo ""
echo "  JBoss Bootstrap Environment"
echo ""
echo "  JBOSS_HOME: $JBOSS_HOME"
echo ""
echo "  JAVA: $JAVA"
echo ""
echo "  JAVA_OPTS: $JAVA_OPTS"
echo ""
echo "========================================================================="
echo ""

while true; do
   if [ "x$LAUNCH_JBOSS_IN_BACKGROUND" = "x" ]; then
      # Execute the JVM in the foreground
      eval \"$JAVA\" -D\"[Standalone]\" $JAVA_OPTS \
         \"-Dorg.jboss.boot.log.file="$JBOSS_LOG_DIR"/server.log\" \
         \"-Dlogging.configuration=file:"$JBOSS_CONFIG_DIR"/logging.properties\" \
         -jar \""$JBOSS_HOME"/jboss-modules.jar\" \
         $MODULE_OPTS \
         -mp \""${JBOSS_MODULEPATH}"\" \
         org.jboss.as.standalone \
         -Djboss.home.dir=\""$JBOSS_HOME"\" \
         -Djboss.server.base.dir=\""$JBOSS_BASE_DIR"\" \
         "$SERVER_OPTS"
      JBOSS_STATUS=$?
   else
      # Execute the JVM in the background
      eval \"$JAVA\" -D\"[Standalone]\" $JAVA_OPTS \
         \"-Dorg.jboss.boot.log.file="$JBOSS_LOG_DIR"/server.log\" \
         \"-Dlogging.configuration=file:"$JBOSS_CONFIG_DIR"/logging.properties\" \
         -jar \""$JBOSS_HOME"/jboss-modules.jar\" \
         $MODULE_OPTS \
         -mp \""${JBOSS_MODULEPATH}"\" \
         org.jboss.as.standalone \
         -Djboss.home.dir=\""$JBOSS_HOME"\" \
         -Djboss.server.base.dir=\""$JBOSS_BASE_DIR"\" \
         "$SERVER_OPTS" "&"
      JBOSS_PID=$!
      # Trap common signals and relay them to the jboss process
      trap "kill -HUP  $JBOSS_PID" HUP
      trap "kill -TERM $JBOSS_PID" INT
      trap "kill -QUIT $JBOSS_PID" QUIT
      trap "kill -PIPE $JBOSS_PID" PIPE
      trap "kill -TERM $JBOSS_PID" TERM
      if [ "x$JBOSS_PIDFILE" != "x" ]; then
        echo $JBOSS_PID > $JBOSS_PIDFILE
      fi
      # Wait until the background process exits
      WAIT_STATUS=128
      while [ "$WAIT_STATUS" -ge 128 ]; do
         wait $JBOSS_PID 2>/dev/null
         WAIT_STATUS=$?
         if [ "$WAIT_STATUS" -gt 128 ]; then
            SIGNAL=`expr $WAIT_STATUS - 128`
            SIGNAL_NAME=`kill -l $SIGNAL`
            echo "*** JBossAS process ($JBOSS_PID) received $SIGNAL_NAME signal ***" >&2
         fi
      done
      if [ "$WAIT_STATUS" -lt 127 ]; then
         JBOSS_STATUS=$WAIT_STATUS
      else
         JBOSS_STATUS=0
      fi
      if [ "$JBOSS_STATUS" -ne 10 ]; then
            # Wait for a complete shudown
            wait $JBOSS_PID 2>/dev/null
      fi
      if [ "x$JBOSS_PIDFILE" != "x" ]; then
            grep "$JBOSS_PID" $JBOSS_PIDFILE && rm $JBOSS_PIDFILE
      fi
   fi
   if [ "$JBOSS_STATUS" -eq 10 ]; then
      echo "Restarting application server..."
   else
      exit $JBOSS_STATUS
   fi
done
