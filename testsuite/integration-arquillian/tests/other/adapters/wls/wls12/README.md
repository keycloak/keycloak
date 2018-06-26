For running WebLogic tests you need to have WLS running on port 8280 on your local machine.

## Running WLS server 

Run WebLogic server on port **8280**

For example for docker image it is necessary to run it with these arguments
```bash
docker run -d \
--name="wls-server" \
--net="host" \
-v /tmp:/tmp \
your_docker_image
```

- We need to use --net="host" so that weblogic can access Keycloak server
- Also we need to map /tmp directory to /tmp directory in docker. This way arquillian will move archives used in testsuite to docker filesystem so that they are deployed to WLS

```
- And also our image always create new admin password when starting weblogic so you need to find out what password it generated
```bash
docker logs wls-server | grep password
```
## Running tests

1. At first we need to add our custom arquillian remote adapter to local repository. Only custom change is to always store tmp files in /tmp
```bash
git clone https://github.com/mhajas/arquillian-container-wls.git
cd arquillian-container-wls/wls-common
mvn clean install -DskipTests [-Dmaven.repo.local=/custom/repo/path]
cd ../wls-remote-12.1.x
mvn clean install -DskipTests [-Dmaven.repo.local=/custom/repo/path]
```

2. Build testsuite-arquillian
```bash
mvn clean install -f testsuite/integration-arquillian/pom.xml -DskipTests=true
```
3. Run tests
```bash
mvn clean install -f testsuite/integration-arquillian/tests/other/pom.xml -Papp-server-wls -Dwl.username=${admin-username} -Dwl.password=${admin-password} -Dwl.home=${wl-home-path}
```

In case of docker image one can replace wl-home-path with some preprepared directory which contains these files (example of downloading files):
```bash
docker cp wls-server:/u01/oracle/wlserver/server/lib/weblogic.jar ${wl-home-path}/server/lib/
docker cp wls-server:/u01/oracle/wlserver/server/lib/wlclient.jar ${wl-home-path}/server/lib/
docker cp wls-server:/u01/oracle/wlserver/server/lib/wljmxclient.jar ${wl-home-path}/server/lib/
```

