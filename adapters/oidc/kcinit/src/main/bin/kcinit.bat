@echo off

if "%OS%" == "Windows_NT" (
  set "DIRNAME=%~dp0%"
) else (
  set DIRNAME=.\
)
java -jar %DIRNAME%\kcinit-${project.version}.jar %*
