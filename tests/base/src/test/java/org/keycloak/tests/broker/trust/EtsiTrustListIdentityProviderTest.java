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

package org.keycloak.tests.broker.trust;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.core.Response;

import org.keycloak.broker.provider.TrustMaterialIdentityProvider;
import org.keycloak.broker.provider.TrustMaterialRequest;
import org.keycloak.broker.provider.TrustMaterialResolver;
import org.keycloak.broker.provider.X509TrustMaterial;
import org.keycloak.broker.trust.etsi.EtsiTrustListIdentityProviderConfig;
import org.keycloak.broker.trust.etsi.EtsiTrustListIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.testframework.annotations.InjectHttpServer;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.broker.trust.TrustListTestSupport.PublishedTrustList;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.broker.trust.TrustListTestSupport.ISSUANCE_SERVICE;
import static org.keycloak.tests.broker.trust.TrustListTestSupport.LOTE_TYPE;
import static org.keycloak.tests.broker.trust.TrustListTestSupport.REVOCATION_SERVICE;
import static org.keycloak.tests.broker.trust.TrustListTestSupport.caCertificate;
import static org.keycloak.tests.broker.trust.TrustListTestSupport.caCertificateWithoutCertificateSigning;
import static org.keycloak.tests.broker.trust.TrustListTestSupport.ecKeyPair;
import static org.keycloak.tests.broker.trust.TrustListTestSupport.encode;
import static org.keycloak.tests.broker.trust.TrustListTestSupport.endEntityCertificate;
import static org.keycloak.tests.broker.trust.TrustListTestSupport.expiredCaCertificate;
import static org.keycloak.tests.broker.trust.TrustListTestSupport.publish;
import static org.keycloak.tests.broker.trust.TrustListTestSupport.service;
import static org.keycloak.tests.broker.trust.TrustListTestSupport.serviceWithStatus;
import static org.keycloak.tests.broker.trust.TrustListTestSupport.subordinateCaCertificate;
import static org.keycloak.tests.broker.trust.TrustListTestSupport.trustList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@KeycloakIntegrationTest(config = EtsiTrustListIdentityProviderTest.TrustListServerConfig.class)
public class EtsiTrustListIdentityProviderTest {

    private static final int REFRESH_INTERVAL_SECONDS = 600;
    private static final String REJECTED = "rejected";
    private static final String PUB_EAA_LOTE_TYPE = "http://uri.etsi.org/19602/LoTEType/EUPubEAAProvidersList";
    private static final String PUB_EAA_ISSUANCE_SERVICE = "http://uri.etsi.org/19602/SvcType/PubEAA/Issuance";
    private static final String NOTIFIED = "http://uri.etsi.org/19602/PubEAAProvidersList/SvcStatus/notified";
    private static final String WITHDRAWN = "http://uri.etsi.org/19602/PubEAAProvidersList/SvcStatus/withdrawn";
    private static final String SCHEME_ACTIVE = "https://scheme.example.test/status/active";

    private static final AtomicInteger IDS = new AtomicInteger();

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectHttpServer
    HttpServer httpServer;

    @InjectRealm
    ManagedRealm realm;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    private PublishedTrustList published;

    @Test
    public void listedCaCertificatesBecomeTrustAnchors() {
        String alias = createProvider(validList(service(ISSUANCE_SERVICE, Pki.SUBORDINATE_CA)));
        List<String> x5c = List.of(encode(Pki.LEAF));

        assertEquals(1, trustAnchorCount(alias));
        assertEquals(KeyUtils.createKeyId(Pki.LEAF_KEY.getPublic()), validatedLeafKeyId(alias, x5c));
    }

    @Test
    public void chainOfAnUnlistedCaIsRejected() {
        String alias = createProvider(validList(service(ISSUANCE_SERVICE, Pki.SUBORDINATE_CA)));

        assertChainRejected(alias, List.of(encode(Pki.LISTED_LEAF)));
    }

    @Test
    public void listedEndEntityCertificatesAreIgnored() {
        String alias = createProvider(validList(service(ISSUANCE_SERVICE, Pki.LISTED_LEAF)));

        assertTrustsNothing(alias);
        assertNull(validatedLeafKeyId(alias, List.of(encode(Pki.LISTED_LEAF))));
        assertNull(validatedLeafKeyId(alias, null));
    }

    @Test
    public void renewedCaCertificateSurvivesItsExpiredPredecessor() {
        KeyPair caKey = ecKeyPair();
        X509Certificate renewed = caCertificate(caKey, "Renewed CA");
        X509Certificate expired = expiredCaCertificate(caKey, "Renewed CA");
        KeyPair leafKey = ecKeyPair();
        X509Certificate leaf = endEntityCertificate(leafKey.getPublic(), "Renewed CA Leaf", renewed, caKey);
        String alias = createProvider(validList(service(ISSUANCE_SERVICE, renewed, expired)));

        assertEquals(1, trustAnchorCount(alias));
        assertEquals(KeyUtils.createKeyId(leafKey.getPublic()), validatedLeafKeyId(alias, List.of(encode(leaf))));
    }

