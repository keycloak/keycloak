@echo off

if "%OS%" == "Windows_NT" (
  set "DIRNAME=%~dp0%"
) else (
  set DIRNAME=.\
)

java -Dkeycloak.theme.dir=%DIRNAME%\..\themes -cp "%DIRNAME%\..\providers\*;%DIRNAME%\..\lib\keycloak-runner.jar" io.quarkus.runner.GeneratedMain %*