### -*- Power Shell file -*- ################################################
#                                                                          ##
#  WildFly bootstrap Script Configuration                                    ##
#                                                                          ##
#############################################################################

#
# This script file is executed by standalone.ps1 to initialize the environment
# variables that standalone.ps1 uses. It is recommended to use this file to
# configure these variables, rather than modifying standalone.ps1 itself.
#
#
# Specify the location of the Java home directory (it is recommended that
# this always be set). If set, then "%JAVA_HOME%\bin\java" will be used as
# the Java VM executable; otherwise, "%JAVA%" will be used (see below).
#
# $JAVA_HOME="C:\opt\jdk1.8.0"

#
# Specify the exact Java VM executable to use - only used if JAVA_HOME is
# not set. Default is "java".
#
# $JAVA="C:\opt\jdk1.8.0\bin\java"

#
# Specify options to pass to the Java VM. Note, there are some additional
# options that are always passed by run.bat.
#


# Uncomment the following line to disable manipulation of JAVA_OPTS (JVM parameters)
# $PRESERVE_JAVA_OPTS=true

if (-Not(test-path env:JBOSS_MODULES_SYSTEM_PKGS )) {
  $JBOSS_MODULES_SYSTEM_PKGS="org.jboss.byteman"
}


$JAVA_OPTS = @()

# JVM memory allocation pool parameters - modify as appropriate.
$JAVA_OPTS += '-Xms64M'
$JAVA_OPTS += '-Xmx512M'
$JAVA_OPTS += '-XX:MetaspaceSize=96M'
$JAVA_OPTS += '-XX:MaxMetaspaceSize=256m'

# Reduce the RMI GCs to once per hour for Sun JVMs.
#$JAVA_OPTS += '-Dsun.rmi.dgc.client.gcInterval=3600000'
#$JAVA_OPTS += '-Dsun.rmi.dgc.server.gcInterval=3600000'
# prefer ipv4 stack
$JAVA_OPTS += '-Djava.net.preferIPv4Stack=true'

# Warn when resolving remote XML DTDs or schemas.
# $JAVA_OPTS += '-Dorg.jboss.resolver.warning=true'

# Make Byteman classes visible in all module loaders
# This is necessary to inject Byteman rules into AS7 deployments
$JAVA_OPTS += "-Djboss.modules.system.pkgs=$JBOSS_MODULES_SYSTEM_PKGS"

# Set the default configuration file to use if -c or --server-config are not used
#$JAVA_OPTS += '-Djboss.server.default.config=standalone.xml'

# Sample JPDA settings for remote socket debugging
# $JAVA_OPTS += '-Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n'

# Sample JPDA settings for shared memory debugging
# $JAVA_OPTS += '-Xrunjdwp:transport=dt_shmem,address=jboss,server=y,suspend=n'

# Use JBoss Modules lockless mode
# $JAVA_OPTS += '-Djboss.modules.lockless=true'

# Uncomment this to run with a security manager enabled
# $SECMGR=$true

# Uncomment this out to control garbage collection logging
# $GC_LOG=$true
