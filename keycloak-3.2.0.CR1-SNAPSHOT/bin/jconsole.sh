#!/bin/sh

DIRNAME=`dirname "$0"`
GREP="grep"

# Use the maximum available, or set MAX_FD != -1 to use that
MAX_FD="maximum"

# OS specific support (must be 'true' or 'false').
cygwin=false;
darwin=false;
linux=false;
case "`uname`" in
    CYGWIN*)
        cygwin=true
        ;;

    Darwin*)
        darwin=true
        ;;

    Linux)
        linux=true
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
RESOLVED_JBOSS_HOME=`cd "$DIRNAME/.."; pwd`
if [ "x$JBOSS_HOME" = "x" ]; then
    # get the full path (without any relative bits)
    JBOSS_HOME=$RESOLVED_JBOSS_HOME
else
 SANITIZED_JBOSS_HOME=`cd "$JBOSS_HOME"; pwd`
 if [ "$RESOLVED_JBOSS_HOME" != "$SANITIZED_JBOSS_HOME" ]; then
   echo "WARNING JBOSS_HOME may be pointing to a different installation - unpredictable results may occur."
   echo ""
 fi
fi
export JBOSS_HOME

# Setup the JVM
if [ "x$JAVA_HOME" = x ]; then
   fail_java_home () {
        echo "JAVA_HOME is not set. Unable to locate the jars needed to run jconsole."
        exit 2
   }

   JCONSOLE_PATH=`which jconsole` || fail_java_home
   which readlink || fail_java_home # make sure readlink is present
   JCONSOLE_TEST=`readlink "$JCONSOLE_PATH"`
   while [ x"$JCONSOLE_TEST" != x ]; do
      JCONSOLE_PATH="$JCONSOLE_TEST"
      JCONSOLE_TEST=`readlink "$JCONSOLE_PATH"`
   done
   JAVA_HOME=`dirname "$JCONSOLE_PATH"`
   JAVA_HOME=`dirname "$JAVA_HOME"`
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
    JBOSS_HOME=`cygpath --path --windows "$JBOSS_HOME"`
    JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
fi

CLASSPATH=$JAVA_HOME/lib/jconsole.jar
CLASSPATH=$CLASSPATH:$JAVA_HOME/lib/tools.jar
CLASSPATH=$CLASSPATH:./bin/client/jboss-cli-client.jar

echo CLASSPATH $CLASSPATH

cd "$JBOSS_HOME"
$JAVA_HOME/bin/jconsole -J-Djava.class.path="$CLASSPATH" "$@"
