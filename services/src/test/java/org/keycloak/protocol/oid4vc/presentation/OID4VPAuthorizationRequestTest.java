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

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.protocol.oid4vc.model.presentation.AuthorizationRequest;
import org.keycloak.protocol.oid4vc.model.presentation.DcqlQuery;
import org.keycloak.util.JsonSerialization;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OID4VPAuthorizationRequestTest {

    @BeforeClass
    public static void initCrypto() {
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
    }

    @Test
    public void testRequestObjectIssuerMatchesClientId() {
        OID4VPIdentityProviderConfig config = new OID4VPIdentityProviderConfig();
        OID4VPIdentityProvider provider = new OID4VPIdentityProvider(null, config);

        AuthorizationRequest authorizationRequest = provider.createAuthorizationRequest(
                "x509_san_dns:verifier.example.org",
                "state",
                "https://verifier.example.org/direct-post",
                "nonce");

        assertEquals("x509_san_dns:verifier.example.org", authorizationRequest.getIssuer());
        assertEquals("x509_san_dns:verifier.example.org", authorizationRequest.getClientId());
    }

    @Test
    public void testQueryParameterWalletUriContainsAuthorizationRequestParameters() throws Exception {
        OID4VPIdentityProviderConfig config = new OID4VPIdentityProviderConfig();
        OID4VPIdentityProvider provider = new OID4VPIdentityProvider(null, config);

        URI walletUri = provider.createQueryParameterWalletUri(
                "redirect_uri:https://verifier.example.org/direct-post",
                "state",
                "https://verifier.example.org/direct-post",
                "nonce");
        Map<String, String> queryParams = queryParams(walletUri);

        assertEquals(Set.of(
                OID4VPConstants.CLIENT_ID,
                OID4VPConstants.RESPONSE_TYPE,
                OID4VPConstants.RESPONSE_MODE,
                OID4VPConstants.RESPONSE_URI,
                OID4VPConstants.STATE,
                OID4VPConstants.NONCE,
                OID4VPConstants.DCQL_QUERY,
                OID4VPConstants.CLIENT_METADATA), queryParams.keySet());
        assertEquals("redirect_uri:https://verifier.example.org/direct-post", queryParams.get(OID4VPConstants.CLIENT_ID));
        assertEquals(OID4VPConstants.RESPONSE_TYPE_VP_TOKEN, queryParams.get(OID4VPConstants.RESPONSE_TYPE));
        assertEquals(OID4VPConstants.RESPONSE_MODE_DIRECT_POST, queryParams.get(OID4VPConstants.RESPONSE_MODE));
        assertEquals("https://verifier.example.org/direct-post", queryParams.get(OID4VPConstants.RESPONSE_URI));
        assertEquals("state", queryParams.get(OID4VPConstants.STATE));
        assertEquals("nonce", queryParams.get(OID4VPConstants.NONCE));
        assertTrue(JsonSerialization.readValue(queryParams.get(OID4VPConstants.DCQL_QUERY), DcqlQuery.class)
                .getCredentials()
                .stream()
                .anyMatch(credential -> OID4VPIdentityProvider.CREDENTIAL_QUERY_ID.equals(credential.getId())));
        assertTrue(queryParams.get(OID4VPConstants.CLIENT_METADATA).contains("vp_formats_supported"));
    }

    @Test
    public void testWalletUriPreservesConfiguredQueryParameters() {
        OID4VPIdentityProviderConfig config = new OID4VPIdentityProviderConfig();
        config.getConfig().put(OID4VPIdentityProviderConfig.WALLET_SCHEME, "openid4vp://wallet?existing=value");
        OID4VPIdentityProvider provider = new OID4VPIdentityProvider(null, config);

        URI walletUri = provider.createQueryParameterWalletUri(
                "redirect_uri:https://verifier.example.org/direct-post",
                "state",
                "https://verifier.example.org/direct-post",
                "nonce");
        Map<String, String> queryParams = queryParams(walletUri);

        assertEquals("value", queryParams.get("existing"));
        assertEquals("state", queryParams.get(OID4VPConstants.STATE));
    }

    @Test
    public void testRequestObjectSignerUsesConfiguredCertificateAndPrivateKey() {
        KeyPair keyPair = KeyUtils.generateEcKeyPair("secp256r1");
        X509Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair, "oid4vp-verifier");
        OID4VPIdentityProviderConfig config = new OID4VPIdentityProviderConfig();
        config.setX509CertificatePem(PemUtils.encodeCertificate(certificate));
        config.setX509PrivateKeyPem(pkcs8PrivateKeyPem(keyPair));

        RequestObjectSigner signer = RequestObjectSigner.fromConfig(config);

        assertEquals(certificate, signer.getCertificate());
    }

    private String pkcs8PrivateKeyPem(KeyPair keyPair) {
        return "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";
    }

    private Map<String, String> queryParams(URI uri) {
        return Arrays.stream(uri.getRawQuery().split("&"))
                .map(parameter -> parameter.split("=", 2))
                .collect(Collectors.toMap(
                        parameter -> URLDecoder.decode(parameter[0], StandardCharsets.UTF_8),
                        parameter -> URLDecoder.decode(parameter[1], StandardCharsets.UTF_8)));
    }
}
