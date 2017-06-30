#############################################################################
#                                                                          ##
#    WildFly CLI Script for interacting with the server                    ##
#                                                                          ##
#############################################################################
$PROGNAME=$MyInvocation.MyCommand.Name
$scripts = (Get-ChildItem $MyInvocation.MyCommand.Path).Directory.FullName;
. $scripts'\common.ps1'

$SERVER_OPTS = Process-Script-Parameters -Params $ARGS

$PROG_ARGS = Get-Java-Arguments -entryModule "org.jboss.as.cli" -serverOpts $SERVER_OPTS -logFileProperties "$JBOSS_HOME/bin/jboss-cli-logging.properties"

& $JAVA $PROG_ARGS

Env-Clean-Up