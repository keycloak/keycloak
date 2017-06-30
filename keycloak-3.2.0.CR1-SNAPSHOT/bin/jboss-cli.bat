@echo off
setlocal ENABLEEXTENSIONS
rem the arguments can contain ')' character, this breaks parser. Delaying 
rem argument evaluation at script execution fixes it.
setlocal ENABLEDELAYEDEXPANSION
rem -------------------------------------------------------------------------
rem JBoss Admin CLI Script for Windows
rem -------------------------------------------------------------------------

rem $Id$

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT" setlocal

if "%OS%" == "Windows_NT" (
  set "DIRNAME=%~dp0%"
) else (
  set DIRNAME=.\
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

rem Setup JBoss specific properties
if "x%JAVA_HOME%" == "x" (
  set  JAVA=java
  echo JAVA_HOME is not set. Unexpected results may occur.
  echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
) else (
  set "JAVA=%JAVA_HOME%\bin\java"
)

rem Find jboss-modules.jar, or we can't continue
set "JBOSS_RUNJAR=%JBOSS_HOME%\jboss-modules.jar"
if not exist "%JBOSS_RUNJAR%" (
  echo Could not locate "%JBOSS_RUNJAR%".
  echo Please check that you are in the bin directory when running this script.
  set /A RC=1
  goto END
)

rem Set default module root paths
if "x%JBOSS_MODULEPATH%" == "x" (
  set "JBOSS_MODULEPATH=%JBOSS_HOME%\modules"
)

rem Add base package for L&F
set "JAVA_OPTS=%JAVA_OPTS% -Djboss.modules.system.pkgs=com.sun.java.swing"

set LOGGING_CONFIG=
echo "%JAVA_OPTS%" | findstr /I "logging.configuration" > nul
if errorlevel == 1 (
  rem It must be quoted in case JBOSS_HOME contains whitespaces 
  set LOGGING_CONFIG="-Dlogging.configuration=file:%JBOSS_HOME%\bin\jboss-cli-logging.properties"
) else (
  echo logging.configuration already set in JAVA_OPTS
)

rem script arguments will be evaluated at execution time.
set ARGS=%*

rem Force following commands to be loaded in memory.
rem This protects the running script from being rewritten.
(
    "%JAVA%" %JAVA_OPTS% %LOGGING_CONFIG% ^
        -jar "%JBOSS_RUNJAR%" ^
        -mp "%JBOSS_MODULEPATH%" ^
         org.jboss.as.cli ^
         !ARGS!

    set /A RC=%errorlevel%
    :END
    if "x%NOPAUSE%" == "x" pause

    if "x%RC%" == "x" (
      set /A RC=0
    )
    exit /B %RC%
)
