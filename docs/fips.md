FIPS 140-2 Integration
======================

Build with FIPS
---------------

With OpenJDK 11 on the classpath, run this from the project root directory:

```
mvn clean install -DskipTests=true -Dfips140-2 -Pquarkus
```
The property `fips140-2` is used to trigger maven profile to build keycloak+quarkus distribution with `bouncycastle-fips` dependencies instead of plain `bouncycastle`
and also with `keycloak-crypto-fips1402` module containing some security code dependent on bouncycastle-fips APIs.

Note, that if you ommit the `fips140-2` property from the command above, then the quarkus distribution will be built
with the plain non-fips bouncycastle dependencies and with `keycloak-crypto-default` module.

Then unzip and check only bouncycastle-fips libraries are inside "lib" directory:
```
tar xf $KEYCLOAK_SOURCES/quarkus/dist/target/keycloak-999-SNAPSHOT.tar.gz
ls keycloak-999-SNAPSHOT/lib/lib/main/org.bouncycastle.bc*
```
Output should be something like:
```
keycloak-999-SNAPSHOT/lib/lib/main/org.bouncycastle.bc-fips-1.0.2.jar      keycloak-999-SNAPSHOT/lib/lib/main/org.bouncycastle.bctls-fips-1.0.11.jar
keycloak-999-SNAPSHOT/lib/lib/main/org.bouncycastle.bcpkix-fips-1.0.3.jar
```

Similarly the JAR keycloak-fips-integration should be available:
```
ls keycloak-999-SNAPSHOT/lib/lib/main/org.keycloak.keycloak-fips-integration-999-SNAPSHOT.jar
```

Now run the server on the FIPS enabled machine with FIPS-enabled OpenJDK (Tested on RHEL 8.6):
```
cd keycloak-999-SNAPSHOT/bin
./kc.sh start-dev
```

NOTE: Right now, server should start, and I am able to create admin user on `http://localhost:8080`, but I am not able to finish
login to the admin console. However the Keycloak uses bouncycastle-fips libraries and the `CryptoIntegration` uses `FIPS1402Provider`. More fixes are required to have Keycloak server working...

Run the tests in the FIPS environment
-------------------------------------
This instruction is about running automated tests on the FIPS enabled RHEL 8.6 system with the FIPS enabled OpenJDK 11.

So far only the unit tests inside the `crypto` module are supported. More effort is needed to have whole testsuite passing.

First it is needed to build the project (See above). Then run the tests in the `crypto` module.
```
mvn clean install -f crypto
```

The tests should work also with the BouncyCastle approved mode, which is more strict in the used crypto algorithms
```
mvn clean install -f crypto -Dorg.bouncycastle.fips.approved_only=true
```
