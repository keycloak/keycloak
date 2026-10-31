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

package org.keycloak.tests.conformance.oidc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.Algorithm;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.GeneratedRsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.conformance.OpenIdConformanceServer;
import org.keycloak.testframework.conformance.runner.BrowserInteraction;
import org.keycloak.testframework.conformance.runner.ConformanceModuleVariant;
import org.keycloak.testframework.conformance.runner.ConformanceResult;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.tests.conformance.ConformanceServerConfig;
import org.keycloak.tests.conformance.oidc.configs.OidcDynamicRealmConfig;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;

import static org.keycloak.tests.conformance.oidc.configs.OidcRealmConfig.DYNAMIC_PLAN;

/**
 * The Dynamic OP certification plan, run once per response type as the plan takes the response type as plan variant.
 * The suite registers its clients itself through Keycloak's dynamic client registration, with private_key_jwt client
 * authentication. Keycloak fetches the jwks_uri, request_uri and sector_identifier_uri these clients point at from the
 * suite (see {@link OpenIdConformanceServer#keycloakProxyMapping()}).
 *
 * The suite skips {@code oidcc-idtoken-unsigned}, as Keycloak does not issue unsigned ID tokens (alg none), which
 * servers are not required to support. The modules not depending on the response type run with {@code code} only.
 */
@KeycloakIntegrationTest(config = ConformanceServerConfig.class)
public class OidcDynamicTest extends AbstractOidcConformanceTest {

    private static final List<String> RESPONSE_TYPES = List.of(
            "code", "id_token", "id_token token", "code id_token", "code token", "code id_token token");

    private static final String SERVER_ROTATE_KEYS = "oidcc-server-rotate-keys";

    private static final Set<String> OPTIONAL_SCOPES = Set.of(OAuth2Constants.SCOPE_PROFILE, OAuth2Constants.SCOPE_EMAIL);

    @InjectRealm(config = OidcDynamicRealmConfig.class)
    ManagedRealm realm;

    private String realmWithOptionalScopes;

    /**
     * Makes the built-in profile and email client scopes optional client scopes of the registered clients, like for the
     * static clients of the other plans, as the suite warns about their claims in ID tokens it did not request them
     * for. A realm import only adds to the built-in default client scopes, hence the admin API. The realm is injected
     * before each test, so this runs once per realm.
     */
    @BeforeEach
    void profileAndEmailAsOptionalScopes() {
        if (realm.getId().equals(realmWithOptionalScopes)) {
            return;
        }
        RealmResource admin = realm.admin();
        admin.getDefaultDefaultClientScopes().stream()
                .filter(scope -> OPTIONAL_SCOPES.contains(scope.getName()))
                .forEach(scope -> {
                    admin.removeDefaultDefaultClientScope(scope.getId());
                    admin.addDefaultOptionalClientScope(scope.getId());
                });
        realmWithOptionalScopes = realm.getId();
    }

    @Override
    protected String plan() {
        return DYNAMIC_PLAN;
    }

    @Override
    protected Stream<ConformanceModuleVariant> moduleVariants() {
        return RESPONSE_TYPES.stream().flatMap(this::moduleVariants);
    }

