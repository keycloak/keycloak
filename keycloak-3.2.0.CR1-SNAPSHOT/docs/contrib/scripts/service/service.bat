@echo off

REM -------------------------------------------------------------------------
REM  WildFly Service Script for Windows
REM    It has to reside in %JBOSS_HOME%\bin
REM    It is expecting that prunsrv.exe reside in:
REM      %JBOSS_HOME%\bin\service\
REM Easiest way to make it work is to copy whole "service" directory to %JBOSS_HOME%\bin
REM
REM  v9 2016-02-16 customize for WildFly, fix working on paths with spaces (Tomaz Cerar)
REM  v8 2016-01-20 customize for EAP 7 (Petr Sakar)
REM  v7 2014-07-09 added /logpath /startup /config /hostconfig /base /debug
REM                      simplified/corrected use of quotes
REM
REM  v6 was shipped with EAP 6.2.0 and supports all previous versions of 6.x as well.
REM  v6 2013-08-21 added /name /desc
REM                added /serviceuser /servicepass
REM                extended directory checking for versions and locations
REM                extended checking on option usage
REM  v5	2013-06-10 adapted for EAP 6.1.0
REM  v4	2012-10-03 Small changes to properly handles spaces in LogPath, StartPath,
REM                and StopPath (George Rypysc)
REM  v3	2012-09-14 fixed service log path
REM                cmd line options for controller,domain host, loglevel,
REM		   username,password
REM  v2	2012-09-05 NOPAUSE support
REM  v1	2012-08-20 initial edit
REM
REM Author: Tom Fonteyne (unless noted above)
REM ========================================================
setlocal EnableExtensions EnableDelayedExpansion

set DEBUG=0
if "%DEBUG%" == "1" (
	echo "Debug info enabled"
	echo on
)

set "DIRNAME=%~dp0%"
if "%DEBUG%" == "1" (
	echo DIRNAME "%DIRNAME%x"
)

if exist "%DIRNAME%..\jboss-modules.jar" (
  REM we are in JBOSS_HOME/bin
  set "WE=%DIRNAME%..\"
  goto :WE_FOUND
) else if exist "%DIRNAME%..\..\jboss-modules.jar" (
  REM we are in bin\service in a WildFly installation
  set "WE=%DIRNAME%..\..\"
  goto :WE_FOUND
) else if exist "%DIRNAME%..\..\..\jboss-modules.jar" (
  REM we are in sbin in a 6.0.x installation
  set "WE=%DIRNAME%..\..\..\"
  goto :WE_FOUND
) else (
  REM we should be in sbin in 6.1 and up
  set "WE=%DIRNAME%..\..\..\..\..\..\"
)

if "%DEBUG%" == "1" (
	echo WE was not found, using "%WE%"
)

:WE_FOUND
if "%DEBUG%" == "1" (
	echo WE "%WE%"
)
pushd "%WE%"
set "RESOLVED_JBOSS_HOME=%CD%"
popd
set WE=
set DIRNAME=
if "x%JBOSS_HOME%" == "x" (
  set "JBOSS_HOME=%RESOLVED_JBOSS_HOME%"
)

pushd "%JBOSS_HOME%"
set "SANITIZED_JBOSS_HOME=%CD%"
popd

if "%DEBUG%" == "1" (
	echo SANITIZED_JBOSS_HOME="%SANITIZED_JBOSS_HOME%"
	echo RESOLVED_JBOSS_HOME="%RESOLVED_JBOSS_HOME%"
	echo JBOSS_HOME="%JBOSS_HOME%"
)

if not "%RESOLVED_JBOSS_HOME%x" == "%SANITIZED_JBOSS_HOME%x" (
    echo WARNING JBOSS_HOME may be pointing to a different installation - unpredictable results may occur.
    goto cmdEnd
)

rem Find jboss-modules.jar to check JBOSS_HOME
if not exist "%JBOSS_HOME%\jboss-modules.jar" (
  echo Could not locate "%JBOSS_HOME%\jboss-modules.jar"
  goto cmdEnd
)

set PRUNSRV=
if "%PROCESSOR_ARCHITECTURE%"=="AMD64" (
  echo Using the X86-64bit version of prunsrv
  set PRUNSRV="%JBOSS_HOME%\bin\service\amd64\wildfly-service"
) else (
  echo Using the X86-32bit version of prunsrv
  set PRUNSRV="%JBOSS_HOME%\bin\service\wildfly-service"
)