    @Test
    public void caCertificateWithoutCertificateSigningDoesNotDisableTheList() {
        X509Certificate unusable = caCertificateWithoutCertificateSigning(ecKeyPair(), "CRL Only CA");
        String alias = createProvider(validList(service(ISSUANCE_SERVICE, unusable, Pki.SUBORDINATE_CA)));

        assertEquals(1, trustAnchorCount(alias));
    }

    @Test
    public void listMixingCertificatesTrustsOnlyItsCaCertificates() {
        String alias = createProvider(validList(service(ISSUANCE_SERVICE, Pki.SUBORDINATE_CA, Pki.LISTED_LEAF)));

        assertEquals(KeyUtils.createKeyId(Pki.LEAF_KEY.getPublic()), validatedLeafKeyId(alias, List.of(encode(Pki.LEAF))));
        assertChainRejected(alias, List.of(encode(Pki.LISTED_LEAF)));
        assertChainRejected(alias, null);
    }

    @Test
    public void onlyConfiguredServiceTypesAreInScope() {
        String alias = createProvider(validList(
                service(ISSUANCE_SERVICE, Pki.SUBORDINATE_CA),
                service(REVOCATION_SERVICE, Pki.ROOT)));

        assertEquals(1, trustAnchorCount(alias));
        assertChainRejected(alias, List.of(encode(Pki.LISTED_LEAF)));
    }

    @Test
    public void withdrawnPubEaaServicesAreIgnored() {
        String alias = createProvider(pubEaaList(), Map.of(
                EtsiTrustListIdentityProviderConfig.LOTE_TYPE, PUB_EAA_LOTE_TYPE,
                EtsiTrustListIdentityProviderConfig.SERVICE_TYPES, PUB_EAA_ISSUANCE_SERVICE,
                EtsiTrustListIdentityProviderConfig.SERVICE_STATUSES, NOTIFIED));

        assertEquals(1, trustAnchorCount(alias));
        assertChainRejected(alias, List.of(encode(Pki.LISTED_LEAF)));
    }

    @Test
    public void servicesWithAStatusAreIgnoredWithoutConfiguredStatuses() {
        String alias = createProvider(pubEaaList(), Map.of(
                EtsiTrustListIdentityProviderConfig.LOTE_TYPE, PUB_EAA_LOTE_TYPE,
                EtsiTrustListIdentityProviderConfig.SERVICE_TYPES, PUB_EAA_ISSUANCE_SERVICE));

        assertTrustsNothing(alias);
    }

    @Test
    public void onlyConfiguredServiceStatusesAreInScope() {
        String alias = createProvider(validList(
                serviceWithStatus(ISSUANCE_SERVICE, SCHEME_ACTIVE, Pki.SUBORDINATE_CA),
                serviceWithStatus(ISSUANCE_SERVICE, NOTIFIED, Pki.ROOT)),
                Map.of(EtsiTrustListIdentityProviderConfig.SERVICE_STATUSES, SCHEME_ACTIVE));

        assertEquals(1, trustAnchorCount(alias));
        assertChainRejected(alias, List.of(encode(Pki.LISTED_LEAF)));
    }

    @Test
    public void listOfAnotherLoteTypeIsRejected() {
        String alias = createProvider(trustList(Pki.SIGNER_KEY, "http://uri.etsi.org/19602/LoTEType/EUPIDProvidersList",
                nextUpdate(), List.of(service(ISSUANCE_SERVICE, Pki.SUBORDINATE_CA))));

        assertTrustsNothing(alias);
    }

    @Test
    public void listWithoutTheConfiguredSignatureIsRejected() {
        KeyPair attacker = ecKeyPair();
        String alias = createProvider(trustList(attacker, LOTE_TYPE, nextUpdate(), List.of(service(ISSUANCE_SERVICE, Pki.SUBORDINATE_CA))));

        assertTrustsNothing(alias);
    }

    @Test
    public void listWithoutNextUpdateTrustsNothing() {
        String alias = createProvider(trustList(Pki.SIGNER_KEY, LOTE_TYPE, null, List.of(service(ISSUANCE_SERVICE, Pki.SUBORDINATE_CA))));

        assertTrustsNothing(alias);
    }

