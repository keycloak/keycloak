@echo off
if not DEFINED JBOSS_HOME (
    exit /b
)

setlocal
set KEYCLOAK_HOME=%CD%\..\..\..
mkdir %JBOSS_HOME%\standalone\deployments\auth-server.war
echo>>%JBOSS_HOME%\standalone\deployments\auth-server.war.dodeploy

set CURDIR=%CD%
cd %JBOSS_HOME%\standalone\deployments\auth-server.war
mklink /D admin-ui %KEYCLOAK_HOME%\admin-ui-styles\src\main\resources\META-INF\resources\admin-ui
mklink /D admin %KEYCLOAK_HOME%\admin-ui\src\main\resources\META-INF\resources\admin
mklink /D WEB-INF %CURDIR%\target\auth-server\WEB-INF
cd %CURDIR%




