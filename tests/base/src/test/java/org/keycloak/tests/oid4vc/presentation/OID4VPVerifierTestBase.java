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
import java.net.URLDecoder;
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
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.tests.oid4vc.OID4VCIssuerTestBase;
import org.keycloak.tests.oid4vc.OID4VCTestContext;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.KeyWrapperUtil;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_SIGNING_ALG;

public abstract class OID4VPVerifierTestBase extends OID4VCIssuerTestBase {

    protected static final String IDP_ALIAS = "oid4vp";
    protected static final String REDIRECT_URI = "http://127.0.0.1:8500/callback";

    @InjectHttpClient
    protected CloseableHttpClient httpClient;

    private String ecKeyComponentId;

    @BeforeEach
    void addVerifierSigningKey() {
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
        testRealm.cleanup().add(r -> r.components().component(ecKeyComponentId).remove());
    }

    @BeforeEach
    void signIssuedCredentialWithEs256() {
        // The verifier enforces ES256, so the issued credential must be signed with it.
        setCredentialScopeAttributes(sdJwtTypeCredentialScope, Map.of(VC_SIGNING_ALG, Algorithm.ES256));
    }

    protected record IssuedCredential(String sdJwtVc, KeyWrapper holderKey) {
    }

    protected IssuedCredential issueCredential() {
        OID4VCTestContext ctx = new OID4VCTestContext(client, sdJwtTypeCredentialScope);
        wallet.fetchCredentialByScope(ctx, ctx.getScope());
        String sdJwtVc = (String) ctx.getCredentialResponse().getCredentials().get(0).getCredential();
        Assertions.assertNotNull(sdJwtVc, "No SD-JWT VC issued");
        IssuedCredential credential = new IssuedCredential(sdJwtVc, wallet.getECKeyPair(ctx));
        wallet.logout();
        driver.cookies().deleteAll();
        driver.open("about:blank");
        return credential;
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
        testRealm.cleanup().add(r -> r.identityProviders().get(IDP_ALIAS).remove());
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

    // Wallet exchange -------------------------------------------------------------------------------------------------

    protected JsonNode requestObject() throws Exception {
        return requestObjectClaims(requestUri(openWalletPage()));
    }

    protected String present(IssuedCredential credential, JsonNode request) {
        return present(credential, request, request.path("nonce").asText());
    }

    protected String present(IssuedCredential credential, JsonNode request, String nonce) {
        return wallet.present(credential.sdJwtVc(), credential.holderKey(),
                request.path("client_id").asText(), nonce);
    }

    protected JsonNode directPost(JsonNode request, String vpToken, int expectedStatus) throws IOException {
        HttpPost post = new HttpPost(request.path("response_uri").asText());
        post.setEntity(new UrlEncodedFormEntity(List.of(
                new BasicNameValuePair("vp_token", vpToken),
                new BasicNameValuePair("state", request.path("state").asText()))));
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            int status = response.getStatusLine().getStatusCode();
            JsonNode body = JsonSerialization.readValue(EntityUtils.toByteArray(response.getEntity()), JsonNode.class);
            Assertions.assertEquals(expectedStatus, status, "Unexpected direct_post status, body: " + body);
            return body;
        }
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
        var idpResource = testRealm.admin().identityProviders().get(IDP_ALIAS);
        IdentityProviderRepresentation idp = idpResource.toRepresentation();
        idp.getConfig().put(OID4VPIdentityProviderConfig.PRINCIPAL_ATTRIBUTE, attribute);
        idpResource.update(idp);
    }

    protected static String requestUri(String walletUrl) {
        Matcher matcher = Pattern.compile("request_uri=([^&]+)").matcher(walletUrl);
        Assertions.assertTrue(matcher.find(), "Wallet link is missing request_uri: " + walletUrl);
        return URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
    }

    protected JsonNode requestObjectClaims(String requestUri) throws Exception {
        return JsonSerialization.readValue(new JWSInput(fetchRequestObject(requestUri)).getContent(), JsonNode.class);
    }

    protected String fetchRequestObject(String requestUri) throws IOException {
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(requestUri))) {
            Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            return EntityUtils.toString(response.getEntity());
        }
    }
}
