set NOPAUSE=true

start "JBoss Server" /b cmd /c %JBOSS_HOME%\bin\standalone.bat --server-config=standalone-ha.xml

set ERROR=0
set TIMEOUT=10
set I=0

ping 127.0.0.1 -n 3 > nul


:wait_for_jboss
call %JBOSS_HOME%\bin\jboss-cli.bat -c --command=":read-attribute(name=server-state)" | findstr "running"
if %ERRORLEVEL% equ 0 goto install_adapters
ping 127.0.0.1 -n 1 > nul
set /a I=%I%+1
if %I% gtr %TIMEOUT% (
    set ERROR=1
    goto shutdown_jboss
)
goto wait_for_jboss


:install_adapters
call %JBOSS_HOME%\bin\jboss-cli.bat -c --file="%JBOSS_HOME%\bin\adapter-install.cli"
set ERROR=%ERRORLEVEL%
echo Installation of OIDC adapter ended with error code: "%ERROR%"
if %ERROR% neq 0 (
    goto shutdown_jboss
)

call %JBOSS_HOME%\bin\jboss-cli.bat -c --file="%JBOSS_HOME%\bin\adapter-install-saml.cli"
set ERROR=%ERRORLEVEL%
echo Installation of SAML adapter ended with error code: "%ERROR%"
if %ERROR% neq 0 (
    goto shutdown_jboss
)


call %JBOSS_HOME%\bin\jboss-cli.bat -c --file="%CLI_PATH%\add-adapter-log-level.cli"
set ERROR=%ERRORLEVEL%
echo Adding log level for adapters ended with error code: "%ERROR%"
if %ERROR% neq 0 (
    goto shutdown_jboss
)

call %JBOSS_HOME%\bin\jboss-cli.bat -c --file="%CLI_PATH%\configure-cluster-config.cli"
set ERROR=%ERRORLEVEL%
echo Configuring cluster config ended with error code: "%ERROR%"
if %ERROR% neq 0 (
    goto shutdown_jboss
)

:shutdown_jboss
echo Shutting down with error code: "%ERROR%"
call %JBOSS_HOME%\bin\jboss-cli.bat -c --command=":shutdown"
exit /b %ERROR%
