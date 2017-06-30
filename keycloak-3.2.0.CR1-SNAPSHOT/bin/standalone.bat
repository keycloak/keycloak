@echo off
rem -------------------------------------------------------------------------
rem JBoss Bootstrap Script for Windows
rem -------------------------------------------------------------------------

rem Use --debug to activate debug mode with an optional argument to specify the port
rem Usage : standalone.bat --debug
rem         standalone.bat --debug 9797

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT" setlocal

rem By default debug mode is disable.
set DEBUG_MODE=false
set DEBUG_PORT_VAR=8787
rem Set to all parameters by default
set "SERVER_OPTS=%*"


if NOT "x%DEBUG%" == "x" (
  set "DEBUG_MODE=%DEBUG%
)

rem Get the program name before using shift as the command modify the variable ~nx0
if "%OS%" == "Windows_NT" (
  set "PROGNAME=%~nx0%"
) else (
  set "PROGNAME=standalone.bat"
)

if "%OS%" == "Windows_NT" (
  set "DIRNAME=%~dp0%"
) else (
  set DIRNAME=.\
)
setlocal EnableDelayedExpansion
rem check for the security manager system property
echo(!SERVER_OPTS! | findstr /r /c:"-Djava.security.manager" > nul
if not errorlevel == 1 (
    echo ERROR: The use of -Djava.security.manager has been removed. Please use the -secmgr command line argument or SECMGR=true environment variable.
    GOTO :EOF
)
setlocal DisableDelayedExpansion

rem Read command-line args, the ~ removes the quotes from the parameter
:READ-ARGS
if "%~1" == "" (
   goto MAIN
) else if "%~1" == "--debug" (
   goto READ-DEBUG-PORT
) else if "%~1" == "-secmgr" (
   set SECMGR=true
)
shift
goto READ-ARGS

:READ-DEBUG-PORT
set "DEBUG_MODE=true"
set DEBUG_ARG="%2"
if not %DEBUG_ARG% == "" (
   if x%DEBUG_ARG:-=%==x%DEBUG_ARG% (
      shift
      set DEBUG_PORT_VAR=%DEBUG_ARG%
   )
   shift
   goto READ-ARGS
)

:MAIN
rem $Id$
)

pushd "%DIRNAME%.."
set "RESOLVED_JBOSS_HOME=%CD%"
popd

if "x%JBOSS_HOME%" == "x" (
  set "JBOSS_HOME=%RESOLVED_JBOSS_HOME%"
)

pushd "%JBOSS_HOME%"
set "SANITIZED_JBOSS_HOME=%CD%"
popd

if /i "%RESOLVED_JBOSS_HOME%" NEQ "%SANITIZED_JBOSS_HOME%" (
   echo.
   echo   WARNING:  JBOSS_HOME may be pointing to a different installation - unpredictable results may occur.
   echo.
   echo       JBOSS_HOME: "%JBOSS_HOME%"
   echo.
)

rem Read an optional configuration file.
if "x%STANDALONE_CONF%" == "x" (
   set "STANDALONE_CONF=%DIRNAME%standalone.conf.bat"
)
if exist "%STANDALONE_CONF%" (
   echo Calling "%STANDALONE_CONF%"
   call "%STANDALONE_CONF%" %*
) else (
   echo Config file not found "%STANDALONE_CONF%"
)

if NOT "x%DEBUG_PORT%" == "x" (
  set DEBUG_PORT_VAR=%DEBUG_PORT%
)

if NOT "x%GC_LOG%" == "x" (
  set "GC_LOG=%GC_LOG%
)

rem Set debug settings if not already set
if "%DEBUG_MODE%" == "true" (
   echo "%JAVA_OPTS%" | findstr /I "\-agentlib:jdwp" > nul
  if errorlevel == 1 (
     set "JAVA_OPTS=%JAVA_OPTS% -agentlib:jdwp=transport=dt_socket,address=%DEBUG_PORT_VAR%,server=y,suspend=n"
  ) else (
     echo Debug already enabled in JAVA_OPTS, ignoring --debug argument
  )
)

set DIRNAME=

rem Setup JBoss specific properties
set "JAVA_OPTS=-Dprogram.name=%PROGNAME% %JAVA_OPTS%"

if "x%JAVA_HOME%" == "x" (
  set  JAVA=java
  echo JAVA_HOME is not set. Unexpected results may occur.
  echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
) else (
  if not exist "%JAVA_HOME%" (
    echo JAVA_HOME "%JAVA_HOME%" path doesn't exist
    goto END
  ) else (
    echo Setting JAVA property to "%JAVA_HOME%\bin\java"
    set "JAVA=%JAVA_HOME%\bin\java"
  )
)

if not "%PRESERVE_JAVA_OPTS%" == "true" (
  rem Add -client to the JVM options, if supported (32 bit VM), and not overriden
  echo "%JAVA_OPTS%" | findstr /I \-server > nul
  if errorlevel == 1 (
    "%JAVA%" -client -version 2>&1 | findstr /I /C:"Client VM" > nul
    if not errorlevel == 1 (
      set "JAVA_OPTS=-client %JAVA_OPTS%"
    )
  )
)


rem Find jboss-modules.jar, or we can't continue
if exist "%JBOSS_HOME%\jboss-modules.jar" (
    set "RUNJAR=%JBOSS_HOME%\jboss-modules.jar"
) else (
  echo Could not locate "%JBOSS_HOME%\jboss-modules.jar".
  echo Please check that you are in the bin directory when running this script.
  goto END
)

