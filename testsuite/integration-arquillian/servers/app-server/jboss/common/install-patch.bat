set NOPAUSE=true

for %%a in ("%APP_PATCH_ZIPS:,=" "%") do (
  call %JBOSS_HOME%\bin\jboss-cli.bat --command="patch apply %%~a"

  if %ERRORLEVEL% neq 0 set ERROR=%ERRORLEVEL%
)

exit /b %ERROR%