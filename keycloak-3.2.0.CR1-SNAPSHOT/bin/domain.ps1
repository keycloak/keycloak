#############################################################################
#                                                                          ##
#    WildFly Startup Script for starting the domain server                 ##
#                                                                          ##
#############################################################################

$scripts = (Get-ChildItem $MyInvocation.MyCommand.Path).Directory.FullName;
. $scripts'\common.ps1'

$SERVER_OPTS = Process-Script-Parameters -Params $ARGS

# Read an optional running configuration file
$DOMAIN_CONF = $scripts +'\domain.conf.ps1'
$DOMAIN_CONF = Get-Env RUN_CONF $DOMAIN_CONF
. $DOMAIN_CONF

Set-Global-Variables-Domain

# consolidate the host-controller and command line opts
$HOST_CONTROLLER_OPTS=$HOST_CONTROLLER_JAVA_OPTS+$SERVER_OPTS
# process the host-controller options
foreach($p in $HOST_CONTROLLER_OPTS){
	if ($p -eq $null){# odd but could happen
		continue
	}
	$arg = $p.Trim()
	if ($arg.StartsWith('-Djboss.domain.base.dir')){
		$JBOSS_BASE_DIR=$p.Substring('-Djboss.domain.base.dir='.Length)
	}elseif ($arg.StartsWith('-Djboss.domain.log.dir')){
		$JBOSS_LOG_DIR=$p.Substring('-Djboss.domain.log.dir='.Length)
	}elseif ($arg.StartsWith('-Djboss.domain.config.dir')){
		$JBOSS_CONFIG_DIR=$p.Substring('-Djboss.domain.config.dir='.Length)
	}
}

# If the -Djava.security.manager is found, enable the -secmgr and include a bogus security manager for JBoss Modules to replace
# Note that HOST_CONTROLLER_JAVA_OPTS will not need to be handled here

if ( $PROCESS_CONTROLLER_JAVA_OPTS -contains 'java.security.manager') {
    echo "ERROR: The use of -Djava.security.manager has been removed. Please use the -secmgr command line argument or SECMGR=true environment variable."
    exit
}

Display-Environment

$MODULE_OPTS += "-jvm"
$MODULE_OPTS += "$JAVA"
$MODULE_OPTS += "-jboss-home"
$MODULE_OPTS += "$JBOSS_HOME"
         

$PROG_ARGS = @()
$PROG_ARGS +='-DProcessController' 
$PROG_ARGS += $PROCESS_CONTROLLER_JAVA_OPTS
$PROG_ARGS += "-Dorg.jboss.boot.log.file=$JBOSS_LOG_DIR\process-controller.log"
$PROG_ARGS += "-Dlogging.configuration=file:$JBOSS_CONFIG_DIR\logging.properties"
$PROG_ARGS += "-Djboss.home.dir=$JBOSS_HOME"
$PROG_ARGS += "-jar"
$PROG_ARGS += "$JBOSS_HOME\jboss-modules.jar"
$PROG_ARGS += "-mp"
$PROG_ARGS += $JBOSS_MODULEPATH
$PROG_ARGS += "org.jboss.as.process-controller"
if ($MODULE_OPTS -ne $null){
	$PROG_ARGS += $MODULE_OPTS
}
$PROG_ARGS += "--"  
$PROG_ARGS += "-Dorg.jboss.boot.log.file=$JBOSS_LOG_DIR\host-controller.log"
$PROG_ARGS += "-Dlogging.configuration=file:$JBOSS_CONFIG_DIR\logging.properties"
$PROG_ARGS += $HOST_CONTROLLER_JAVA_OPTS
$PROG_ARGS += "--"  
$PROG_ARGS += "-default-jvm"
$PROG_ARGS += $JAVA
if ($SERVER_OPTS -ne $null){
	$PROG_ARGS += $SERVER_OPTS
}

 
$backgroundProcess = Get-Env LAUNCH_JBOSS_IN_BACKGROUND 'false'
$runInBackGround = $global:RUN_IN_BACKGROUND -or ($backgroundProcess -eq 'true')

Start-WildFly-Process -programArguments $PROG_ARGS -runInBackground $runInBackGround