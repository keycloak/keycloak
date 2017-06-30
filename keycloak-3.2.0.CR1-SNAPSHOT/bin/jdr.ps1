#############################################################################
#                                                                          ##
#    JBoss Diagnostic Report (JDR) Script for Windows                      ##
#                                                                          ##
#############################################################################
$scripts = (Get-ChildItem $MyInvocation.MyCommand.Path).Directory.FullName;
. $scripts'\common.ps1'

$JAVA_OPTS = @()

$PROG_ARGS = Get-Java-Arguments -entryModule "org.jboss.as.jdr" -serverOpts $ARGS -logFileProperties $null

try{
	pushd $JBOSS_HOME
	& $JAVA $PROG_ARGS
}finally{
	popd
	Env-Clean-Up
}
