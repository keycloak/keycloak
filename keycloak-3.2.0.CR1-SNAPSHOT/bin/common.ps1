if($PSVersionTable.PSVersion.Major -lt 2) {
    Write-Warning "This script requires PowerShell 2.0 or better; you have version $($Host.Version)."
    return
}

$SCRIPTS_HOME = (Get-ChildItem $MyInvocation.MyCommand.Path).Directory.FullName
$RESOLVED_JBOSS_HOME = (Get-ChildItem $MyInvocation.MyCommand.Path).Directory.Parent.FullName


# A collection of functions that are used by the other scripts

Function Set-Env {
  $key = $args[0]
  $value = $args[1]
  Set-Content -Path env:$key -Value $value
}

Function Get-Env {
  $key = $args[0]
  if( Test-Path env:$key ) {
    return (Get-ChildItem env:$key).Value
  }
  return $args[1]
}
Function Get-Env-Boolean{
  $key = $args[0]
  if( Test-Path env:$key ) {
    return (Get-ChildItem env:$key).Value -eq 'true'
  }
  return $args[1]
}

$global:SECMGR = Get-Env-Boolean SECMGR $false
$global:DEBUG_MODE=Get-Env DEBUG $false
$global:DEBUG_PORT=Get-Env DEBUG_PORT 8787
$global:RUN_IN_BACKGROUND=$false
$GC_LOG=Get-Env GC_LOG
#module opts that are passed to jboss modules
$global:MODULE_OPTS = @()

Function Get-String {
  $value = ''
  foreach($k in $args) {
    $value += $k
  }
  return $value
}

Function String-To-Array($value) {
  $res = @()
  if (!$value){
  	return $res
  }
  $tmpArr = $value.split()

  foreach ($str in $tmpArr) {
    if ($str) {
	  $res += $str
	}
  }
  return $res
}

Function Display-Environment {
Param(
   [string[]]$javaOpts
) #end param

if (-Not $javaOpts){
	$javaOpts = Get-Java-Opts
}

# Display our environment
Write-Host "================================================================================="
Write-Host ""
Write-Host "  JBoss Bootstrap Environment"
Write-Host ""
Write-Host "  JBOSS_HOME: $JBOSS_HOME"
Write-Host ""
Write-Host "  JAVA: $JAVA"
Write-Host ""
Write-Host "  MODULE_OPTS: $MODULE_OPTS"
Write-Host ""
Write-Host "  JAVA_OPTS: $javaOpts"
Write-Host ""
Write-Host "================================================================================="
Write-Host ""

}

#todo: bit funky at the moment, should probably be done via global variable
Function Get-Java-Opts {
	if($PRESERVE_JAVA_OPTS -ne 'true') { # if not perserve, then check for enviroment variable and use that
		if( (Test-Path env:JAVA_OPTS)) {
			$ops = Get-Env JAVA_OPTS
			# This is Powershell, so split the incoming string on a space into array
			return String-To-Array -value $ops
			Write-Host "JAVA_OPTS already set in environment; overriding default settings with values: $JAVA_OPTS"
		}
	}
	return $JAVA_OPTS
}

Function Display-Array($array){
	for ($i=0; $i -lt $array.length; $i++) {
		$v =  "$i " + $array[$i]
		Write-Host $v
	}
}


