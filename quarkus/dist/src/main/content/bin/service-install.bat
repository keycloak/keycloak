@echo off
setlocal enabledelayedexpansion

set "DIRNAME=%~dp0"
if "%DIRNAME%" == "" set "DIRNAME=."
cd "%DIRNAME%\.."

set "KEYCLOAK_HOME=%cd%"
set SERVICE_NAME=keycloak
set SERVICE_DISPLAY_NAME=Keycloak Server
set SERVICE_DESCRIPTION=Keycloak Identity and Access Management
set STARTUP_MODE=auto
set SERVICE_USER=
set SERVICE_PASSWORD=

REM Parse command line parameters
:PARAM_LOOP
if "%~1" == "" goto PARAM_DONE
if /I "%~1" == "--name" (
    set "SERVICE_NAME=%~2"
    shift & shift & goto PARAM_LOOP
)
if /I "%~1" == "--display-name" (
    set "SERVICE_DISPLAY_NAME=%~2"
    shift & shift & goto PARAM_LOOP
)
if /I "%~1" == "--description" (
    set "SERVICE_DESCRIPTION=%~2"
    shift & shift & goto PARAM_LOOP
)
if /I "%~1" == "--startup" (
    set "STARTUP_MODE=%~2"
    shift & shift & goto PARAM_LOOP
)
if /I "%~1" == "--jvm" (
    set "JVM=%~2"
    shift & shift & goto PARAM_LOOP
)
if /I "%~1" == "--keycloak-args" (
    set "KC_OPTS=%~2"
    shift & shift & goto PARAM_LOOP
)
if /I "%~1" == "--jvm-args" (
    set "JVM_ARGS=%~2"
    shift & shift & goto PARAM_LOOP
)
if /I "%~1" == "--service-user" (
    set "SERVICE_USER=%~2"
    shift & shift & goto PARAM_LOOP
)
if /I "%~1" == "--service-password" (
    set "SERVICE_PASSWORD=%~2"
    shift & shift & goto PARAM_LOOP
)
if /I "%~1" == "--classpath-jar" (
    set "CLASSPATH_JAR=%~2"
    shift & shift & goto PARAM_LOOP
)
shift
goto PARAM_LOOP
:PARAM_DONE

REM Check if prunsrv is available
if exist "%KEYCLOAK_HOME%\bin\prunsrv.exe" goto SETUP_SERVICE

echo ERROR: Apache Commons Daemon (Procrun) executable not found.
echo Looking for prunsrv.exe in: %KEYCLOAK_HOME%\bin\prunsrv.exe
exit /b 1

:SETUP_SERVICE
set PRUNSRV="%KEYCLOAK_HOME%\bin\prunsrv.exe"

if not defined JVM (
    if defined JAVA_HOME (
        if exist "%JAVA_HOME%\bin\server\jvm.dll" (
            set "JVM=%JAVA_HOME%\bin\server\jvm.dll"
        ) else if exist "%JAVA_HOME%\jre\bin\server\jvm.dll" (
            set "JVM=%JAVA_HOME%\jre\bin\server\jvm.dll"
        ) else (
            echo ERROR: Cannot find jvm.dll in JAVA_HOME. Please specify --jvm parameter.
            exit /b 1
        )
    ) else (
        echo ERROR: JAVA_HOME not set. Please specify --jvm parameter.
        exit /b 1
    )
)

echo Using JVM at: "%JVM%"
if not exist "%JVM%" (
    echo ERROR: JVM DLL not found at "%JVM%"
    exit /b 1
)

REM Try to find quarkus-run.jar if not specified
if not defined CLASSPATH_JAR (
    echo Trying to locate quarkus-run.jar automatically...
    
    set "CLASSPATH_JAR=%KEYCLOAK_HOME%\lib\quarkus-run.jar"
    if not exist "!CLASSPATH_JAR!" (
        echo ERROR: Could not find quarkus-run.jar at %CLASSPATH_JAR%
        echo Please specify the correct path using --classpath-jar parameter
        exit /b 1
    )
)

