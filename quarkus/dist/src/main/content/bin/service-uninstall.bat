@echo off
setlocal enabledelayedexpansion

set "DIRNAME=%~dp0%"
if "%DIRNAME%" == "" set "DIRNAME=."
cd "%DIRNAME%\.."

set "KEYCLOAK_HOME=%cd%"
set SERVICE_NAME=keycloak

REM Parse command line parameters
:PARAM_LOOP
if "%~1" == "" goto PARAM_DONE
if /I "%~1" == "--name" (
    set "SERVICE_NAME=%~2"
    shift
    shift
    goto PARAM_LOOP
)
shift
goto PARAM_LOOP
:PARAM_DONE

REM Check if prunsrv is available
if exist "%KEYCLOAK_HOME%\bin\prunsrv.exe" goto PROCRUN_SCRIPTS

echo ERROR: Apache Commons Daemon (Procrun) executable not found. Please ensure prunsrv.exe is available.
echo Download from https://downloads.apache.org/commons/daemon/binaries/windows/ and place it in:
echo - %KEYCLOAK_HOME%\bin\
exit /b 1

:PROCRUN_SCRIPTS
set PRUNSRV="%KEYCLOAK_HOME%\bin\prunsrv.exe"
goto UNINSTALL_SERVICE

:UNINSTALL_SERVICE
echo Uninstalling Keycloak service '%SERVICE_NAME%'...

%PRUNSRV% delete %SERVICE_NAME%

IF %ERRORLEVEL% EQU 0 (
    echo Service '%SERVICE_NAME%' uninstalled successfully.
) ELSE (
    echo Failed to uninstall service '%SERVICE_NAME%'. Error code: %ERRORLEVEL%
)

exit /b %ERRORLEVEL%
