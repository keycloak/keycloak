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

set "SERVER_OPTS=-Djava.util.logging.manager=org.jboss.logmanager.LogManager"

set DEBUG_MODE=false
set DEBUG_PORT_VAR=8787

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
    shift
    shift
    goto READ-ARGS
)
if not "%KEY:~0,2%"=="--" if "%KEY:~0,1%"=="-" (
    set "SERVER_OPTS=%SERVER_OPTS% %KEY%=%~2"
    shift
)
if not "%KEY:~0,2%"=="--" if not "%KEY:~0,1%"=="-" (
    set "CONFIG_ARGS=%CONFIG_ARGS% %KEY%"
)
if "%KEY:~0,2%"=="--" (
    if "%~2"=="" (
        set "CONFIG_ARGS=%CONFIG_ARGS% %KEY%"
    ) else (
        set "CONFIG_ARGS=%CONFIG_ARGS% %KEY%=%~2%"
    )

    shift
)
shift
goto READ-ARGS

:MAIN
if not "x%JAVA_OPTS%" == "x" (
  echo "JAVA_OPTS already set in environment; overriding default settings with values: %JAVA_OPTS%"
) else (
  set "JAVA_OPTS=-Xms64m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m -Djava.net.preferIPv4Stack=true"
)

if NOT "x%DEBUG%" == "x" (
  set "DEBUG_MODE=%DEBUG%
)

if NOT "x%DEBUG_PORT%" == "x" (
  set DEBUG_PORT_VAR=%DEBUG_PORT%
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

set "CLASSPATH_OPTS=%DIRNAME%..\lib\quarkus-run.jar;%DIRNAME%..\lib\lib\main\*.*"

"%JAVA%" %JAVA_OPTS% -Dkc.home.dir="%DIRNAME%.." -Djboss.server.config.dir="%DIRNAME%..\conf" -Dkeycloak.theme.dir="%DIRNAME%..\themes" %SERVER_OPTS% -cp "%CLASSPATH_OPTS%" io.quarkus.bootstrap.runner.QuarkusEntryPoint %CONFIG_ARGS%

:END