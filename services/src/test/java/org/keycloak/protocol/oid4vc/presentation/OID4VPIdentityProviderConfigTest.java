/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.protocol.oid4vc.presentation;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.List;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class OID4VPIdentityProviderConfigTest {

    @BeforeClass
    public static void initCrypto() {
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
    }

    @Test
    public void testDefaultsUseQueryParameterTransportAndRedirectUriClientIdentifierPrefix() {
        OID4VPIdentityProviderConfig config = new OID4VPIdentityProviderConfig();

        assertEquals(AuthorizationRequestTransport.QUERY_PARAMETERS, config.getAuthorizationRequestTransport());
        assertEquals(ClientIdentifierPrefix.REDIRECT_URI, config.getClientIdentifierPrefix());
    }

    @Test
    public void testConfiguredAuthorizationRequestTransport() {
        OID4VPIdentityProviderConfig config = new OID4VPIdentityProviderConfig();
        config.getConfig().put(
                OID4VPIdentityProviderConfig.AUTHORIZATION_REQUEST_TRANSPORT,
                AuthorizationRequestTransport.QUERY_PARAMETERS.getValue());

        assertEquals(AuthorizationRequestTransport.QUERY_PARAMETERS, config.getAuthorizationRequestTransport());
    }

    @Test
    public void testConfiguredClientIdentifierPrefix() {
        OID4VPIdentityProviderConfig config = new OID4VPIdentityProviderConfig();
        config.getConfig().put(OID4VPIdentityProviderConfig.CLIENT_IDENTIFIER_PREFIX, ClientIdentifierPrefix.X509_HASH.getValue());

        assertEquals(ClientIdentifierPrefix.X509_HASH, config.getClientIdentifierPrefix());
    }

    @Test
    public void testRejectsUnsupportedAuthorizationRequestTransport() {
        OID4VPIdentityProviderConfig config = new OID4VPIdentityProviderConfig();
        config.getConfig().put(OID4VPIdentityProviderConfig.AUTHORIZATION_REQUEST_TRANSPORT, "unsupported");

        assertThrows(IllegalArgumentException.class, config::getAuthorizationRequestTransport);
    }

    @Test
    public void testRejectsUnsupportedClientIdentifierPrefix() {
        OID4VPIdentityProviderConfig config = new OID4VPIdentityProviderConfig();
        config.getConfig().put(OID4VPIdentityProviderConfig.CLIENT_IDENTIFIER_PREFIX, "unsupported");

        assertThrows(IllegalArgumentException.class, config::getClientIdentifierPrefix);
    }

    @Test
    public void testRejectsRedirectUriPrefixWithSignedRequestObject() {
        OID4VPIdentityProviderConfig config = new OID4VPIdentityProviderConfig();
        config.getConfig().put(OID4VPIdentityProviderConfig.AUTHORIZATION_REQUEST_TRANSPORT,
                AuthorizationRequestTransport.REQUEST_URI.getValue());
        config.getConfig().put(OID4VPIdentityProviderConfig.CLIENT_IDENTIFIER_PREFIX,
                ClientIdentifierPrefix.REDIRECT_URI.getValue());

        assertThrows(IllegalArgumentException.class, () -> config.validate(null));
    }

    @Test
    public void testRejectsCertificateBoundPrefixesWithoutSignedRequestObject() {
        OID4VPIdentityProviderConfig config = new OID4VPIdentityProviderConfig();
        config.getConfig().put(OID4VPIdentityProviderConfig.AUTHORIZATION_REQUEST_TRANSPORT,
                AuthorizationRequestTransport.QUERY_PARAMETERS.getValue());
        config.getConfig().put(OID4VPIdentityProviderConfig.CLIENT_IDENTIFIER_PREFIX,
                ClientIdentifierPrefix.X509_SAN_DNS.getValue());

        assertThrows(IllegalArgumentException.class, () -> config.validate(null));
    }

    @Test
    public void testRejectsX509SanDnsPrefixWithoutCertificate() {
        OID4VPIdentityProviderConfig config = requestUriConfig(ClientIdentifierPrefix.X509_SAN_DNS);

        assertThrows(IllegalArgumentException.class, () -> config.validate(null));
    }

    @Test
    public void testRejectsX509SanDnsPrefixWithoutCertificateSan() {
        OID4VPIdentityProviderConfig config = requestUriConfig(ClientIdentifierPrefix.X509_SAN_DNS);
        config.setX509CertificatePem(PemUtils.encodeCertificate(certificate()));

        assertThrows(IllegalArgumentException.class, () -> config.validate(null));
    }

    @Test
    public void testAcceptsX509SanDnsPrefixWithCertificateSan() throws Exception {
        OID4VPIdentityProviderConfig config = requestUriConfig(ClientIdentifierPrefix.X509_SAN_DNS);
        config.setX509CertificatePem(PemUtils.encodeCertificate(certificateWithDnsSan("verifier.example.org")));

        config.validate(null);
    }

    private OID4VPIdentityProviderConfig requestUriConfig(ClientIdentifierPrefix clientIdentifierPrefix) {
        OID4VPIdentityProviderConfig config = new OID4VPIdentityProviderConfig();
        config.getConfig().put(OID4VPIdentityProviderConfig.AUTHORIZATION_REQUEST_TRANSPORT,
                AuthorizationRequestTransport.REQUEST_URI.getValue());
        config.getConfig().put(OID4VPIdentityProviderConfig.CLIENT_IDENTIFIER_PREFIX, clientIdentifierPrefix.getValue());
        return config;
    }

    private X509Certificate certificate() {
        return CertificateUtils.generateV1SelfSignedCertificate(KeyUtils.generateEcKeyPair("secp256r1"), "oid4vp-verifier");
    }

    private X509Certificate certificateWithDnsSan(String dnsName) throws Exception {
        KeyPair keyPair = KeyUtils.generateEcKeyPair("secp256r1");
        X509Certificate caCert = CertificateUtils.generateV1SelfSignedCertificate(keyPair, dnsName);
        return CertificateUtils.generateV3Certificate(keyPair, keyPair.getPrivate(), caCert, dnsName, List.of(dnsName));
    }
}