if "%DEBUG%" == "1" (
	echo PRUNSRV %PRUNSRV%
)

echo(

rem defaults
set SHORTNAME=Wildfly
set DISPLAYNAME=WildFly
rem NO quotes around the description here !
set DESCRIPTION=WildFly Application Server
set CONTROLLER=localhost:9990
set DC_HOST=master
set IS_DOMAIN=false
set LOGLEVEL=INFO
set LOGPATH=
set JBOSSUSER=
set JBOSSPASS=
set SERVICE_USER=
set SERVICE_PASS=
set STARTUP_MODE=manual
set ISDEBUG=
set CONFIG=
set HOSTCONFIG=host.xml
set BASE=

set COMMAND=%1
shift
if /I "%COMMAND%" == "install"   goto cmdInstall
if /I "%COMMAND%" == "uninstall" goto cmdUninstall
if /I "%COMMAND%" == "start"     goto cmdStart
if /I "%COMMAND%" == "stop"      goto cmdStop
if /I "%COMMAND%" == "restart"   goto cmdRestart

echo ERROR: invalid command

:cmdUsage
echo WildFly Service Script for Windows
echo Usage:
echo(
echo   service install ^<options^>  , where the options are:
echo(
echo     /startup                  : Set the service to auto start
echo                                 Not specifying sets the service to manual
echo(
echo     /jbossuser ^<username^>     : JBoss username to use for the shutdown command.
echo     /jbosspass ^<password^>     : Password for /jbossuser
echo(
echo     /controller ^<host:port^>   : The host:port of the management interface.
echo                                 default: %CONTROLLER%
echo(
echo     /host [^<domainhost^>]      : Indicates that domain mode is to be used,
echo                                 with an optional domain/host controller name.
echo                                 default: %DC_HOST%
echo                                 Not specifying /host will install JBoss in
echo                                 standalone mode.
echo(
echo Options to use when multiple services or different accounts are needed:
echo(
echo     /name ^<servicename^>       : The name of the service
echo(
echo                                 default: %SHORTNAME%
echo     /desc ^<description^>       : The description of the service, use double
echo                                 quotes to allow spaces.
echo                                 Maximum 1024 characters.
echo                                 default: %DESCRIPTION%
echo(
echo     /serviceuser ^<username^>   : Specifies the name of the account under which
echo                                 the service should run.
echo                                 Use an account name in the form of
echo                                 DomainName\UserName
echo                                 default: not used, the service runs as
echo                                 Local System Account.
echo     /servicepass ^<password^>   : password for /serviceuser
echo(
echo Advanced options:
echo(
echo     /config ^<xmlfile^>         : The server-config to use
echo                                 default: standalone.xml / domain.xml
echo     /hostconfig ^<xmlfile^>     : domain mode only, the host config to use
echo                                 default: host.xml
echo(
echo     /base ^<directory^>         : The base directory for server/domain content
echo                                 Must be specified as a fully qualified path
echo                                 default: %JBOSS_HOME%\standalone or
echo                                          %JBOSS_HOME%\domain
echo(
echo     /loglevel ^<level^>         : The log level for the service:  Error, Info,
echo                                 Warn or Debug ^(Case insensitive^)
echo                                 default: %LOGLEVEL%
echo     /logpath ^<path^>           : Path of the log
echo                                 default depends on domain or standalone mode
echo                                 /base applies when /logpath is not set.
echo                                   %JBOSS_HOME%\domain\log
echo                                   %JBOSS_HOME%\standalone\log
echo(
echo     /debug                    : run the service install in debug mode
echo(
echo Other commands:
echo(
echo   service uninstall [/name ^<servicename^>]
echo   service start [/name ^<servicename^>]
echo   service stop [/name ^<servicename^>]
echo   service restart [/name ^<servicename^>]
echo(
echo     /name  ^<servicename^>      : Name of the service: should not contain spaces
echo                                 default: %SHORTNAME%
echo(
goto endBatch

:cmdInstall

:LoopArgs
if "%~1" == "" goto doInstall

if /I "%~1"== "/debug" (
  set ISDEBUG=true
  shift
  goto LoopArgs
)
if /I "%~1"== "/startup" (
  set STARTUP_MODE=auto
  shift
  goto LoopArgs
)
if /I "%~1"== "/config" (
  set CONFIG=
  if not "%~2"=="" (
    set T=%~2
    if not "!T:~0,1!"=="/" (
      set CONFIG=%~2
    )
  )
  if "!CONFIG!" == "" (
    echo ERROR: You need to specify a config name
    goto endBatch
  )
  shift
  shift
  goto LoopArgs
)
if /I "%~1"== "/hostconfig" (
  set HOSTCONFIG=
  if not "%~2"=="" (
    set T=%~2
    if not "!T:~0,1!"=="/" (
      set HOSTCONFIG=%~2
    )
  )
  if "!HOSTCONFIG!" == "" (
    echo ERROR: You need to specify a host-config name
    goto endBatch
  )
  shift
  shift
  goto LoopArgs
)
if /I "%~1"== "/base" (
  set BASE=
  if not "%~2"=="" (
    set T=%~2
    if not "!T:~0,1!"=="/" (
      set BASE=%~2
    )
  )
  if "!BASE!" == "" (
    echo ERROR: You need to specify a base directory name
    goto endBatch
  )
  shift
  shift
  goto LoopArgs
)
if /I "%~1"== "/controller" (
  set CONTROLLER=
  if not "%~2"=="" (
    set T=%~2
    if not "!T:~0,1!"=="/" (
      set CONTROLLER=%~2
    )
  )
  if "!CONTROLLER!" == "" (
    echo ERROR: The management interface should be specified in the format host:port, example:  127.0.0.1:9999
    goto endBatch
  )
  shift
  shift
  goto LoopArgs
)
if /I "%~1"== "/name" (
  set SHORTNAME=
  if not "%~2"=="" (
    set T=%~2
    if not "!T:~0,1!"=="/" (
      set SHORTNAME=%~2
      set DISPLAYNAME=%~2
    )
  )
  if "!SHORTNAME!" == "" (
    echo ERROR: You need to specify a service name
    goto endBatch
  )
  shift
  shift
  goto LoopArgs
)
if /I "%~1"== "/desc" (
  set DESCRIPTION=
  if not "%~2"=="" (
    set T=%~2
    if not "!T:~0,1!"=="/" (
      set DESCRIPTION=%~2
    )
  )
  if "!DESCRIPTION!" == "" (
    echo ERROR: You need to specify a description, maximum of 1024 characters
    goto endBatch
  )
  shift
  shift
  goto LoopArgs
)
if /I "%~1"== "/jbossuser" (
  set JBOSSUSER=
  if not "%~2"=="" (
    set T=%~2
    if not "!T:~0,1!"=="/" (
      set JBOSSUSER=%~2
    )
  )
  if "!JBOSSUSER!" == "" (
    echo ERROR: You need to specify a username
    goto endBatch
  )
  shift
  shift
  goto LoopArgs
)
if /I "%~1"== "/jbosspass" (
  set JBOSSPASS=
  if not "%~2"=="" (
    set T=%~2
    if not "!T:~0,1!"=="/" (
      set JBOSSPASS=%~2
    )
  )
  if "!JBOSSPASS!" == "" (
    echo ERROR: You need to specify a password for /jbosspass
    goto endBatch
  )
  shift
  shift
  goto LoopArgs
)
if /I "%~1"== "/serviceuser" (
  set SERVICE_USER=
  if not "%~2"=="" (
    set T=%~2
    if not "!T:~0,1!"=="/" (
      set SERVICE_USER=%~2
    )
  )
  if "!SERVICE_USER!" == "" (
    echo ERROR: You need to specify a username in the format DOMAIN\USER, or .\USER for the local domain
    goto endBatch
  )
  shift
  shift
  goto LoopArgs
)
if /I "%~1"== "/servicepass" (
  set SERVICE_PASS=
  if not "%~2"=="" (
    set T=%~2
    if not "!T:~0,1!"=="/" (
      set SERVICE_PASS=%~2
    )
  )
  if "!SERVICE_PASS!" == "" (
    echo ERROR: You need to specify a password for /servicepass
    goto endBatch
  )
  shift
  shift
  goto LoopArgs
)
rem the hostname is optional
if /I "%~1"== "/host" (
  set IS_DOMAIN=true
  if not "%~2"=="" (
    set T=%~2
    if not "!T:~0,1!"=="/" (
      set DC_HOST=%~2
      shift
    )
  )
  shift
  goto LoopArgs
)
if /I "%~1"== "/loglevel" (
  if /I not "%~2"=="Error" if /I not "%~2"=="Info" if /I not "%~2"=="Warn" if /I not "%~2"=="Debug" (
    echo ERROR: /loglevel must be set to Error, Info, Warn or Debug ^(Case insensitive^)
    goto endBatch
  )
  set LOGLEVEL=%~2
  shift
  shift
  goto LoopArgs
)
if /I "%~1"== "/logpath" (
  set LOGPATH=
  if not "%~2"=="" (
    set T=%~2
    if not "!T:~0,1!"=="/" (
      set LOGPATH=%~2
	)
  )
  if "!LOGPATH!" == "" (
    echo ERROR: You need to specify a path for the service log
    goto endBatch
  )
  shift
  shift
  goto LoopArgs
)
echo ERROR: Unrecognised option: %1
echo(
goto cmdUsage

:doInstall
set CREDENTIALS=
if not "%JBOSSUSER%" == "" (
  if "%JBOSSPASS%" == "" (
    echo When specifying a user, you need to specify the password
    goto endBatch
  )
  set CREDENTIALS=--user=%JBOSSUSER% --password=%JBOSSPASS%
)

set RUNAS=
if not "%SERVICE_USER%" == "" (
  if "%SERVICE_PASS%" == "" (
    echo When specifying a user, you need to specify the password
    goto endBatch
  )
  set RUNAS=--ServiceUser="%SERVICE_USER%" --ServicePassword="%SERVICE_PASS%"
)

if "%STDOUT%"=="" set STDOUT=auto
if "%STDERR%"=="" set STDERR=auto

if "%START_PATH%"=="" set START_PATH="%JBOSS_HOME%\bin"
if "%STOP_PATH%"=="" set STOP_PATH="%JBOSS_HOME%\bin"

if "%STOP_SCRIPT%"=="" set STOP_SCRIPT=jboss-cli.bat

if /I "%IS_DOMAIN%" == "true" (
  if "%BASE%"=="" set "BASE=%JBOSS_HOME%\domain"
  if "%CONFIG%"=="" set CONFIG=domain.xml
  if "%START_SCRIPT%"=="" set START_SCRIPT=domain.bat
  set STARTPARAM="/c#set#NOPAUSE=Y#&&#!START_SCRIPT!#-Djboss.domain.base.dir=!BASE!#--domain-config=!CONFIG!#--host-config=!HOSTCONFIG!"
  set STOPPARAM="/c %STOP_SCRIPT% --controller=%CONTROLLER% --connect %CREDENTIALS% --command=/host=!DC_HOST!:shutdown"
) else (
  if "%BASE%"=="" set "BASE=%JBOSS_HOME%\standalone"
  if "%CONFIG%"=="" set CONFIG=standalone.xml
  if "%START_SCRIPT%"=="" set START_SCRIPT=standalone.bat
  set STARTPARAM="/c#set#NOPAUSE=Y#&&#!START_SCRIPT!#-Djboss.server.base.dir=!BASE!#--server-config=!CONFIG!"
  set STOPPARAM="/c !STOP_SCRIPT! --controller=%CONTROLLER% --connect %CREDENTIALS% --command=:shutdown"
)

if "%LOGPATH%"=="" set LOGPATH="!BASE!\log"

if not exist "%BASE%" (
  echo The base directory does not exist: "%BASE%"
  goto endBatch
)

if not exist "%BASE%\configuration\%CONFIG%" (
  echo The configuration does not exist: "%BASE%\configuration\%CONFIG%"
  goto endBatch
)

if /I "%ISDEBUG%" == "true" (
  echo JBOSS_HOME="%JBOSS_HOME%"
  echo RUNAS=%RUNAS%
  echo SHORTNAME="%SHORTNAME%"
  echo DESCRIPTION="%DESCRIPTION%"
  echo STARTPARAM=%STARTPARAM%
  echo STOPPARAM=%STOPPARAM%
  echo LOGLEVEL=%LOGLEVEL%
  echo LOGPATH=%LOGPATH%
  echo CREDENTIALS=%CREDENTIALS%
  echo BASE="%BASE%"
  echo CONFIG="%CONFIG%"
  echo START_SCRIPT=%START_SCRIPT%
  echo START_PATH=%START_PATH%
  echo STOP_SCRIPT=%STOP_SCRIPT%
  echo STOP_PATH=%STOP_PATH%
  echo STDOUT="%STDOUT%"
  echo STDERR="%STDERR%"
)
if /I "%ISDEBUG%" == "true" (
  @echo on
)

@rem quotes around the "%DESCRIPTION%" but nowhere else
echo %PRUNSRV% install %SHORTNAME% %RUNAS% --DisplayName=%DISPLAYNAME% --Description="%DESCRIPTION%" --LogLevel=%LOGLEVEL% --LogPath=%LOGPATH% --LogPrefix=service --StdOutput=%STDOUT% --StdError=%STDERR% --StartMode=exe --Startup=%STARTUP_MODE% --StartImage=cmd.exe --StartPath=%START_PATH% ++StartParams=%STARTPARAM% --StopMode=exe --StopImage=cmd.exe --StopPath=%STOP_PATH%  ++StopParams=%STOPPARAM%

%PRUNSRV% install %SHORTNAME% %RUNAS% --DisplayName=%DISPLAYNAME% --Description="%DESCRIPTION%" --LogLevel=%LOGLEVEL% --LogPath=%LOGPATH% --LogPrefix=service --StdOutput=%STDOUT% --StdError=%STDERR% --StartMode=exe --Startup=%STARTUP_MODE% --StartImage=cmd.exe --StartPath=%START_PATH% ++StartParams=%STARTPARAM% --StopMode=exe --StopImage=cmd.exe --StopPath=%STOP_PATH%  ++StopParams=%STOPPARAM%
@rem %PRUNSRV% install "%SHORTNAME%" "%RUNAS%" --DisplayName="%DISPLAYNAME%" --Description="%DESCRIPTION%" --LogLevel="%LOGLEVEL%" --LogPath="%LOGPATH%" --LogPrefix=service --StdOutput="%STDOUT%" --StdError="%STDERR%" --StartMode=exe --Startup="%STARTUP_MODE%" --StartImage=cmd.exe --StartPath="%START_PATH%" ++StartParams="%STARTPARAM%" --StopMode=exe --StopImage=cmd.exe --StopPath="%STOP_PATH%"  ++StopParams="%STOPPARAM%"

@if /I "%ISDEBUG%" == "true" (
  @echo off
)

if errorlevel 8 (
  echo ERROR: The service %SHORTNAME% already exists
  goto endBatch
)
if errorlevel 0 (
  echo Service %SHORTNAME% installed
  goto endBatch
)
goto cmdEnd


REM the other commands take a /name parameter - if there is no ^<servicename^> passed as second parameter,
REM we silently ignore this and use the default SHORTNAME

:cmdUninstall
if /I "%~1"=="/name" (
  if not "%~2"=="" (
    set SHORTNAME="%~2"
  )
)
%PRUNSRV% stop %SHORTNAME%
if errorlevel 0 (
  %PRUNSRV% delete %SHORTNAME%
  if errorlevel 0 (
    echo Service %SHORTNAME% uninstalled
  )
) else (
  echo Unable to stop the service %SHORTNAME%
)
goto cmdEnd

:cmdStart
if /I "%~1"=="/name" (
  if not "%~2"=="" (
    set SHORTNAME="%~2"
  )
)
%PRUNSRV% start %SHORTNAME%
echo Service %SHORTNAME% starting...
goto cmdEnd

:cmdStop
if /I "%~1"=="/name" (
  if not "%~2"=="" (
    set SHORTNAME="%~2"
  )
)
%PRUNSRV% stop %SHORTNAME%
echo Service %SHORTNAME% stopping...
goto cmdEnd

:cmdRestart
if /I "%~1"=="/name" (
  if not "%~2"=="" (
    set SHORTNAME="%~2"
  )
)
%PRUNSRV% stop %SHORTNAME%
echo Service %SHORTNAME% stopping...
if "%errorlevel%" == "0" (
  %PRUNSRV% start %SHORTNAME%
  echo Service %SHORTNAME% starting...
) else (
  echo Unable to stop the service %SHORTNAME%
)
goto cmdEnd


:cmdEnd
REM if there is a need to add other error messages, make sure to list higher numbers first !
if errorlevel 2 (
  echo ERROR: Failed to load service %SHORTNAME% configuration
  goto endBatch
)
if errorlevel 0 (
  goto endBatch
)
echo "Unforseen error=%errorlevel%"

rem nothing below, exit
:endBatch
