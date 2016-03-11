set NOPAUSE=true

start /b cmd /c %JBOSS_HOME%\bin\standalone.bat

timeout /t 10

set ERROR=0

call %JBOSS_HOME%\bin\jboss-cli.bat -c --command=":read-attribute(name=server-state)" | findstr "running"
if %ERRORLEVEL% neq 0 (
    set ERROR=%ERRORLEVEL%
) else (
    call %JBOSS_HOME%\bin\jboss-cli.bat -c --file="%JBOSS_HOME%\bin\adapter-install.cli"
    if %ERRORLEVEL% neq 0 set ERROR=%ERRORLEVEL%
    if "%SAML_SUPPORTED" == "true" (
        call %JBOSS_HOME%\bin\jboss-cli.bat -c --file="%JBOSS_HOME%\bin\adapter-install-saml.cli"
        if %ERRORLEVEL% neq 0 set ERROR=%ERRORLEVEL%
    )
)
call %JBOSS_HOME%\bin\jboss-cli.bat -c --command=":shutdown"

if %ERROR% neq 0 exit /b %ERROR%

goto:eof
