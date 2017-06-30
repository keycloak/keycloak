@echo off
rem -------------------------------------------------------------------------
rem JBoss Bootstrap Script for Windows
rem -------------------------------------------------------------------------

rem $Id$

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT" setlocal
rem Set to all parameters by default
set SERVER_OPTS=%*

if "%OS%" == "Windows_NT" (
  set "DIRNAME=%~dp0%"
) else (
  set DIRNAME=.\
)

pushd "%DIRNAME%.."
set "RESOLVED_JBOSS_HOME=%CD%"
popd



setlocal EnableDelayedExpansion
rem check for the security manager system property
echo(!SERVER_OPTS! | findstr /r /c:"-Djava.security.manager" > nul
if not errorlevel == 1 (
    echo ERROR: The use of -Djava.security.manager has been removed. Please use the -secmgr command line argument or SECMGR=true environment variable.
    GOTO :EOF
)
setlocal DisableDelayedExpansion

rem Read command-line args.
:READ-ARGS
if "%~1" == "" (
   goto MAIN
) else if "%~1" == "-secmgr" (
   set SECMGR=true
)
shift
goto READ-ARGS

:MAIN

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
if "x%DOMAIN_CONF%" == "x" (
   set "DOMAIN_CONF=%DIRNAME%domain.conf.bat"
)
if exist "%DOMAIN_CONF%" (
   echo Calling "%DOMAIN_CONF%"
   call "%DOMAIN_CONF%" %*
) else (
   echo Config file not found "%DOMAIN_CONF%"
)

set DIRNAME=

if "%OS%" == "Windows_NT" (
  set "PROGNAME=%~nx0%"
) else (
  set "PROGNAME=domain.bat"
)

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

rem Add -server to the JVM options, if supported
"%JAVA%" -server -version 2>&1 | findstr /I hotspot > nul
if not errorlevel == 1 (
  set "PROCESS_CONTROLLER_JAVA_OPTS=%PROCESS_CONTROLLER_JAVA_OPTS% -server"
  set "HOST_CONTROLLER_JAVA_OPTS=%HOST_CONTROLLER_JAVA_OPTS% -server"
)

rem Find run.jar, or we can't continue
if exist "%JBOSS_HOME%\jboss-modules.jar" (
    set "RUNJAR=%JBOSS_HOME%\jboss-modules.jar"
) else (
  echo Could not locate "%JBOSS_HOME%\jboss-modules.jar".
  echo Please check that you are in the bin directory when running this script.
  goto END
)

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
   if "%%~a" == "-Djboss.domain.base.dir" (
       set baseDirFound=true
   )
   if "%%~a" == "-Djboss.domain.config.dir" (
       set configDirFound=true
   )
   if "%%~a" == "-Djboss.domain.log.dir" (
       set logDirFound=true
   )
)

rem If the -Djava.security.manager is found, enable the -secmgr and include a bogus security manager for JBoss Modules to replace
echo(!PROCESS_CONTROLLER_JAVA_OPTS! | findstr /r /c:"-Djava.security.manager" > nul && (
  echo "ERROR: Support for using -Djava.security.manager has been removed. Please use -secmgr or set the environment variable SECMGR=true"
  GOTO :EOF
)
setlocal DisableDelayedExpansion

rem Setup JBoss specific properties

rem Set default module root paths
if "x%JBOSS_MODULEPATH%" == "x" (
  set  "JBOSS_MODULEPATH=%JBOSS_HOME%\modules"
)

rem Set the domain base dir
if "x%JBOSS_BASE_DIR%" == "x" (
  set  "JBOSS_BASE_DIR=%JBOSS_HOME%\domain"
)
rem Set the domain log dir
if "x%JBOSS_LOG_DIR%" == "x" (
  set  "JBOSS_LOG_DIR=%JBOSS_BASE_DIR%\log"
)
rem Set the domain configuration dir
if "x%JBOSS_CONFIG_DIR%" == "x" (
  set  "JBOSS_CONFIG_DIR=%JBOSS_BASE_DIR%\configuration"
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
echo   JAVA_OPTS: "%PROCESS_CONTROLLER_JAVA_OPTS%"
echo.
echo ===============================================================================
echo.

:RESTART
"%JAVA%" %PROCESS_CONTROLLER_JAVA_OPTS% ^
 "-Dorg.jboss.boot.log.file=%JBOSS_LOG_DIR%\process-controller.log" ^
 "-Dlogging.configuration=file:%JBOSS_CONFIG_DIR%/logging.properties" ^
    -jar "%JBOSS_HOME%\jboss-modules.jar" ^
    %MODULE_OPTS% ^
    -mp "%JBOSS_MODULEPATH%" ^
     org.jboss.as.process-controller ^
    -jboss-home "%JBOSS_HOME%" ^
    -jvm "%JAVA%" ^
    %MODULE_OPTS% ^
    -mp "%JBOSS_MODULEPATH%" ^
    -- ^
    "-Dorg.jboss.boot.log.file=%JBOSS_LOG_DIR%\host-controller.log" ^
    "-Dlogging.configuration=file:%JBOSS_CONFIG_DIR%/logging.properties" ^
    %HOST_CONTROLLER_JAVA_OPTS% ^
    -- ^
    -default-jvm "%JAVA%" ^
    %*

if ERRORLEVEL 10 goto RESTART

:END
if "x%NOPAUSE%" == "x" pause

:END_NO_PAUSE