    @Test
    public void listPastNextUpdateIsRejected() {
        String alias = createProvider(trustList(Pki.SIGNER_KEY, LOTE_TYPE, Instant.now().minusSeconds(60),
                List.of(service(ISSUANCE_SERVICE, Pki.SUBORDINATE_CA))));

        assertTrustsNothing(alias);
    }

    @Test
    public void refreshDropsAWithdrawnCertificate() {
        String alias = createProvider(validList(service(ISSUANCE_SERVICE, Pki.SUBORDINATE_CA)));
        assertEquals(1, trustAnchorCount(alias));

        published.replace(validList(service(ISSUANCE_SERVICE, Pki.LISTED_LEAF)));
        assertEquals(1, trustAnchorCount(alias));

        timeOffSet.set(REFRESH_INTERVAL_SECONDS + 1);
        assertEquals(0, trustAnchorCount(alias));
    }

    @Test
    public void failedRefreshRejectsTheListUntilAValidListIsAvailable() {
        String alias = createProvider(validList(service(ISSUANCE_SERVICE, Pki.SUBORDINATE_CA)));
        assertEquals(1, trustAnchorCount(alias));

        published.replace("not a trust list");
        timeOffSet.set(REFRESH_INTERVAL_SECONDS + 1);
        assertTrustsNothing(alias);

        published.replace(validList(service(ISSUANCE_SERVICE, Pki.SUBORDINATE_CA)));
        assertEquals(1, trustAnchorCount(alias));
    }

    @Test
    public void failedDownloadIsRetried() {
        String alias = createProvider("not a trust list");
        assertTrustsNothing(alias);

        published.replace(validList(service(ISSUANCE_SERVICE, Pki.SUBORDINATE_CA)));
        assertEquals(1, trustAnchorCount(alias));
    }

    @Test
    public void unavailableListDoesNotBlockOtherTrustProviders() {
        String unavailable = createProvider("not a trust list");
        String available = createProvider(validList(service(ISSUANCE_SERVICE, Pki.SUBORDINATE_CA)));
        String aliases = unavailable + "," + available;
        List<String> x5c = new ArrayList<>(List.of(encode(Pki.LEAF)));

        assertEquals(1, runOnServer.fetch(session -> (int) new TrustMaterialResolver()
                .resolveX509Trust(session, aliases, request()).count(), Integer.class));
        assertEquals(KeyUtils.createKeyId(Pki.LEAF_KEY.getPublic()), runOnServer.fetchString(session -> {
            try {
                return new TrustMaterialResolver().validateX509Chain(session, aliases, request(), x5c, Algorithm.ES256).getKeyId();
            } catch (VerificationException e) {
                return REJECTED;
            }
        }));
    }

    @Test
    public void adminApiRejectsIncompleteConfiguration() {
        String signingCertificates = PemUtils.addCertificateBeginEnd(encode(Pki.SIGNER));
        Map<String, String> valid = Map.of(
                EtsiTrustListIdentityProviderConfig.TRUST_LIST_URL, "https://lists.example.test/wallet.jwt",
                EtsiTrustListIdentityProviderConfig.SIGNING_CERTIFICATES, signingCertificates,
                EtsiTrustListIdentityProviderConfig.LOTE_TYPE, LOTE_TYPE,
                EtsiTrustListIdentityProviderConfig.SERVICE_TYPES, ISSUANCE_SERVICE);

        assertEquals(201, createWithConfig("etsi-valid", valid));
        for (String key : valid.keySet()) {
            Map<String, String> incomplete = new HashMap<>(valid);
            incomplete.remove(key);
            assertEquals(400, createWithConfig("etsi-without-" + key.toLowerCase(), incomplete), key);
        }

        Map<String, String> invalidInterval = new HashMap<>(valid);
        invalidInterval.put(EtsiTrustListIdentityProviderConfig.REFRESH_INTERVAL, "0");
        assertEquals(400, createWithConfig("etsi-invalid-interval", invalidInterval));

        Map<String, String> invalidCertificates = new HashMap<>(valid);
        invalidCertificates.put(EtsiTrustListIdentityProviderConfig.SIGNING_CERTIFICATES, "not a certificate");
        assertEquals(400, createWithConfig("etsi-invalid-certificates", invalidCertificates));
    }

    @SafeVarargs
    private static String validList(Map<String, Object>... services) {
        return trustList(Pki.SIGNER_KEY, LOTE_TYPE, nextUpdate(), List.of(services));
    }

    private static String pubEaaList() {
        return trustList(Pki.SIGNER_KEY, PUB_EAA_LOTE_TYPE, nextUpdate(), List.of(
                serviceWithStatus(PUB_EAA_ISSUANCE_SERVICE, NOTIFIED, Pki.SUBORDINATE_CA),
                serviceWithStatus(PUB_EAA_ISSUANCE_SERVICE, WITHDRAWN, Pki.ROOT)));
    }