Function Get-Java-Arguments {
Param(
   [Parameter(Mandatory=$true)]
   [string]$entryModule,
   [string]$logFileProperties = "$JBOSS_CONFIG_DIR/logging.properties",
   [string]$logFile = "$JBOSS_LOG_DIR/server.log",
   [string[]]$serverOpts


) #end param
  $JAVA_OPTS = Get-Java-Opts #takes care of looking at defind settings and/or using env:JAVA_OPTS

  $PROG_ARGS = @()
  if ($JAVA_OPTS -ne $null){
  	$PROG_ARGS += $JAVA_OPTS
  }
  if ($logFile){
  	$PROG_ARGS += "-Dorg.jboss.boot.log.file=$logFile"
  }
  if ($logFileProperties){
  	$PROG_ARGS += "-Dlogging.configuration=file:$logFileProperties"
  }
  $PROG_ARGS += "-Djboss.home.dir=$JBOSS_HOME"
  $PROG_ARGS += "-Djboss.server.base.dir=$global:JBOSS_BASE_DIR"
  $PROG_ARGS += "-Djboss.server.config.dir=$global:JBOSS_CONFIG_DIR"

  if ($GC_LOG -eq $true){
    if ($PROG_ARGS -notcontains "-verbose:gc"){
        Rotate-GC-Logs
		if (-not(Test-Path $JBOSS_LOG_DIR)) {
			$dir = New-Item $JBOSS_LOG_DIR -type directory -ErrorAction SilentlyContinue
		}
        $PROG_ARGS += "-verbose:gc"
        $PROG_ARGS += "-XX:+PrintGCDetails"
        $PROG_ARGS += "-XX:+PrintGCDateStamps"
        $PROG_ARGS += "-XX:+UseGCLogFileRotation"
        $PROG_ARGS += "-XX:NumberOfGCLogFiles=5"
        $PROG_ARGS += "-XX:GCLogFileSize=3M"
        $PROG_ARGS += "-XX:-TraceClassUnloading"
        $PROG_ARGS += "-Xloggc:$JBOSS_LOG_DIR\gc.log"
    }
  }
  $global:FINAL_JAVA_OPTS = $PROG_ARGS

  $PROG_ARGS += "-jar"
  $PROG_ARGS += "$JBOSS_HOME\jboss-modules.jar"
  if ($MODULE_OPTS -ne $null){
  	$PROG_ARGS += $MODULE_OPTS
  }
  $PROG_ARGS += "-mp"
  $PROG_ARGS += "$JBOSS_MODULEPATH"
  $PROG_ARGS += $entryModule
  if ($serverOpts -ne $null){
  	$PROG_ARGS += $serverOpts
  }
  return $PROG_ARGS
}

Function Process-Script-Parameters {
Param(
   [Parameter(Mandatory=$false)]
   [string[]]$Params

) #end param
    $res = @()
	for($i=0; $i -lt $Params.Count; $i++){
		$arg = $Params[$i]
		if ($arg -eq '--debug'){
			$global:DEBUG_MODE=$true
			if ($args[$i+1] -match '\d+'){ #port number can only follow --debug
				$global:DEBUG_PORT = $Params[$i+1]
				$i++
				continue
			}
		}elseif ($arg -contains '-Djava.security.manager'){
			Write-Warning "ERROR: The use of -Djava.security.manager has been removed. Please use the -secmgr command line argument or SECMGR=true environment variable."
			exit
		}elseif ($arg -eq '-secmgr'){
			$global:SECMGR = $true
		}elseif ($arg -eq '--background'){
			$global:RUN_IN_BACKGROUND = $true
		}else{
			$res+=$arg
		}
	}
	return $res
}

Function Start-WildFly-Process {
 Param(
   [Parameter(Mandatory=$true)]
   [string[]] $programArguments,
   [boolean] $runInBackground = $false

) #end param

	if(($JBOSS_PIDFILE -ne '') -and (Test-Path $JBOSS_PIDFILE)) {
		$processId = gc $JBOSS_PIDFILE
		if ($processId -ne $null){
			$proc = Get-Process -Id $processId -ErrorAction SilentlyContinue
		}
		if ($proc -ne $null){
			Write-Warning "Looks like a server process is already running. If it isn't then, remove the $JBOSS_PIDFILE and try again"
			return
		}else{
			Remove-Item $JBOSS_PIDFILE
		}
	}

	if($runInBackground) {
		$process = Start-Process -FilePath $JAVA -ArgumentList $programArguments -NoNewWindow -RedirectStandardOutput $global:CONSOLE_LOG -WorkingDirectory $JBOSS_HOME -PassThru
		$processId = $process.Id;
		echo "Started process in background, process id: $processId"
		if ($JBOSS_PIDFILE -ne $null){
			$processId >> $JBOSS_PIDFILE
		}
	} else {
		try{
			pushd $JBOSS_HOME
			& $JAVA $programArguments
			if ($LastExitCode -eq 10){ # :shutdown(restart=true) was called
			    Write-Host "Restarting application server..."
				Start-WildFly-Process -programArguments $programArguments
			}

		}finally{
			popd
		}
	}
	Env-Clean-Up
}

