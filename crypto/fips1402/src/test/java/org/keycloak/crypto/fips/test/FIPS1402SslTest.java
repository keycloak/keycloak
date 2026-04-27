package org.keycloak.crypto.fips.test;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManagerFactory;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.Environment;
import org.keycloak.rule.CryptoInitRule;

import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FIPS1402SslTest {

    protected static final Logger logger = Logger.getLogger(FIPS1402SslTest.class);

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();


    @BeforeClass
    public static void dumpSecurityProviders() {
        logger.info(CryptoIntegration.dumpJavaSecurityProviders());
        logger.info(CryptoIntegration.dumpSecurityProperties());
    }

    @Before
    public void before() {
        // Run this test just if java is in FIPS mode
        Assume.assumeTrue("Java is not in FIPS mode. Skipping the test.", Environment.isJavaInFipsMode());
    }

    @Test
    public void testPkcs12KeyStoreWithPKIXKeyMgrFactory() throws Exception {
        // PKCS12 keystore works just in non-approved mode
        Assume.assumeFalse(CryptoServicesRegistrar.isInApprovedOnlyMode());
        String type = "PKCS12";
        String password = "passwordpassword";

        KeyStore keystore = loadKeystore(type, password);
        String keyMgrDefaultAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
        KeyManagerFactory keyMgrFact = getKeyMgrFactory(password, keystore, keyMgrDefaultAlgorithm);
        testSSLContext(keyMgrFact);
    }

    // This works with BCFIPS, but requires addition of security provider "com.sun.net.ssl.internal.ssl.Provider BCFIPS" to Java Security providers
    @Test
    @Ignore("Skip for now and keep it just for the reference. We can check if we want to test this path with SunX509 algorithm withadditional security provider")
    public void testPkcs12KeyStoreWithSunX509KeyMgrFactory() throws Exception {
        // PKCS12 keystore works just in non-approved mode
        Assume.assumeFalse(CryptoServicesRegistrar.isInApprovedOnlyMode());
        String type = "PKCS12";
        String password = "passwordpassword";

        KeyStore keystore = loadKeystore(type, password);
        String keyMgrDefaultAlgorithm = "SunX509";
        KeyManagerFactory keyMgrFact = getKeyMgrFactory(password, keystore, keyMgrDefaultAlgorithm);
        testSSLContext(keyMgrFact);
    }

    @Test
    public void testBcfksKeyStoreWithPKIXKeyMgrFactory() throws Exception {
        String type = "BCFKS";
        String password = "passwordpassword";

        KeyStore keystore = loadKeystore(type, password);
        String keyMgrDefaultAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
        KeyManagerFactory keyMgrFact = getKeyMgrFactory(password, keystore, keyMgrDefaultAlgorithm);
        testSSLContext(keyMgrFact);
    }

    // This works with BCFIPS, but requires addition of security provider "com.sun.net.ssl.internal.ssl.Provider BCFIPS" to Java Security providers
    @Test
    @Ignore("Skip for now and keep it just for the reference. We can check if we want to test this path with SunX509 algorithm withadditional security provider")
    public void testBcfksKeyStoreWithSunX509KeyMgrFactory() throws Exception {
        String type = "BCFKS";
        String password = "passwordpassword";

        KeyStore keystore = loadKeystore(type, password);
        String keyMgrDefaultAlgorithm = "SunX509";
        KeyManagerFactory keyMgrFact = getKeyMgrFactory(password, keystore, keyMgrDefaultAlgorithm);
        testSSLContext(keyMgrFact);
    }

    @Test
    public void testDefaultTruststore() throws Exception {
        String defaultAlg = TrustManagerFactory.getDefaultAlgorithm();
        logger.infof("Default trust manager factory algorithm: %s", defaultAlg);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(defaultAlg);

        // This may fail if default truststore is "pkcs12" and security property "keystore.type.compat" is set to false
        trustManagerFactory.init((KeyStore) null);
    }

    private KeyStore loadKeystore(String type, String password) throws Exception {
        KeyStore keystore = KeyStore.getInstance(type);
        InputStream in = FIPS1402SslTest.class.getClassLoader().getResourceAsStream("bcfips-keystore." + type.toLowerCase());
        keystore.load(in, password != null ? password.toCharArray() : null);
        logger.infof("Keystore loaded successfully. Type: %s, provider: %s", keystore.getProvider().getName());
        return keystore;
    }

    private KeyManagerFactory getKeyMgrFactory(String password, KeyStore keystore, String keyMgrAlgorithm) throws Exception {
        KeyManagerFactory keyMgrFact = KeyManagerFactory.getInstance(keyMgrAlgorithm);
        char[] keyPassword = password.toCharArray();
        keyMgrFact.init(keystore, keyPassword);
        logger.infof("KeyManagerFactory loaded for algorithm: %s", keyMgrAlgorithm);
        return keyMgrFact;
    }


    private void testSSLContext(KeyManagerFactory keyMgrFact) throws Exception {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyMgrFact.getKeyManagers(), null, null);
        SSLEngine engine = context.createSSLEngine();

        List<String> enabledCipherSuites = Arrays.asList(engine.getEnabledCipherSuites());
        List<String> supportedProtocols = Arrays.asList(context.getDefaultSSLParameters().getProtocols());
        List<String> supportedCiphers = Arrays.asList(engine.getSupportedCipherSuites());

        logger.infof("SSLContext provider: %s, SSLContext class: %s", context.getProvider().getName(), context.getClass().getName());
        logger.infof("Enabled ciphersuites: %s", enabledCipherSuites.size());
        logger.infof("Supported protocols: %s", supportedProtocols);
        logger.infof("Supported ciphers size: %d", supportedCiphers.size());
        assertThat(enabledCipherSuites.size(), greaterThan(0));
        assertThat(supportedProtocols.size(), greaterThan(0));
        assertThat(supportedCiphers.size(), greaterThan(0));

        SSLSessionContext sslServerCtx = context.getServerSessionContext();
        Assert.assertNotNull(sslServerCtx);
    }
}
