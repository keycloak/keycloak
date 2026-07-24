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

package org.keycloak.tests.oid4vc.presentation;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.ws.rs.core.Response;

import org.keycloak.broker.oid4vp.OID4VPIdentityProviderConfig;
import org.keycloak.broker.oid4vp.OID4VPIdentityProviderFactory;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.GeneratedEcdsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.testsuite.util.oauth.AbstractHttpResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vpRequestObjectResponse;
import org.keycloak.util.KeyWrapperUtil;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Assertions;

import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_SIGNING_ALG;

public abstract class OID4VPVerifierTestBase extends OID4VCIssuerTestBase {

    protected static final String IDP_ALIAS = "oid4vp";
    protected static final String REDIRECT_URI = "http://127.0.0.1:8500/callback";

    @InjectHttpClient
    protected CloseableHttpClient httpClient;

    // Static so the value set on the first test instance is visible to the later ones.
    private static String ecKeyComponentId;

    @Override
    @TestSetup
    public void configureTestRealm() {
        super.configureTestRealm();
        addVerifierSigningKey();
        // The verifier enforces ES256, so the issued credential must be signed with it.
        setCredentialScopeAttributes(requireExistingCredentialScope(sdJwtTypeCredentialScopeName),
                Map.of(VC_SIGNING_ALG, Algorithm.ES256));
    }

    private void addVerifierSigningKey() {
        ComponentRepresentation keyProvider = new ComponentRepresentation();
        keyProvider.setName("oid4vp-verifier-signing-key");
        keyProvider.setParentId(testRealm.getId());
        keyProvider.setProviderId(GeneratedEcdsaKeyProviderFactory.ID);
        keyProvider.setProviderType(KeyProvider.class.getName());
        keyProvider.setConfig(new MultivaluedHashMap<>(Map.of(
                Attributes.PRIORITY_KEY, List.of("100"),
                Attributes.ENABLED_KEY, List.of("true"),
                Attributes.ACTIVE_KEY, List.of("true"),
                GeneratedEcdsaKeyProviderFactory.ECDSA_ELLIPTIC_CURVE_KEY, List.of("P-256"),
                Attributes.EC_GENERATE_CERTIFICATE_KEY, List.of("true"))));

        try (Response response = testRealm.admin().components().add(keyProvider)) {
            Assertions.assertEquals(201, response.getStatus(), "Failed to add the verifier signing key");
            String location = response.getHeaderString("Location");
            ecKeyComponentId = location.substring(location.lastIndexOf('/') + 1);
        }
    }

    protected OID4VCTestContext issueCredential() {
        OID4VCTestContext ctx = new OID4VCTestContext(client, sdJwtTypeCredentialScope);
        wallet.fetchCredentialByScope(ctx, ctx.getScope());
        Assertions.assertNotNull(ctx.getCredentialResponse().getCredentials().get(0).getCredential(),
                "No SD-JWT VC issued");
        wallet.logout();
        driver.cookies().deleteAll();
        driver.open("about:blank");
        return ctx;
    }

    protected void createVerifierIdp(Map<String, String> config) {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias(IDP_ALIAS);
        idp.setProviderId(OID4VPIdentityProviderFactory.PROVIDER_ID);
        idp.setEnabled(true);
        idp.setConfig(config);
        try (Response response = testRealm.admin().identityProviders().create(idp)) {
            Assertions.assertEquals(201, response.getStatus(), "Failed to create OID4VP identity provider");
        }
    }

    protected static String dcqlQuery() {
        return """
                {
                  "credentials": [
                    {
                      "id": "identity",
                      "format": "dc+sd-jwt",
                      "meta": { "vct_values": ["%s"] },
                      "claims": [{ "path": ["email"] }, { "path": ["lastName"] }]
                    }
                  ]
                }""".formatted(sdJwtTypeCredentialVct);
    }

    protected void assertLoginContinued() {
        // TODO assert the presented claims (email, given and family name) were mapped to the user
        Assertions.assertTrue(driver.driver().getCurrentUrl().contains("login-actions"),
                "Expected to continue into the login flow, was: " + driver.driver().getCurrentUrl());
    }

