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
package org.keycloak.tests.broker.saml;

import java.io.IOException;
import java.net.URI;
import java.security.Key;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.keys.PublicKeyStorageProvider;
import org.keycloak.keys.infinispan.InfinispanPublicKeyStorageProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlMetadataKeyLocator;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.rotation.KeyLocator;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAML2LoginResponseBuilder;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectHttpServer;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.HttpServerUtil;
import org.keycloak.tests.saml.SamlClient;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * A SAML identity provider that takes the keys to validate signatures from the metadata descriptor URL. The
 * signature of a response is validated before the response is trusted, so a response that requests a key that
 * is not cached must not make the request wait for the metadata endpoint. The keys are reloaded in the
 * background and the cached keys are used until the reload finishes successfully.
 */
@KeycloakIntegrationTest(config = SamlBrokerMetadataKeysReloadTest.ServerConfig.class)
public class SamlBrokerMetadataKeysReloadTest {

    private static final String IDP_ALIAS = "external-saml-idp";
    private static final String IDP_ENTITY_ID = "https://external-saml-idp.example.org/";
    private static final String IDP_METADATA_PATH = "/external-saml-idp/metadata";
    private static final String IDP_INITIATED_SSO_URL_NAME = "sales-post";

    // defaults of the minTimeBetweenRequests and maxCacheTime options of the public key storage, in seconds
    private static final int MIN_TIME_BETWEEN_REQUESTS = 10;
    private static final int MAX_CACHE_TIME = 24 * 60 * 60;

