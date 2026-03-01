package org.keycloak.crypto.fips.test;

import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.Environment;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.rule.CryptoInitRule;

import org.bouncycastle.crypto.CryptoServicesRegistrar;
import org.hamcrest.Matchers;
import org.junit.Assume;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FIPS1402KeystoreTypesTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Before
    public void before() {
        // Run this test just if java is in FIPS mode
        Assume.assumeTrue("Java is not in FIPS mode. Skipping the test.", Environment.isJavaInFipsMode());
    }

    @Test
    public void testKeystoreFormatsInNonApprovedMode() {
        Assume.assumeFalse(CryptoServicesRegistrar.isInApprovedOnlyMode());
        Set<KeystoreUtil.KeystoreFormat> supportedKeystoreFormats = CryptoIntegration.getProvider().getSupportedKeyStoreTypes().collect(Collectors.toSet());
        assertThat(supportedKeystoreFormats, Matchers.containsInAnyOrder(
                KeystoreUtil.KeystoreFormat.PKCS12,
                KeystoreUtil.KeystoreFormat.BCFKS));
    }

    // BCFIPS approved mode supports only BCFKS. No JKS nor PKCS12 support for keystores
    @Test
    public void testKeystoreFormatsInApprovedMode() {
        Assume.assumeTrue(CryptoServicesRegistrar.isInApprovedOnlyMode());
        Set<KeystoreUtil.KeystoreFormat> supportedKeystoreFormats = CryptoIntegration.getProvider().getSupportedKeyStoreTypes().collect(Collectors.toSet());
        assertThat(supportedKeystoreFormats, Matchers.containsInAnyOrder(
                KeystoreUtil.KeystoreFormat.BCFKS));
    }
}