rem Setup JBoss specific properties

rem Setup directories, note directories with spaces do not work
setlocal EnableDelayedExpansion
set "CONSOLIDATED_OPTS=%JAVA_OPTS% %SERVER_OPTS%"
set baseDirFound=false
set configDirFound=false
set logDirFound=false
for %%a in (!CONSOLIDATED_OPTS!) do (
   if !baseDirFound! == true (
      set "JBOSS_BASE_DIR=%%~a"
      set baseDirFound=false
   )
   if !configDirFound! == true (
      set "JBOSS_CONFIG_DIR=%%~a"
      set configDirFound=false
   )
   if !logDirFound! == true (
      set "JBOSS_LOG_DIR=%%~a"
      set logDirFound=false
   )
   if "%%~a" == "-Djboss.server.base.dir" (
       set baseDirFound=true
   )
   if "%%~a" == "-Djboss.server.config.dir" (
       set configDirFound=true
   )
   if "%%~a" == "-Djboss.server.log.dir" (
       set logDirFound=true
   )
)

rem If the -Djava.security.manager is found, enable the -secmgr and include a bogus security manager for JBoss Modules to replace
echo(!JAVA_OPTS! | findstr /r /c:"-Djava.security.manager" > nul && (
    echo ERROR: The use of -Djava.security.manager has been removed. Please use the -secmgr command line argument or SECMGR=true environment variable.
    GOTO :EOF
)
setlocal DisableDelayedExpansion

rem Set default module root paths
if "x%JBOSS_MODULEPATH%" == "x" (
  set  "JBOSS_MODULEPATH=%JBOSS_HOME%\modules"
)

rem Set the standalone base dir
if "x%JBOSS_BASE_DIR%" == "x" (
  set  "JBOSS_BASE_DIR=%JBOSS_HOME%\standalone"
)
rem Set the standalone log dir
if "x%JBOSS_LOG_DIR%" == "x" (
  set  "JBOSS_LOG_DIR=%JBOSS_BASE_DIR%\log"
)
rem Set the standalone configuration dir
if "x%JBOSS_CONFIG_DIR%" == "x" (
  set  "JBOSS_CONFIG_DIR=%JBOSS_BASE_DIR%\configuration"
)

if not "%PRESERVE_JAVA_OPTS%" == "true" (
    if "%GC_LOG%" == "true" (
      rem Add rotating GC logs, if supported, and not already defined
      echo "%JAVA_OPTS%" | findstr /I "\-verbose:gc" > nul
      if errorlevel == 1 (
        rem Back up any prior logs
        move /y "%JBOSS_LOG_DIR%\gc.log.1" "%JBOSS_LOG_DIR%\backupgc.log.1" > nul 2>&1
        move /y "%JBOSS_LOG_DIR%\gc.log.0" "%JBOSS_LOG_DIR%\backupgc.log.0" > nul 2>&1
        move /y "%JBOSS_LOG_DIR%\gc.log.2" "%JBOSS_LOG_DIR%\backupgc.log.2" > nul 2>&1
        move /y "%JBOSS_LOG_DIR%\gc.log.3" "%JBOSS_LOG_DIR%\backupgc.log.3" > nul 2>&1
        move /y "%JBOSS_LOG_DIR%\gc.log.4" "%JBOSS_LOG_DIR%\backupgc.log.4" > nul 2>&1
        move /y "%JBOSS_LOG_DIR%\gc.log.*.current" "%JBOSS_LOG_DIR%\backupgc.log.current" > nul 2>&1

        "%JAVA%" -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=3M -Xloggc:"%JBOSS_LOG_DIR%\gc.log" -XX:-TraceClassUnloading -version > nul 2>&1
        if not errorlevel == 1 (
          if not exist "%JBOSS_LOG_DIR" > nul 2>&1 (
            mkdir "%JBOSS_LOG_DIR%"
          )
         set "JAVA_OPTS=-verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=3M -Xloggc:"%JBOSS_LOG_DIR%\gc.log" -XX:-TraceClassUnloading %JAVA_OPTS%"
        )
       )
    )
)




rem Set the module options
set "MODULE_OPTS="
if "%SECMGR%" == "true" (
    set "MODULE_OPTS=-secmgr"
)

echo ===============================================================================
echo.
echo   JBoss Bootstrap Environment
echo.
echo   JBOSS_HOME: "%JBOSS_HOME%"
echo.
echo   JAVA: "%JAVA%"
echo.
echo   JAVA_OPTS: "%JAVA_OPTS%"
echo.
echo ===============================================================================
echo.

:RESTART
  "%JAVA%" %JAVA_OPTS% ^
   "-Dorg.jboss.boot.log.file=%JBOSS_LOG_DIR%\server.log" ^
   "-Dlogging.configuration=file:%JBOSS_CONFIG_DIR%/logging.properties" ^
      -jar "%JBOSS_HOME%\jboss-modules.jar" ^
      %MODULE_OPTS% ^
      -mp "%JBOSS_MODULEPATH%" ^
      org.jboss.as.standalone ^
      "-Djboss.home.dir=%JBOSS_HOME%" ^
      %SERVER_OPTS%

if ERRORLEVEL 10 goto RESTART

:END
if "x%NOPAUSE%" == "x" pause

:END_NO_PAUSE