    @InjectRealm(config = ConsumerRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectHttpServer
    HttpServer httpServer;

    @InjectHttpClient(followRedirects = false)
    CloseableHttpClient httpClient;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    // The server loads this test class (lambda body) and its nested classes from the test JVM, so its package must be permitted
    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    private final AtomicInteger metadataRequests = new AtomicInteger();
    private final BlockingQueue<HttpExchange> heldMetadataRequests = new LinkedBlockingQueue<>();

    // What the metadata endpoint of the external IdP does with the next requests
    private volatile String metadata;
    private volatile int metadataStatus = 200;
    private volatile boolean holdMetadataRequests = false;

    private SigningKey currentKey;
    private SigningKey newKey;

    @BeforeEach
    public void startExternalIdp() {
        CryptoIntegration.init(getClass().getClassLoader());
        currentKey = SigningKey.create("current-key");
        newKey = SigningKey.create("new-key");
        metadata = metadata(currentKey);

        httpServer.createContext(IDP_METADATA_PATH, this::metadataRequested);
        String metadataUrl = "http://localhost:" + httpServer.getAddress().getPort() + IDP_METADATA_PATH;
        realm.updateIdentityProvider(IDP_ALIAS, idp -> idp.getConfig().put(SAMLIdentityProviderConfig.METADATA_DESCRIPTOR_URL, metadataUrl));
    }

    @AfterEach
    public void stopExternalIdp() {
        heldMetadataRequests.forEach(HttpExchange::close);
        httpServer.removeContext(IDP_METADATA_PATH);
    }

    @Test
    public void unknownKeyDoesNotWaitForMetadata() throws Exception {
        assertLoginAccepted(currentKey);
        assertThat("the first response loads the keys", metadataRequests.get(), is(1));

        // the IdP rotates the key and its metadata endpoint does not answer for now
        metadata = metadata(newKey);
        holdMetadataRequests = true;
        timeOffSet.set(MIN_TIME_BETWEEN_REQUESTS + 5);

        // answered although the metadata request triggered by the unknown key is still waiting
        assertInvalidSignature(newKey);
        HttpExchange metadataRequest = heldMetadataRequests.poll(30, TimeUnit.SECONDS);
        assertThat("the keys are reloaded in the background", metadataRequest, notNullValue());

        // the cached keys remain in use while the reload is running, and no other reload is started
        assertLoginAccepted(currentKey);
        assertInvalidSignature(newKey);
        assertThat(metadataRequests.get(), is(2));

        holdMetadataRequests = false;
        sendMetadata(metadataRequest);
        awaitLoginAccepted(newKey);

        // the retired key is unknown now, it does not cause another request before the minimum time passes
        assertInvalidSignature(currentKey);
        assertThat(metadataRequests.get(), is(2));
    }

    @Test
    public void cachedKeysAreUsedWhenReloadFails() throws Exception {
        assertLoginAccepted(currentKey);
        assertThat(metadataRequests.get(), is(1));

        metadataStatus = 500;
        timeOffSet.set(MIN_TIME_BETWEEN_REQUESTS + 5);

        assertInvalidSignature(newKey);
        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAtomic(metadataRequests, is(2));

        // the failed reload does not remove the cached keys and it is not repeated before the minimum time passes
        assertLoginAccepted(currentKey);
        for (int i = 0; i < 5; i++) {
            assertInvalidSignature(newKey);
        }
        assertThat(metadataRequests.get(), is(2));

        // the metadata endpoint works again and publishes the new key, the reload is tried when the minimum time passes
        metadata = metadata(newKey);
        metadataStatus = 200;
        timeOffSet.set(2 * (MIN_TIME_BETWEEN_REQUESTS + 5));

        awaitLoginAccepted(newKey);
        assertThat(metadataRequests.get(), is(3));
    }

    @Test
    public void publishedKeyIsLoadedByPeriodicReload() throws Exception {
        assertLoginAccepted(currentKey);
        assertThat(metadataRequests.get(), is(1));

        // the IdP publishes the new key before using it
        metadata = metadata(currentKey, newKey);
        timeOffSet.set(MAX_CACHE_TIME + 5);

        // the keys are due for a reload, it is done in the background by a response signed with a known key
        assertLoginAccepted(currentKey);
        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAtomic(metadataRequests, is(2));

        awaitLoginAccepted(newKey);
        assertLoginAccepted(currentKey);
        assertThat(metadataRequests.get(), is(2));
    }

    @Test
    public void failingReloadIsLimitedForRepeatedUnknownKeys() {
        runOnServer.run(session -> {
            String modelKey = "test::" + UUID.randomUUID();
            PublicKeysWrapper keys = new PublicKeysWrapper(List.of(SigningKey.create("current-key").toKeyWrapper()));
            AtomicInteger reloads = new AtomicInteger();
            Function<KeycloakSession, PublicKeyLoader> failingReload = backgroundSession -> () -> {
                reloads.incrementAndGet();
                throw new IOException("the metadata endpoint is not available");
            };
            KeyLocator keyLocator = new SamlMetadataKeyLocator(modelKey, () -> keys, failingReload, KeyUse.SIG,
                    session.getProvider(PublicKeyStorageProvider.class));

            List<String> warnings = new CopyOnWriteArrayList<>();
            WarningHandler warningHandler = new WarningHandler(modelKey, warnings);
            Logger locatorLogger = Logger.getLogger(SamlMetadataKeyLocator.class.getName());
            Logger storageLogger = Logger.getLogger(InfinispanPublicKeyStorageProvider.class.getName());
            locatorLogger.addHandler(warningHandler);
            storageLogger.addHandler(warningHandler);
            try {
                // the keys are loaded by the first request, they can be reloaded when the minimum time passes
                assertThat(getKey(keyLocator, "current-key"), notNullValue());
                Time.setOffset(MIN_TIME_BETWEEN_REQUESTS + 5);

                // only the first unknown key starts a reload, also after the reload has failed
                for (int i = 0; i < 20; i++) {
                    assertThat(getKey(keyLocator, "unknown-key-" + i), nullValue());
                }
                Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> warnings.size() == 2);
                for (int i = 20; i < 40; i++) {
                    assertThat(getKey(keyLocator, "unknown-key-" + i), nullValue());
                }
                assertThat(reloads.get(), is(1));
                assertThat(warnings, containsInAnyOrder(containsString("unknown-key-0"), containsString("Error reloading keys")));
                assertThat(getKey(keyLocator, "current-key"), notNullValue());

                // the reload is tried again when the minimum time has passed since the failure
                Time.setOffset(2 * (MIN_TIME_BETWEEN_REQUESTS + 5));
                for (int i = 40; i < 60; i++) {
                    assertThat(getKey(keyLocator, "unknown-key-" + i), nullValue());
                }
                Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> warnings.size() == 4);
                assertThat(reloads.get(), is(2));
                assertThat(warnings, hasItem(containsString("unknown-key-40")));
            } finally {
                Time.setOffset(0);
                locatorLogger.removeHandler(warningHandler);
                storageLogger.removeHandler(warningHandler);
            }
        });
    }

    private static Key getKey(KeyLocator keyLocator, String name) {
        try {
            return keyLocator.getKey(name);
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private void metadataRequested(HttpExchange exchange) {
        metadataRequests.incrementAndGet();
        if (holdMetadataRequests) {
            // answered later by the test, the thread of the HTTP server is not blocked
            heldMetadataRequests.add(exchange);
        } else {
            sendMetadata(exchange);
        }
    }

    private void sendMetadata(HttpExchange exchange) {
        HttpServerUtil.sendResponse(exchange, metadataStatus, Map.of("Content-Type", List.of("application/xml")), metadata);
    }

    private void awaitLoginAccepted(SigningKey key) {
        Awaitility.await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> assertLoginAccepted(key));
    }

    private void assertLoginAccepted(SigningKey key) throws Exception {
        try (CloseableHttpResponse response = postResponse(key)) {
            assertThat(response.getStatusLine().getStatusCode(), is(302));
            assertThat(response.getFirstHeader(HttpHeaders.LOCATION).getValue(), containsString("/login-actions/first-broker-login"));
        }
    }

    private void assertInvalidSignature(SigningKey key) throws Exception {
        try (CloseableHttpResponse response = postResponse(key)) {
            assertThat(response.getStatusLine().getStatusCode(), is(400));
            assertThat(EntityUtils.toString(response.getEntity()), containsString("Invalid signature in response from identity provider"));
        }
    }

    private CloseableHttpResponse postResponse(SigningKey key) throws Exception {
        SAML2LoginResponseBuilder builder = new SAML2LoginResponseBuilder()
                .issuer(IDP_ENTITY_ID)
                .destination(brokerEndpoint())
                .requestIssuer(realm.getBaseUrl())
                .assertionExpiration(300)
                .subjectExpiration(300)
                .nameIdentifier(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get(), "alice@example.org");
        Document response = builder.buildDocument(builder.buildModel());
        BaseSAML2BindingBuilder<?> bindingBuilder = new BaseSAML2BindingBuilder<>()
                .signatureAlgorithm(SignatureAlgorithm.RSA_SHA256)
                .signWith(key.name(), key.keyPair(), key.certificate());
        bindingBuilder.signAssertions().signAssertion(response);
        bindingBuilder.signDocument(response);

        return httpClient.execute(SamlClient.Binding.POST.createSamlUnsignedResponse(URI.create(brokerEndpoint()), null, response));
    }

    private String brokerEndpoint() {
        return realm.getBaseUrl() + "/broker/" + IDP_ALIAS + "/endpoint/clients/" + IDP_INITIATED_SSO_URL_NAME;
    }

    private static String metadata(SigningKey... keys) {
        // No validUntil/cacheDuration: the maximum cache time of the public key storage is used.
        StringBuilder descriptor = new StringBuilder()
                .append("<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" entityID=\"")
                .append(IDP_ENTITY_ID).append("\">")
                .append("<md:IDPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">");
        for (SigningKey key : keys) {
            descriptor.append("<md:KeyDescriptor use=\"signing\"><ds:KeyInfo><ds:KeyName>").append(key.name()).append("</ds:KeyName>")
                    .append("<ds:X509Data><ds:X509Certificate>").append(PemUtils.encodeCertificate(key.certificate()))
                    .append("</ds:X509Certificate></ds:X509Data></ds:KeyInfo></md:KeyDescriptor>");
        }
        return descriptor
                .append("<md:SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"").append(IDP_ENTITY_ID).append("sso\"/>")
                .append("</md:IDPSSODescriptor></md:EntityDescriptor>")
                .toString();
    }

    private record SigningKey(String name, KeyPair keyPair, X509Certificate certificate) {

        static SigningKey create(String name) {
            KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);
            return new SigningKey(name, keyPair, CertificateUtils.generateV1SelfSignedCertificate(keyPair, name));
        }

        KeyWrapper toKeyWrapper() {
            KeyWrapper key = new KeyWrapper();
            key.setKid(name);
            key.setUse(KeyUse.SIG);
            key.setType(keyPair.getPublic().getAlgorithm());
            key.setAlgorithm(keyPair.getPublic().getAlgorithm());
            key.setPublicKey(keyPair.getPublic());
            key.setCertificate(certificate);
            return key;
        }
    }

    /**
     * Collects the warnings logged in the server for a model key. Must be static to not send the test to the server.
     */
    private static class WarningHandler extends Handler {

        private final String modelKey;
        private final List<String> warnings;

        WarningHandler(String modelKey, List<String> warnings) {
            this.modelKey = modelKey;
            this.warnings = warnings;
        }

        @Override
        public void publish(LogRecord record) {
            if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                String warning = record.getMessage() + " " + Arrays.toString(record.getParameters());
                if (warning.contains(modelKey)) {
                    warnings.add(warning);
                }
            }
        }

        @Override
        public void flush() {
            // no-op
        }

        @Override
        public void close() {
            // no-op
        }
    }

    public static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            // A test leaves a request to the metadata endpoint without answer while it checks other responses.
            // That request must not reach the socket timeout of the server, 5 seconds by default, if the checks are slow.
            return config.option("spi-connections-http-client-default-socket-timeout-millis", "300000");
        }
    }

    public static class ConsumerRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .identityProviders(IdentityProviderBuilder.create()
                            .providerId(SAMLIdentityProviderFactory.PROVIDER_ID)
                            .alias(IDP_ALIAS)
                            .attribute(SAMLIdentityProviderConfig.IDP_ENTITY_ID, IDP_ENTITY_ID)
                            .attribute(SAMLIdentityProviderConfig.SINGLE_SIGN_ON_SERVICE_URL, IDP_ENTITY_ID + "sso")
                            .attribute(SAMLIdentityProviderConfig.VALIDATE_SIGNATURE, Boolean.TRUE.toString())
                            .attribute(SAMLIdentityProviderConfig.USE_METADATA_DESCRIPTOR_URL, Boolean.TRUE.toString())
                            // replaced with the URL of the HTTP server of the test
                            .attribute(SAMLIdentityProviderConfig.METADATA_DESCRIPTOR_URL, IDP_ENTITY_ID + "metadata"))
                    // the client that receives the logins initiated by the external IdP
                    .clients(ClientBuilder.create("http://localhost:8080/sales-post/")
                            .protocol(SamlProtocol.LOGIN_PROTOCOL)
                            .baseUrl("http://localhost:8080/sales-post")
                            .redirectUris("http://localhost:8080/sales-post/*")
                            .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, SamlProtocol.ATTRIBUTE_FALSE_VALUE)
                            .attribute(SamlProtocol.SAML_IDP_INITIATED_SSO_URL_NAME, IDP_INITIATED_SSO_URL_NAME)
                            .attribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, "http://localhost:8080/sales-post/saml"));
        }
    }
}
