@echo off
rem -------------------------------------------------------------------------
rem Keycloak Startup Script
rem -------------------------------------------------------------------------

@if not "%ECHO%" == ""  echo %ECHO%
setlocal

rem Get the program name before using shift as the command modify the variable ~nx0
if "%OS%" == "Windows_NT" (
  set "PROGNAME=%~nx0%"
) else (
  set "PROGNAME=kc.bat"
)

if "%OS%" == "Windows_NT" (
  set "DIRNAME=%~dp0%"
) else (
  set DIRNAME=.\
)

set "SERVER_OPTS=-Djava.util.logging.manager=org.jboss.logmanager.LogManager -Dquarkus-log-max-startup-records=10000"

set DEBUG_MODE=false
set DEBUG_PORT_VAR=8787
set DEBUG_SUSPEND_VAR=n
set CONFIG_ARGS=

rem Read command-line args, the ~ removes the quotes from the parameter
:READ-ARGS
set "KEY=%~1"
if "%KEY%" == "" (
    goto MAIN
)
if "%KEY%" == "--debug" (
  set "DEBUG_MODE=true"
  set "DEBUG_PORT_VAR=%~2"
  if "%DEBUG_PORT_VAR%" == "" (
     set DEBUG_PORT_VAR=8787
  )
  if "%DEBUG_SUSPEND_VAR%" == "" (
     set DEBUG_SUSPEND_VAR=n
  )
  shift
  shift
  goto READ-ARGS
)
if "%KEY%" == "start-dev" (
  set "CONFIG_ARGS=%CONFIG_ARGS% --profile=dev %KEY% "
  shift
  goto READ-ARGS
)
if not "%KEY:~0,2%"=="--" if "%KEY:~0,2%"=="-D" (
  set "SERVER_OPTS=%SERVER_OPTS% %KEY%=%~2"
  shift
)
if not "%KEY:~0,2%"=="--" if not "%KEY:~0,1%"=="-" (
  set "CONFIG_ARGS=%CONFIG_ARGS% %KEY%"
)
if not "%KEY:~0,2%"=="-D" (
  if "%KEY:~0,1%"=="-" (
      if "%~2"=="" (
        set "CONFIG_ARGS=%CONFIG_ARGS% %KEY%"
      ) else (
        set "CONFIG_ARGS=%CONFIG_ARGS% %KEY% %~2%"
      )
      shift
  )
)
shift
goto READ-ARGS

:MAIN
if not "x%JAVA_OPTS%" == "x" (
  echo "JAVA_OPTS already set in environment; overriding default settings with values: %JAVA_OPTS%"
) else (
  set "JAVA_OPTS=-Xms64m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8"
)

if not "x%JAVA_OPTS_APPEND%" == "x" (
  echo "Appending additional Java properties to JAVA_OPTS: %JAVA_OPTS_APPEND%"
  set "JAVA_OPTS=%JAVA_OPTS% %JAVA_OPTS_APPEND%"
)

if NOT "x%DEBUG%" == "x" (
  set "DEBUG_MODE=%DEBUG%
)

if NOT "x%DEBUG_PORT%" == "x" (
  set DEBUG_PORT_VAR=%DEBUG_PORT%
)

if NOT "x%DEBUG_SUSPEND%" == "x" (
  set DEBUG_SUSPEND_VAR=%DEBUG_SUSPEND%
)

rem Set debug settings if not already set
if "%DEBUG_MODE%" == "true" (
   echo "%JAVA_OPTS%" | findstr /I "\-agentlib:jdwp" > nul
  if errorlevel == 1 (
     set "JAVA_OPTS=%JAVA_OPTS% -agentlib:jdwp=transport=dt_socket,address=%DEBUG_PORT_VAR%,server=y,suspend=%DEBUG_SUSPEND_VAR%"
  ) else (
     echo Debug already enabled in JAVA_OPTS, ignoring --debug argument
  )
)

rem Setup Keycloak specific properties
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
     if not exist "%JAVA_HOME%\bin\java.exe" (
       echo "%JAVA_HOME%\bin\java.exe" does not exist
       goto END
     )
    set "JAVA=%JAVA_HOME%\bin\java"
  )
)

set "CLASSPATH_OPTS=%DIRNAME%..\lib\quarkus-run.jar"

set "JAVA_RUN_OPTS=%JAVA_OPTS% -Dkc.home.dir="%DIRNAME%.." -Djboss.server.config.dir="%DIRNAME%..\conf" -Dkeycloak.theme.dir="%DIRNAME%..\themes" %SERVER_OPTS% -cp "%CLASSPATH_OPTS%" io.quarkus.bootstrap.runner.QuarkusEntryPoint %CONFIG_ARGS%"

SetLocal EnableDelayedExpansion

set "OPTIMIZED_OPTION=--optimized"
set "HELP_LONG_OPTION=--help"
set "BUILD_OPTION= build"
set IS_HELP_SHORT=false

echo "%CONFIG_ARGS%" | findstr /r "\<-h\>" > nul

if not errorlevel == 1 (
    set IS_HELP_SHORT=true
)

set START_SERVER=true

if "!CONFIG_ARGS:%OPTIMIZED_OPTION%=!"=="!CONFIG_ARGS!" if "!CONFIG_ARGS:%BUILD_OPTION%=!"=="!CONFIG_ARGS!" if "!CONFIG_ARGS:%HELP_LONG_OPTION%=!"=="!CONFIG_ARGS!" if "%IS_HELP_SHORT%" == "false" (
    setlocal enabledelayedexpansion

    "%JAVA%" -Dkc.config.build-and-exit=true %JAVA_RUN_OPTS%

    if not !errorlevel! == 0 (
        set START_SERVER=false
    )

    set "JAVA_RUN_OPTS=-Dkc.config.built=true %JAVA_RUN_OPTS%"
)

if "%START_SERVER%" == "true" (
    "%JAVA%" %JAVA_RUN_OPTS%
)

:END
