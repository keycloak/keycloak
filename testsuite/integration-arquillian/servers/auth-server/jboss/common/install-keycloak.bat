set NOPAUSE=true

call %JBOSS_HOME%\bin\jboss-cli.bat --file=keycloak-install.cli
call %JBOSS_HOME%\bin\jboss-cli.bat --file=keycloak-install-ha.cli

if %ERRORLEVEL% neq 0 set ERROR=%ERRORLEVEL%

exit /b %ERROR%