    private Stream<ConformanceModuleVariant> moduleVariants(String responseType) {
        Map<String, String> planVariant = Map.of("response_type", responseType);
        boolean codeFlow = responseType.startsWith("code");
        List<Stream<ConformanceModuleVariant>> groups = new ArrayList<>();

        List<String> login = new ArrayList<>(List.of(
                "oidcc-server",
                "oidcc-idtoken-rs256",
                "oidcc-redirect-uri-query-OK",
                "oidcc-registration-sector-uri",
                "oidcc-request-uri-unsigned",
                "oidcc-request-uri-signed-rs256",
                "oidcc-ensure-request-object-with-redirect-uri",
                "oidcc-ensure-client-assertion-with-iss-aud-succeeds"));
        if (!responseType.equals("id_token")) {
            // only the response types issuing an access token for the userinfo endpoint
            login.add("oidcc-userinfo-rs256");
        }
        if (codeFlow) {
            // only the response types returning an authorization code, which the client exchanges for its tokens
            login.addAll(List.of(
                    "oidcc-registration-jwks-uri",
                    "oidcc-refresh-token",
                    "oidcc-refresh-token-rp-key-rotation"));
        }
        // The registered clients require consent, under Keycloak's consent required client registration policy
        groups.add(modules(planVariant, ConformanceResult.PASSED, BrowserInteraction.LOGIN_WITH_CONSENT,
                login.toArray(String[]::new)));
        // Rejected by the registration endpoint, without any authorization
        groups.add(modules(planVariant, ConformanceResult.PASSED, BrowserInteraction.NONE,
                "oidcc-redirect-uri-regfrag",
                "oidcc-registration-sector-bad"));
        // Keycloak renders its error page instead of redirecting to a redirect_uri not registered or missing for a
        // client with several, which the suite snapshots for review
        groups.add(modules(planVariant, ConformanceResult.REVIEW, BrowserInteraction.errorPage("Invalid parameter: redirect_uri"),
                "oidcc-ensure-redirect-uri-in-authorization-request",
                "oidcc-redirect-uri-query-mismatch",
                "oidcc-redirect-uri-query-added"));
        // The consent page shows the logo, terms of service and policy the client registered, which the suite
        // snapshots for review
        groups.add(modules(planVariant, ConformanceResult.REVIEW, BrowserInteraction.CONSENT_PAGE_SCREENSHOT,
                "oidcc-registration-logo-uri",
                "oidcc-registration-policy-uri",
                "oidcc-registration-tos-uri"));
        if (responseType.equals("code") || responseType.equals("code token")) {
            // only the response types where the ID token comes from the token endpoint
            groups.add(modules(planVariant, ConformanceResult.SKIPPED, BrowserInteraction.NONE,
                    "oidcc-idtoken-unsigned"));
        }
        if (responseType.equals("code")) {
            // The modules not depending on the response type
            groups.add(modules(planVariant, ConformanceResult.PASSED, BrowserInteraction.NONE,
                    "oidcc-discovery-endpoint-verification",
                    SERVER_ROTATE_KEYS));
        }
        return groups.stream().flatMap(group -> group);
    }

    @Override
    protected JsonNode suiteConfig(ConformanceModuleVariant module) {
        return OidcSuiteConfig.createDynamic(OpenIdConformanceServer.KEYCLOAK_BASE_URI, module.browserInteraction()).toJson();
    }

    /**
     * The key rotation module fetches the realm's keys while it is configured and expects a new key, besides the old
     * one, once it is started, so the realm gets a new RS256 key in between. Its priority is lower than the one of the
     * realm's key, which keeps signing the tokens.
     */
    @Override
    protected Runnable beforeStart(ConformanceModuleVariant moduleVariant) {
        return SERVER_ROTATE_KEYS.equals(moduleVariant.name()) ? this::rotateSigningKeys : null;
    }

    private void rotateSigningKeys() {
        ComponentRepresentation key = new ComponentRepresentation();
        key.setName("conformance-rotated-rs256-key-" + System.currentTimeMillis());
        key.setProviderId(GeneratedRsaKeyProviderFactory.ID);
        key.setProviderType(KeyProvider.class.getName());
        key.setParentId(realm.admin().toRepresentation().getId());
        key.setConfig(new MultivaluedHashMap<>());
        key.getConfig().putSingle(Attributes.PRIORITY_KEY, "50");
        key.getConfig().putSingle(Attributes.ENABLED_KEY, "true");
        key.getConfig().putSingle(Attributes.ACTIVE_KEY, "true");
        key.getConfig().putSingle(Attributes.ALGORITHM_KEY, Algorithm.RS256);
        try (Response response = realm.admin().components().add(key)) {
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                throw new IllegalStateException("Failed to add a signing key to rotate to: HTTP " + response.getStatus());
            }
        }
    }
}
