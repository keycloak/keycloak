set NOPAUSE=true

start "JBoss Server" /b cmd /c %JBOSS_HOME%\bin\standalone.bat

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
if %ERRORLEVEL% neq 0 set ERROR=%ERRORLEVEL%
if "%SAML_SUPPORTED%" == "true" (
    call %JBOSS_HOME%\bin\jboss-cli.bat -c --file="%JBOSS_HOME%\bin\adapter-install-saml.cli"
    if %ERRORLEVEL% neq 0 set ERROR=%ERRORLEVEL%
)


:shutdown_jboss
call %JBOSS_HOME%\bin\jboss-cli.bat -c --command=":shutdown"
exit /b %ERROR%
