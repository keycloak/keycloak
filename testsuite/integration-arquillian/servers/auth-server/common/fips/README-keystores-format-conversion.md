How to convert keystores and truststores
----------------------------------------
Magic command to import PKCS12 keystore to BCFKS

```
keytool -importkeystore -srckeystore keycloak-fips.keystore.pkcs12 -destkeystore keycloak-fips.keystore.bcfks \
    -srcstoretype PKCS12 -deststoretype BCFKS -deststorepass passwordpassword \
    -providername BCFIPS \
    -providerclass org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider \
    -provider org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider \
    -providerpath $MAVEN_REPO_HOME/org/bouncycastle/bc-fips/1.0.2.5/bc-fips-1.0.2.5.jar \
    -J-Djava.security.properties=$KEYCLOAK_SOURCES/testsuite/integration-arquillian/servers/auth-server/common/fips/kc.keystore-create.java.security
```
Default password is `passwordpassword`.

When converting from `JKS` to `PKCS12` on non-FIPS host, only first 2 lines from this command are needed (no need to use BCFIPS provider).
Original JKS keystore, which was used to create `PKCS12` (and transitively also `BCFKS`) keystore is [keycloak.jks](../keystore/keycloak.jks).
Original JKS truststore is [keycloak.truststore](../keystore/keycloak.truststore).