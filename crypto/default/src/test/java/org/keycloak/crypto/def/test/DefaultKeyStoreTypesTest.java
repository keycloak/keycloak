package org.keycloak.crypto.def.test;

import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.rule.CryptoInitRule;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultKeyStoreTypesTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Test
    public void testKeystoreFormats() {
        Set<KeystoreUtil.KeystoreFormat> supportedKeystoreFormats = CryptoIntegration.getProvider().getSupportedKeyStoreTypes().collect(Collectors.toSet());
        assertThat(supportedKeystoreFormats, Matchers.containsInAnyOrder(
                KeystoreUtil.KeystoreFormat.JKS,
                KeystoreUtil.KeystoreFormat.PKCS12,
                KeystoreUtil.KeystoreFormat.BCFKS));
    }

    @Test
    public void testDefaultKeystoreType() {
        Assert.assertEquals("PKCS12", KeystoreUtil.getKeystoreType("PKCS12", "some/foo.jks", "JKS"));
        Assert.assertEquals("PKCS12", KeystoreUtil.getKeystoreType("PKCS12", "some/foo.pkcs12", "JKS"));
        Assert.assertEquals("PKCS12", KeystoreUtil.getKeystoreType("PKCS12", "some/foo.bcfks", "JKS"));
        Assert.assertEquals("JKS", KeystoreUtil.getKeystoreType(null, "some/foo.jks", "JKS"));
        Assert.assertEquals("PKCS12", KeystoreUtil.getKeystoreType(null, "some/foo.p12", "JKS"));
        Assert.assertEquals("BCFKS", KeystoreUtil.getKeystoreType(null, "some/foo.bcfks", "JKS"));
        Assert.assertEquals("JKS", KeystoreUtil.getKeystoreType(null, "some/foo.bcfksl", "JKS"));
    }
}
