#############################################################################
#                                                                          ##
#    WildFly Startup Script for adding users							   ##
#                                                                          ##
#############################################################################
$scripts = (Get-ChildItem $MyInvocation.MyCommand.Path).Directory.FullName;
. $scripts'\common.ps1'

$JAVA_OPTS = @()

# Sample JPDA settings for remote socket debugging
#$JAVA_OPTS+="-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=y"
# Uncomment to override standalone and domain user location
#$JAVA_OPTS+="-Djboss.server.config.user.dir=$JBOSS_HOME/standalone/configuration"
#$JAVA_OPTS+="-Djboss.domain.config.user.dir=$JBOSS_HOME/domain/configuration"


$PROG_ARGS = Get-Java-Arguments -entryModule "org.jboss.as.domain-add-user" -serverOpts $ARGS -logFileProperties $SCRIPTS_HOME\add-user.properties

try{
	pushd $JBOSS_HOME
	& $JAVA $PROG_ARGS
}finally{
	popd
	Env-Clean-Up
}
