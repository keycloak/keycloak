FIPS 140-2 Integration
======================

Environment
-----------
All the steps below were tested on RHEL 8.6 with FIPS mode enabled (See [this page](https://access.redhat.com/documentation/en-us/red_hat_enterprise_linux/8/html/security_hardening/assembly_installing-a-rhel-8-system-with-fips-mode-enabled_security-hardening#doc-wrapper)
for the details) and with OpenJDK 17.0.5 on that host.

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
export BCTLSFIPS_VERSION=1.0.14
export BCPKIXFIPS_VERSION=1.0.7
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
export KEYSTORE_FILE=keycloak-server.p12
#export KEYSTORE_FILE=keycloak-server.bcfks
export KEYCLOAK_SOURCES=$HOME/IdeaProjects/keycloak

export KEYSTORE_FORMAT=$(echo $KEYSTORE_FILE | cut -d. -f2)
if [ "$KEYSTORE_FORMAT" == "p12" ]; then
    export KEYSTORE_FORMAT=pkcs12;
fi;

# Removing old keystore file to start from fresh
rm keycloak-server.p12
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
  -J-Djava.security.properties=$KEYCLOAK_SOURCES/testsuite/integration-arquillian/servers/auth-server/common/fips/kc.keystore-create.java.security
```

3) Run "build" to re-augment with `enabled` fips mode and start the server. This will run the server with BCFIPS in non-approved mode

```
./kc.sh start --fips-mode=enabled --hostname=localhost \
  --https-key-store-file=$PWD/$KEYSTORE_FILE \
  --https-key-store-type=$KEYSTORE_FORMAT \
  --https-key-store-password=passwordpassword \
  --log-level=INFO,org.keycloak.common.crypto:TRACE,org.keycloak.crypto:TRACE
```

4) For the `fips-mode` option, the more secure alternative is to use `--fips-mode=strict` in which case BouncyCastle FIPS will use "approved mode",
which means even stricter security requirements on cryptography and security algorithms. Few more points:
- As mentioned above, strict mode won't work with `pkcs12` keystore. So it is needed to use other keystore (probably `bcfks`).
- User passwords must be 14 characters or longer. Keycloak uses PBKDF2 based password encoding by default. BCFIPS approved mode requires passwords to be at least 112 bits 
- (effectively 14 characters). If you want to allow shorter password, you need to set property `max-padding-length` of
  provider `pbkdf2-sha256` of SPI `password-hashing` to value 14, so there will be some additional padding used when verifying hash created by this algorithm.
  This is also backwards compatible with previously stored passwords (if you had your user's DB in non-FIPS environment and you have shorter passwords and you
  want to verify them now with Keycloak using BCFIPS in approved mode, it should work fine). So effectively, you can use option like this when starting the server:
```
--spi-password-hashing-pbkdf2-sha256-max-padding-length=14
  ```
- RSA keys of 1024 bits don't work (2048 is the minimum)
- Also `jks` and `pkcs12` keystores/trustores are not supported.

When starting server at startup, you can check that startup log contains `KC` provider contains KC provider with the note about `Approved Mode` like this:
```
KC(BCFIPS version 1.000203 Approved Mode) version 1.0 - class org.keycloak.crypto.fips.KeycloakFipsSecurityProvider,
```

Other considerations
--------------------
#### SAML and Kerberos
In order to have SAML working, there is a need to have `XMLDSig` security provider to be available in your `JAVA_HOME/conf/security/java.security`.
In order to have Kerberos working, there is a need to have `SunJGSS` security provider available. In FIPS enabled RHEL 8.6 in OpenJDK 17.0.5, these
security providers are not by default in the `java.security`, which means that they effectively cannot work.

To have SAML working, you can manually add the provider into `java.security` into the list fips providers. For example add the line like:
```
fips.provider.7=XMLDSig
```
Adding this security provider should be fine as in fact it is FIPS compliant and likely will be added by default in the future OpenJDK micro version.
Details: https://bugzilla.redhat.com/show_bug.cgi?id=1940064

For Kerberos, there are few more things to be done to have security provider FIPS compliant. Hence it is not recommended to add security provider
if you want to be FIPS compliant. The `KERBEROS` feature is disabled by default in Keycloak when it is executed on this platform and when security provider is not
available. Details: https://bugzilla.redhat.com/show_bug.cgi?id=2051628

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
See the FIPS section in the [HOW-TO-RUN.md](../testsuite/integration-arquillian/HOW-TO-RUN.md)
