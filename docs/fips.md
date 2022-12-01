FIPS 140-2 Integration
======================

Run the server with FIPS
------------------------

To run Keycloak quarkus distribution, on the FIPS enabled host and FIPS enabled OpenJDK, you need to:
- Make sure that Keycloak will use the BouncyCastle FIPS dependencies instead of the normal BouncyCastle dependencies
- Make sure to start the server with the FIPS mode.

1) Copy BCFIPS dependencies to your Keycloak distribution. 
You can either download them from BouncyCastle page and add it manually to the directory `KEYCLOAK_HOME/providers`(make sure to
use proper versions compatible with BouncyCastle Keycloak dependencies).

Or you can use for example commands like this to copy the appropriate BCFIPS jars to the Keycloak distribution. Again, replace
the BCFIPS versions with the appropriate versions from pom.xml. Assumption is that you have already these BCFIPS in
your local maven repository, which can be achieved for example by building `crypto/fips1402` module (See the section for
running the unit tests below):

```
cd $KEYCLOAK_HOME/bin
export MAVEN_REPO_HOME=$HOME/.m2/repository
export BCFIPS_VERSION=1.0.2.3
export BCTLSFIPS_VERSION=1.0.12.2
export BCPKIXFIPS_VERSION=1.0.5
cp $MAVEN_REPO_HOME/org/bouncycastle/bc-fips/$BCFIPS_VERSION/bc-fips-$BCFIPS_VERSION.jar ../providers/
cp $MAVEN_REPO_HOME/org/bouncycastle/bctls-fips/$BCTLSFIPS_VERSION/bctls-fips-$BCTLSFIPS_VERSION.jar ../providers/
cp $MAVEN_REPO_HOME/org/bouncycastle/bcpkix-fips/$BCPKIXFIPS_VERSION/bcpkix-fips-$BCPKIXFIPS_VERSION.jar ../providers/
```

2) Now create either pkcs12 or bcfks keystore. The pkcs12 works just in BCFIPS non-approved mode.

Please choose either `bcfips` or `pkcs12` and use the appropriate value of `KEYSTORE_FILE` variable according to your choice:

Also make sure to set `KEYCLOAK_SOURCES` to the location with your Keycloak codebase.

Note that for keystore generation, it is needed to use the BouncyCastle FIPS libraries and use custom security file, which
will remove default SUN and SunPKCS11 providers as it doesn't work to create keystore with them on FIPS enabled OpenJDK11 due
the limitation described here https://access.redhat.com/solutions/6954451 and in the related bugzilla https://bugzilla.redhat.com/show_bug.cgi?id=2048582.
```
export KEYSTORE_FILE=keycloak-server.pkcs12
#export KEYSTORE_FILE=keycloak-server.bcfks
export KEYCLOAK_SOURCES=$HOME/IdeaProjects/keycloak

export KEYSTORE_FORMAT=$(echo $KEYSTORE_FILE | cut -d. -f2)

# Removing old keystore file to start from fresh
rm keycloak-server.pkcs12
rm keycloak-server.bcfks

keytool -keystore $KEYSTORE_FILE \
  -storetype $KEYSTORE_FORMAT \
  -providername BCFIPS \
  -providerclass org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider \
  -provider org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider \
  -providerpath $MAVEN_REPO_HOME/org/bouncycastle/bc-fips/1.0.2.3/bc-fips-1.0.2.3.jar \
  -alias localhost \
  -genkeypair -sigalg SHA512withRSA -keyalg RSA -storepass passwordpassword \
  -dname CN=localhost -keypass passwordpassword \
  -J-Djava.security.properties=$KEYCLOAK_SOURCES/crypto/fips1402/src/test/resources/kc.java.security
```

3) Run "build" to re-augment with `enabled` fips mode and start the server.

For the `fips-mode`, he alternative is to use `--fips-mode=strict` in which case BouncyCastle FIPS will use "approved mode",
which means even stricter security algorithms. As mentioned above, strict mode won't work with `pkcs12` keystore:

```
./kc.sh build --fips-mode=enabled
./kc.sh start --optimized --hostname=localhost \
  --https-key-store-file=$PWD/$KEYSTORE_FILE \
  --https-key-store-type=$KEYSTORE_FORMAT \
  --https-key-store-password=passwordpassword \
  --log-level=INFO,org.keycloak.common.crypto:TRACE,org.keycloak.crypto:TRACE
```

