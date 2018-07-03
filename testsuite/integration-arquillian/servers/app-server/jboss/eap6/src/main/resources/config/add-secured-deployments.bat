set NOPAUSE=true

start "JBoss Server" /b cmd /c %JBOSS_HOME%\bin\standalone.bat -Djboss.server.config.dir=%JBOSS_HOME%\standalone-secured-deployments\configuration

set ERROR=0
set TIMEOUT=10
set I=0

ping 127.0.0.1 -n 3 > nul


:wait_for_jboss
call %JBOSS_HOME%\bin\jboss-cli.bat -c --command=":read-attribute(name=server-state)" | findstr "running"
if %ERRORLEVEL% equ 0 goto add_secured_deployments
ping 127.0.0.1 -n 1 > nul
set /a I=%I%+1
if %I% gtr %TIMEOUT% (
    set ERROR=1
    goto shutdown_jboss
)
goto wait_for_jboss


:add_secured_deployments
call %JBOSS_HOME%\bin\jboss-cli.bat -c --file="%CLI_PATH%\add-secured-deployments.cli"
set ERROR=%ERRORLEVEL%
echo Error code: "%ERROR%"
if %ERROR% neq 0 (
    goto shutdown_jboss
)

:shutdown_jboss
echo Shutting down with error code: "%ERROR%"
call %JBOSS_HOME%\bin\jboss-cli.bat -c --command=":shutdown"
exit /b %ERROR%