    protected String realmSigningJwks() {
        try (CloseableHttpResponse response = httpClient.execute(
                new HttpGet(testRealm.getBaseUrl() + "/protocol/openid-connect/certs"))) {
            Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            return EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected JsonNode requestObject() {
        Oid4vpRequestObjectResponse response = wallet.fetchRequestObject(wallet.requestUri(openWalletPage()));
        assertNotCacheable(response);
        return response.getClaims();
    }

    // Wallet facing responses carry login correlating secrets and must not be cacheable.
    protected static void assertNotCacheable(AbstractHttpResponse response) {
        String cacheControl = response.getHeader("Cache-Control");
        Assertions.assertNotNull(cacheControl, "Wallet facing responses must send Cache-Control");
        Assertions.assertTrue(cacheControl.contains("no-store"),
                "Wallet facing responses must not be cacheable, was: " + cacheControl);
    }

    protected static boolean verifyEs256(JWSInput jws, X509Certificate certificate) throws Exception {
        KeyWrapper key = new KeyWrapper();
        key.setType(KeyType.EC);
        key.setAlgorithm(Algorithm.ES256);
        key.setUse(KeyUse.SIG);
        key.setPublicKey(certificate.getPublicKey());
        return KeyWrapperUtil.createSignatureVerifierContext(key)
                .verify(jws.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8), jws.getSignature());
    }

    protected String authUrl() {
        return testRealm.getBaseUrl() + "/protocol/openid-connect/auth"
                + "?client_id=" + OID4VCI_CLIENT_ID
                + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8)
                + "&response_type=code&scope=openid&state=abc&kc_idp_hint=" + IDP_ALIAS;
    }

    protected String openWalletPage() {
        driver.open(authUrl());
        String html = driver.driver().getPageSource();
        Matcher matcher = Pattern.compile("openid4vp://[^\"'\\s]+").matcher(html);
        Assertions.assertTrue(matcher.find(), "Wallet login page did not contain an openid4vp:// link");
        return matcher.group().replace("&amp;", "&");
    }

    // Adds an active ES256 key at higher priority than the verifier signing key but without a
    // certificate, so it becomes the active key the verifier would try to derive the client id from.
    protected void addActiveEs256KeyWithoutCertificate() {
        ComponentRepresentation keyProvider = new ComponentRepresentation();
        keyProvider.setName("oid4vp-key-without-certificate");
        keyProvider.setParentId(testRealm.getId());
        keyProvider.setProviderId(GeneratedEcdsaKeyProviderFactory.ID);
        keyProvider.setProviderType(KeyProvider.class.getName());
        keyProvider.setConfig(new MultivaluedHashMap<>(Map.of(
                Attributes.PRIORITY_KEY, List.of("200"),
                Attributes.ENABLED_KEY, List.of("true"),
                Attributes.ACTIVE_KEY, List.of("true"),
                GeneratedEcdsaKeyProviderFactory.ECDSA_ELLIPTIC_CURVE_KEY, List.of("P-256"),
                Attributes.EC_GENERATE_CERTIFICATE_KEY, List.of("false"))));
        try (Response response = testRealm.admin().components().add(keyProvider)) {
            Assertions.assertEquals(201, response.getStatus(), "Failed to add the key without certificate");
            String location = response.getHeaderString("Location");
            String id = location.substring(location.lastIndexOf('/') + 1);
            testRealm.cleanup().add(r -> r.components().component(id).remove());
        }
    }

    protected void updatePrincipalAttribute(String attribute) {
        updateIdpConfig(OID4VPIdentityProviderConfig.PRINCIPAL_ATTRIBUTE, attribute);
    }

    protected void updateIdpConfig(String key, String value) {
        testRealm.updateIdentityProvider(IDP_ALIAS, idp -> idp.getConfig().put(key, value));
    }

    protected void disableVerifierSigningKey() {
        testRealm.updateComponent(ecKeyComponentId,
                component -> component.getConfig().putSingle(Attributes.ENABLED_KEY, "false"));
    }

    protected String verifierSigningKeyKid() {
        return verifierSigningKeyMetadata().getKid();
    }

    protected String verifierSigningKeyPublicKeyPem() {
        return verifierSigningKeyMetadata().getPublicKey();
    }

    private KeysMetadataRepresentation.KeyMetadataRepresentation verifierSigningKeyMetadata() {
        return testRealm.admin().keys().getKeyMetadata().getKeys().stream()
                .filter(key -> ecKeyComponentId.equals(key.getProviderId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Verifier signing key not found in the realm keys"));
    }

}