set CLASSPATH_JAR=%CLASSPATH_JAR:"=%
echo Found JAR: %CLASSPATH_JAR%

REM Ensure log directory exists
set "LOG_PATH=%KEYCLOAK_HOME%\log"
if not exist "%LOG_PATH%" (
    mkdir "%LOG_PATH%"
)

REM Set default JVM options
set "DEFAULT_JVM_OPTS=-Djava.awt.headless=true"
set "DEFAULT_JVM_OPTS=%DEFAULT_JVM_OPTS%;-Dkc.home.dir=%KEYCLOAK_HOME%"


if defined JVM_ARGS (
    set JVM_ARGS=%DEFAULT_JVM_OPTS%;%JVM_ARGS%
) else (
    set JVM_ARGS=%DEFAULT_JVM_OPTS%
)

REM Prepare Keycloak arguments - default to start-dev
if not defined KC_OPTS (
    set KC_OPTS=start-dev
)

echo Installing Keycloak as a Windows service '%SERVICE_NAME%'...

set SRV_CMD=%PRUNSRV% install %SERVICE_NAME%
set SRV_CMD=%SRV_CMD% --DisplayName="%SERVICE_DISPLAY_NAME%"
set SRV_CMD=%SRV_CMD% --Description="%SERVICE_DESCRIPTION%"
set SRV_CMD=%SRV_CMD% --Startup=%STARTUP_MODE%
set SRV_CMD=%SRV_CMD% --Jvm="%JVM%"
set SRV_CMD=%SRV_CMD% --JvmOptions="%JVM_ARGS%"
set SRV_CMD=%SRV_CMD% --StartPath="%KEYCLOAK_HOME%"
set SRV_CMD=%SRV_CMD% --StartMode=jvm
set SRV_CMD=%SRV_CMD% --StartClass=io.quarkus.bootstrap.runner.QuarkusEntryPoint
set SRV_CMD=%SRV_CMD% --StartMethod=main
set SRV_CMD=%SRV_CMD% --StartParams="%KC_OPTS%"
set SRV_CMD=%SRV_CMD% --StopMode=jvm
set SRV_CMD=%SRV_CMD% --StopClass=io.quarkus.bootstrap.runner.QuarkusEntryPoint
set SRV_CMD=%SRV_CMD% --StopMethod=main
set SRV_CMD=%SRV_CMD% --StopTimeout=30
set SRV_CMD=%SRV_CMD% --LogPath="%LOG_PATH%"
set SRV_CMD=%SRV_CMD% --LogLevel=Info
set SRV_CMD=%SRV_CMD% --StdOutput=auto
set SRV_CMD=%SRV_CMD% --StdError=auto
set SRV_CMD=%SRV_CMD% --Classpath="%CLASSPATH_JAR%"

REM Configure service account - explicitly set Local System if no user specified
if defined SERVICE_USER (
    echo Configuring service to run as user: %SERVICE_USER%
    set SRV_CMD=%SRV_CMD% --ServiceUser="%SERVICE_USER%"
    if defined SERVICE_PASSWORD (
        set SRV_CMD=%SRV_CMD% --ServicePassword="%SERVICE_PASSWORD%"
    )
) else (
    echo Configuring service to run as Local System account
    REM Explicitly set to Local System account to avoid defaulting to Local Service
    set SRV_CMD=%SRV_CMD% --ServiceUser="LocalSystem"
)

%SRV_CMD%
set INSTALL_STATUS=%ERRORLEVEL%

if %INSTALL_STATUS% EQU 0 (
    echo Service '%SERVICE_NAME%' installed successfully.
    if not defined SERVICE_USER (
        echo Service is configured to run as Local System account.
    )
    echo.
    echo To start the service, run as Administrator:
    echo    net start %SERVICE_NAME%
) else (
    echo Failed to install service '%SERVICE_NAME%'. Error code: %INSTALL_STATUS%
)
exit /b %INSTALL_STATUS%