    private static Instant nextUpdate() {
        return Instant.now().plus(Duration.ofHours(2));
    }

    private int createWithConfig(String alias, Map<String, String> config) {
        IdentityProviderRepresentation representation = new IdentityProviderRepresentation();
        representation.setAlias(alias);
        representation.setProviderId(EtsiTrustListIdentityProviderFactory.PROVIDER_ID);
        representation.setEnabled(true);
        representation.setConfig(config);
        try (Response response = realm.admin().identityProviders().create(representation)) {
            if (response.getStatus() == 201) {
                realm.cleanup().add(r -> r.identityProviders().get(alias).remove());
            }
            return response.getStatus();
        }
    }

    private String createProvider(String initialList) {
        return createProvider(initialList, Map.of());
    }

    private String createProvider(String initialList, Map<String, String> configOverrides) {
        String alias = "etsi-trust-list-" + IDS.incrementAndGet();
        published = publish(httpServer, "/" + alias, initialList);
        Map<String, String> config = new HashMap<>(Map.of(
                EtsiTrustListIdentityProviderConfig.TRUST_LIST_URL, published.url(),
                EtsiTrustListIdentityProviderConfig.SIGNING_CERTIFICATES, PemUtils.addCertificateBeginEnd(encode(Pki.SIGNER)),
                EtsiTrustListIdentityProviderConfig.LOTE_TYPE, LOTE_TYPE,
                EtsiTrustListIdentityProviderConfig.SERVICE_TYPES, ISSUANCE_SERVICE,
                EtsiTrustListIdentityProviderConfig.REFRESH_INTERVAL, String.valueOf(REFRESH_INTERVAL_SECONDS)));
        config.putAll(configOverrides);
        assertEquals(201, createWithConfig(alias, config));
        return alias;
    }

    private void assertTrustsNothing(String alias) {
        assertEquals(0, trustAnchorCount(alias));
        assertEquals(0, keyCount(alias));
    }

    private int trustAnchorCount(String alias) {
        return runOnServer.fetch(session -> provider(session, alias).resolveX509Trust(request())
                .map(X509TrustMaterial::trustAnchors)
                .mapToInt(Set::size)
                .sum(), Integer.class);
    }

    private int keyCount(String alias) {
        return runOnServer.fetch(session -> (int) provider(session, alias).resolveKeys(request()).count(), Integer.class);
    }

    private String validatedLeafKeyId(String alias, List<String> x5c) {
        List<String> chain = x5c == null ? null : new ArrayList<>(x5c);
        return runOnServer.fetchString(session -> {
            try {
                JWK key = provider(session, alias).validateX509Chain(request(), chain, Algorithm.ES256);
                return key == null ? null : key.getKeyId();
            } catch (VerificationException e) {
                return REJECTED;
            }
        });
    }

    private void assertChainRejected(String alias, List<String> x5c) {
        assertEquals(REJECTED, validatedLeafKeyId(alias, x5c));
    }

    private static TrustMaterialRequest request() {
        return TrustMaterialRequest.builder().kid("wallet-chosen-kid").algorithm(Algorithm.ES256).build();
    }

    private static TrustMaterialIdentityProvider<?> provider(KeycloakSession session, String alias) {
        RealmModel realm = session.getContext().getRealm();
        return IdentityBrokerService.getIdentityProvider(session, realm.getIdentityProviderByAlias(alias), TrustMaterialIdentityProvider.class);
    }

    // Kept apart from the test class, because code run on the server initializes the test class.
    private static final class Pki {

        private static final KeyPair SIGNER_KEY = ecKeyPair();
        private static final X509Certificate SIGNER = caCertificate(SIGNER_KEY, "LoTE Signer");
        private static final KeyPair ROOT_KEY = ecKeyPair();
        private static final X509Certificate ROOT = caCertificate(ROOT_KEY, "Wallet Provider Root");
        private static final KeyPair CA_KEY = ecKeyPair();
        private static final X509Certificate SUBORDINATE_CA = subordinateCaCertificate(CA_KEY.getPublic(), "Wallet Provider CA", ROOT, ROOT_KEY);
        private static final KeyPair LEAF_KEY = ecKeyPair();
        private static final X509Certificate LEAF = endEntityCertificate(LEAF_KEY.getPublic(), "Wallet Provider Leaf", SUBORDINATE_CA, CA_KEY);
        private static final KeyPair LISTED_KEY = ecKeyPair();
        private static final X509Certificate LISTED_LEAF = endEntityCertificate(LISTED_KEY.getPublic(), "Listed Leaf", ROOT, ROOT_KEY);
    }

    public static class TrustListServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_AUTH_ABCA);
        }
    }
}