Function Set-Global-Variables {
PARAM(
[Parameter(Mandatory=$true)]
   [string]$baseDir
)

	# determine the default base dir, if not set
	$global:JBOSS_BASE_DIR = $baseDir;

	# determine the default log dir, if not set
	$global:JBOSS_LOG_DIR = Get-Env JBOSS_LOG_DIR $JBOSS_BASE_DIR\log

	# determine the default configuration dir, if not set
	$global:JBOSS_CONFIG_DIR = Get-Env JBOSS_CONFIG_DIR $JBOSS_BASE_DIR\configuration

	$global:CONSOLE_LOG = $JBOSS_LOG_DIR + '\console.log'
}

Function Set-Global-Variables-Standalone {
	$dir = Get-Env JBOSS_BASE_DIR $JBOSS_HOME\standalone
	Set-Global-Variables -baseDir $dir
}

Function Set-Global-Variables-Domain {
	$dir = Get-Env JBOSS_BASE_DIR $JBOSS_HOME\domain
	Set-Global-Variables -baseDir $dir
}

Function Env-Clean-Up {
	[Environment]::SetEnvironmentVariable("JBOSS_HOME", $null, "Process")
}

Function Rotate-GC-Logs {
	mv -ErrorAction SilentlyContinue $JBOSS_LOG_DIR/gc.log.0 $JBOSS_LOG_DIR/backupgc.log.0
	mv -ErrorAction SilentlyContinue $JBOSS_LOG_DIR/gc.log.1 $JBOSS_LOG_DIR/backupgc.log.1
	mv -ErrorAction SilentlyContinue $JBOSS_LOG_DIR/gc.log.2 $JBOSS_LOG_DIR/backupgc.log.2
	mv -ErrorAction SilentlyContinue $JBOSS_LOG_DIR/gc.log.3 $JBOSS_LOG_DIR/backupgc.log.3
	mv -ErrorAction SilentlyContinue $JBOSS_LOG_DIR/gc.log.4 $JBOSS_LOG_DIR/backupgc.log.4
	mv -ErrorAction SilentlyContinue $JBOSS_LOG_DIR/gc.log.*.current $JBOSS_LOG_DIR/backupgc.log.current
}

Function Check-For-GC-Log {
	if (GC_LOG){
		$args = (,'-verbose:gc',"-Xloggc:$JBOSS_LOG_DIR/gc.log","-XX:+PrintGCDetails","-XX:+PrintGCDateStamps","-XX:+UseGCLogFileRotation","-XX:NumberOfGCLogFiles=5","-XX:GCLogFileSize=3M","-XX:-TraceClassUnloading",'-version')
		$OutputVariable = (&$JAVA $args )  | Out-String
	}
}

# Setup JBOSS_HOME
if((Test-Path env:JBOSS_HOME) -and (Test-Path (Get-Item env:JBOSS_HOME))) {# checks if env variable jboss is set and is valid folder
  $SANITIZED_JBOSS_HOME = (Get-Item env:JBOSS_HOME).FullName
  if($SANITIZED_JBOSS_HOME -ne $RESOLVED_JBOSS_HOME) {
    echo "WARNING JBOSS_HOME may be pointing to a different installation - unpredictable results may occur."
    echo ""
  }
  $JBOSS_HOME=$SANITIZED_JBOSS_HOME
} else {
    # get the full path (without any relative bits)
    $JBOSS_HOME=$RESOLVED_JBOSS_HOME
}

# Setup the JVM
if (!(Test-Path env:JAVA)) {
  if( Test-Path env:JAVA_HOME) {
	$JAVA_HOME = (Get-ChildItem env:JAVA_HOME).Value
	$JAVA = $JAVA_HOME + "\bin\java.exe"
  } else {
    $JAVA = 'java'
  }
}

# determine the default module path, if not set
$JBOSS_MODULEPATH = Get-Env JBOSS_MODULEPATH $JBOSS_HOME\modules

Set-Global-Variables-Standalone

# Determine the default JBoss PID file
$JBOSS_PIDFILE = Get-Env JBOSS_PIDFILE $SCRIPTS_HOME\process.pid

[Environment]::SetEnvironmentVariable("JBOSS_HOME", $JBOSS_HOME, "Process")