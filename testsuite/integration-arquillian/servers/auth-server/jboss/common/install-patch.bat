set NOPAUSE=true

call %JBOSS_HOME%\bin\jboss-cli.bat --command="patch apply %PATCH_ZIP%"

if %ERRORLEVEL% neq 0 set ERROR=%ERRORLEVEL%
exit /b %ERROR%

