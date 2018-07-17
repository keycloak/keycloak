set NOPAUSE=true

cd %JBOSS_HOME%
start javaw -jar %FUSE_INSTALLER_NAME%
ping 127.0.0.1 -n 40 > nul
del %FUSE_INSTALLER_NAME%

set JBOSS_HOME=%JBOSS_HOME:/=\%
ren %JBOSS_HOME%\standalone\deployments\hawtio*.war hawtio.war

exit 0