4) The approach above will run the Keycloak JVM with all the default java security providers and will add also
BouncyCastle FIPS security providers on top of that in runtime. This works fine, however it may not be guaranteed that
all the crypto algorithms are used in the FIPS compliant way as the default providers like "Sun" potentially allow non-FIPS
usage in the Java. Some more details here: https://access.redhat.com/documentation/en-us/openjdk/11/html-single/configuring_openjdk_11_on_rhel_with_fips/index#ref_openjdk-default-fips-configuration_openjdk

To ensure that Java strictly allows to use only FIPS-compliant crypto, it can be good to rely solely just on the BCFIPS.
This is possible by using custom java security file, which adds just the BouncyCastle FIPS security providers. This requires
BouncyCastle FIPS dependencies to be available in the bootstrap classpath instead of adding them in runtime.

So for this approach, it is needed to move the BCFIPS jars from the `providers` directory to bootstrap classpath.
```
mkdir ../lib/bootstrap
mv ../providers/bc*.jar ../lib/bootstrap/
```
Then run `build` and `start` commands as above, but with additional property for the alternative security file like
```
-Djava.security.properties=$KEYCLOAK_SOURCES/crypto/fips1402/src/test/resources/kc.java.security
```
At the server startup, you should see the message like this in the log and you can check if correct providers are present and not any others:
```
2022-10-10 08:23:07,097 TRACE [org.keycloak.common.crypto.CryptoIntegration] (main) Java security providers: [
 KC(BCFIPS version 1.000203) version 1.0 - class org.keycloak.crypto.fips.KeycloakFipsSecurityProvider,
 BCFIPS version 1.000203 - class org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider,
 BCJSSE version 1.001202 - class org.bouncycastle.jsse.provider.BouncyCastleJsseProvider,
]
```

NOTE: If you want to use BouncyCastle approved mode, then it is recommended to change/add these properties into the `kc.java.security`
file:
```
keystore.type=BCFKS
fips.keystore.type=BCFKS
org.bouncycastle.fips.approved_only=true
```
and then check that startup log contains `KC` provider contains KC provider with the note about `Approved Mode` like this:
```
KC(BCFIPS version 1.000203 Approved Mode) version 1.0 - class org.keycloak.crypto.fips.KeycloakFipsSecurityProvider,
```
Note that in approved mode, there are few limitations at the moment like for example:
- User passwords must be at least 14 characters long
- Keystore/truststore must be of type bcfks due the both of `jks` and `pkcs12` don't work
- Some warnings in the server.log at startup

Run the CLI on the FIPS host
----------------------------
In case you want to run Client Registration CLI (`kcreg.sh/bat` script) or Admin CLI (`kcadm.sh/bat` script), it is needed
that CLI will also use the BouncyCastle FIPS dependencies instead of plain BouncyCastle dependencies. To achieve this, you may copy the
jars to the CLI library folder and that is enough. CLI tool will automatically use BCFIPS dependencies instead of plain BC when
it detects that corresponding BCFIPS jars are present (see above for the versions used):
```
cp $MAVEN_REPO_HOME/org/bouncycastle/bc-fips/$BCFIPS_VERSION/bc-fips-$BCFIPS_VERSION.jar ../bin/client/lib/
cp $MAVEN_REPO_HOME/org/bouncycastle/bctls-fips/$BCTLSFIPS_VERSION/bctls-fips-$BCTLSFIPS_VERSION.jar ../bin/client/lib/
```

Run the unit tests in the FIPS environment
------------------------------------------
This instruction is about running automated tests on the FIPS enabled RHEL 8.6 system with the FIPS enabled OpenJDK 11.

So far only the unit tests inside the `crypto` module are supported. More effort is needed to have whole testsuite passing.

First it is needed to build the project (See above). Then run the tests in the `crypto` module.
```
mvn clean install -f common -DskipTests=true
mvn clean install -f core -DskipTests=true
mvn clean install -f server-spi -DskipTests=true
mvn clean install -f server-spi-private -DskipTests=true
mvn clean install -f services -DskipTests=true
mvn clean install -f crypto/fips1402
```

The tests should work also with the BouncyCastle approved mode, which is more strict in the used crypto algorithms
```
mvn clean install -f crypto/fips1402 -Dorg.bouncycastle.fips.approved_only=true
```

Run the integration tests in the FIPS environment
-------------------------------------------------
See the FIPS section in the [MySQL docker image](../testsuite/integration-arquillian/HOW-TO-RUN.md)

