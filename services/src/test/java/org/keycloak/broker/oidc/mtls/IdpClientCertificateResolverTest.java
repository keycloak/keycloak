package org.keycloak.broker.oidc.mtls;

import java.lang.reflect.Proxy;
import java.util.stream.Stream;

import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

public class IdpClientCertificateResolverTest {

    // Proxy-based stubs so the services module needs no Mockito dependency.
    private static KeycloakSession sessionReturning(Stream<KeyWrapper> keys) {
        KeyManager keyManager = (KeyManager) Proxy.newProxyInstance(
                IdpClientCertificateResolverTest.class.getClassLoader(),
                new Class<?>[] { KeyManager.class },
                (proxy, method, args) -> {
                    if ("getKeysStream".equals(method.getName()) && args != null && args.length == 1) {
                        return keys;
                    }
                    throw new UnsupportedOperationException("stub: " + method.getName());
                });
        return (KeycloakSession) Proxy.newProxyInstance(
                IdpClientCertificateResolverTest.class.getClassLoader(),
                new Class<?>[] { KeycloakSession.class },
                (proxy, method, args) -> {
                    if ("keys".equals(method.getName())) {
                        return keyManager;
                    }
                    throw new UnsupportedOperationException("stub: " + method.getName());
                });
    }

    private static RealmModel realmStub() {
        return (RealmModel) Proxy.newProxyInstance(
                IdpClientCertificateResolverTest.class.getClassLoader(),
                new Class<?>[] { RealmModel.class },
                (proxy, method, args) -> { throw new UnsupportedOperationException("stub: " + method.getName()); });
    }

    @Test
    public void resolvesKeyByProviderId() {
        KeyWrapper match = new KeyWrapper();
        match.setProviderId("kp-1");
        match.setStatus(KeyStatus.from(true, true)); // ACTIVE + enabled
        match.setPrivateKey(new java.security.PrivateKey() {
            public String getAlgorithm() { return "RSA"; }
            public String getFormat() { return "PKCS8"; }
            public byte[] getEncoded() { return new byte[0]; }
        });
        match.setCertificate(dummyCert());
        KeyWrapper other = new KeyWrapper();
        other.setProviderId("kp-2");

        RealmModel realm = realmStub();
        KeycloakSession session = sessionReturning(Stream.of(other, match));

        KeyWrapper result = new IdpClientCertificateResolver(session).resolve(realm, "kp-1");
        assertSame(match, result);
    }

    @Test
    public void skipsDisabledKey() {
        // A key that matches by providerId but is DISABLED must not be returned.
        KeyWrapper disabled = new KeyWrapper();
        disabled.setProviderId("kp-1");
        disabled.setStatus(KeyStatus.from(false, false)); // DISABLED
        disabled.setPrivateKey(new java.security.PrivateKey() {
            public String getAlgorithm() { return "RSA"; }
            public String getFormat() { return "PKCS8"; }
            public byte[] getEncoded() { return new byte[0]; }
        });
        disabled.setCertificate(dummyCert());

        RealmModel realm = realmStub();
        KeycloakSession session = sessionReturning(Stream.of(disabled));
        IdpClientCertificateResolver resolver = new IdpClientCertificateResolver(session);
        assertThrows(IllegalStateException.class, () -> resolver.resolve(realm, "kp-1"));
    }

    @Test
    public void failsWhenKeyProviderMissing() {
        RealmModel realm = realmStub();
        KeycloakSession session = sessionReturning(Stream.empty());
        IdpClientCertificateResolver resolver = new IdpClientCertificateResolver(session);
        assertThrows(IllegalStateException.class, () -> resolver.resolve(realm, "missing"));
    }

    @Test
    public void failsWhenKeyHasNoPrivateKey() {
        KeyWrapper noPriv = new KeyWrapper();
        noPriv.setProviderId("kp-1");
        noPriv.setStatus(KeyStatus.from(true, true)); // enabled so the filter passes
        noPriv.setCertificate(dummyCert());
        RealmModel realm = realmStub();
        KeycloakSession session = sessionReturning(Stream.of(noPriv));
        IdpClientCertificateResolver resolver = new IdpClientCertificateResolver(session);
        assertThrows(IllegalStateException.class, () -> resolver.resolve(realm, "kp-1"));
    }

    private static java.security.cert.X509Certificate dummyCert() {
        return new java.security.cert.X509Certificate() {
            public void checkValidity() {}
            public void checkValidity(java.util.Date date) {}
            public int getVersion() { return 3; }
            public java.math.BigInteger getSerialNumber() { return java.math.BigInteger.ONE; }
            public java.security.Principal getIssuerDN() { return null; }
            public java.security.Principal getSubjectDN() { return null; }
            public java.util.Date getNotBefore() { return null; }
            public java.util.Date getNotAfter() { return null; }
            public byte[] getTBSCertificate() { return new byte[0]; }
            public byte[] getSignature() { return new byte[0]; }
            public String getSigAlgName() { return "SHA256withRSA"; }
            public String getSigAlgOID() { return "1.2.840.113549.1.1.11"; }
            public byte[] getSigAlgParams() { return null; }
            public boolean[] getIssuerUniqueID() { return null; }
            public boolean[] getSubjectUniqueID() { return null; }
            public boolean[] getKeyUsage() { return null; }
            public int getBasicConstraints() { return -1; }
            public byte[] getEncoded() { return new byte[0]; }
            public void verify(java.security.PublicKey key) {}
            public void verify(java.security.PublicKey key, String sigProvider) {}
            public String toString() { return "dummy"; }
            public java.security.PublicKey getPublicKey() { return null; }
            public java.util.Set<String> getCriticalExtensionOIDs() { return null; }
            public java.util.Set<String> getNonCriticalExtensionOIDs() { return null; }
            public byte[] getExtensionValue(String oid) { return null; }
            public boolean hasUnsupportedCriticalExtension() { return false; }
        };
    }
}
