

# Almighty-Keycloak Docker Image

To build this image is neccessary to previously generate the executables of this
project.

`$ mvn clean install -DskipTests=true -pl :keycloak-server-dist -am -P distribution`

This generates some tarballs with the required executables. In our case, we just
need to copy the generated tarball from `server-dist` e.g. `keycloak-3.0.0.CR1-SNAPSHOT.tar.gz`.
You can find this tarball in `./distribution/server-dist/target/` directory.

`$ cp ../distribution/server-dist/target/keycloak-3.0.0.CR1-SNAPSHOT.tar.gz .`

Then you just need to build the docker image:

`$ docker build -t IMAGE_NAME .`


Note that, this docker image installs the certificate to securely talk to OpenShift Online.
This step is done inside the `install_certificate.sh` script which adds this
certificate into the Java system keystore at building time. We assume this certificate
points to `tsrv.devshift.net`. So any change on that direction should trigger a
change in the docker image.

In the content of the Dockerfile, you could find this ENV variables:
```
ENV OSO_ADDRESS tsrv.devshift.net:8443
ENV OSO_DOMAIN_NAME tsrv.devshift.net
```
