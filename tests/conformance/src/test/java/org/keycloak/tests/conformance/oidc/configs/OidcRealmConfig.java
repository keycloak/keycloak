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

package org.keycloak.tests.conformance.oidc.configs;

import java.util.List;
import java.util.Map;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testframework.conformance.OpenIdConformanceServer;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.tests.conformance.ConformanceRealmUtil;
import org.keycloak.userprofile.DeclarativeUserProfileProvider;
import org.keycloak.userprofile.DeclarativeUserProfileProviderFactory;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.config.UPConfigUtils;
import org.keycloak.util.JsonSerialization;

/**
 * The OpenID Connect Core conformance realm, shared by the Config, Basic, Implicit and Hybrid certification plans.
 * Both clients authenticate with a client secret (the client-secret authenticator accepts client_secret_basic and
 * client_secret_post) and allow the implicit flow, which the Implicit and Hybrid plans need.
 *
 * The standard claim scopes are optional, so claims are only returned when their scope is requested, as the suite
 * warns about unrequested claims in the ID token. The user carries a value for every standard claim; most of them are
 * mapped from user attributes that the default user profile does not declare, so the realm enables unmanaged
 * attributes to keep them on the imported user.
 */
public class OidcRealmConfig implements RealmConfig {

    public static final String REALM = "oidcc";
    public static final String USER = "john";
    public static final String PASSWORD = "password";
    public static final String CLIENT = "oidcc-client";
    // At least 32 bytes, as the suite signs HS256 tokens with the client secret
    public static final String CLIENT_SECRET = "oidcc-client-secret-0123456789abcdef";
    public static final String CLIENT2 = "oidcc-client2";
    public static final String CLIENT2_SECRET = "oidcc-client2-secret-0123456789abcdef";
    public static final String CONFORMANCE_CALLBACK = OpenIdConformanceServer.INTERNAL_BASE_URI + "/test/a/keycloak/callback";

    public static final String CONFIG_PLAN = "oidcc-config-certification-test-plan";
    public static final String BASIC_PLAN = "oidcc-basic-certification-test-plan";
    public static final String IMPLICIT_PLAN = "oidcc-implicit-certification-test-plan";
    public static final String HYBRID_PLAN = "oidcc-hybrid-certification-test-plan";
    public static final String FORM_POST_BASIC_PLAN = "oidcc-formpost-basic-certification-test-plan";
    public static final String FORM_POST_IMPLICIT_PLAN = "oidcc-formpost-implicit-certification-test-plan";
    public static final String FORM_POST_HYBRID_PLAN = "oidcc-formpost-hybrid-certification-test-plan";
    public static final String DYNAMIC_PLAN = "oidcc-dynamic-certification-test-plan";

    @Override
    public RealmBuilder configure(RealmBuilder realm) {
        return realm.name(REALM)
                .eventsEnabled(true)
                .eventsListeners("jboss-logging")
                .users(user(REALM))
                .clients(oidccClient(CLIENT, CLIENT_SECRET), oidccClient(CLIENT2, CLIENT2_SECRET))
                .update(OidcRealmConfig::enableUnmanagedAttributes);
    }

    /**
     * The user the suite logs in as, with the claims of all the scopes the modules request.
     */
    public static UserBuilder user(String realm) {
        return UserBuilder.create()
                        .username(USER)
                        .enabled(true)
                        .email("john@example.test")
                        .emailVerified(true)
                        .firstName("John")
                        .lastName("Doe")
                        .password(PASSWORD)
                        // the default roles grant offline_access, which the refresh token modules request
                        .realmRoles(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm)
                        .attribute("middleName", "Q")
                        .attribute("nickname", "Johnny")
                        .attribute("profile", "https://example.test/john")
                        .attribute("picture", "https://example.test/john.png")
                        .attribute("website", "https://example.test")
                        .attribute("gender", "male")
                        .attribute("birthdate", "1980-01-01")
                        .attribute("zoneinfo", "Europe/London")
                        .attribute("locale", "en")
                        .attribute("updatedAt", "1700000000")
                        .attribute("phoneNumber", "+1 555 0100")
                        .attribute("phoneNumberVerified", "true")
                        .attribute("street", "221B Baker Street")
                        .attribute("locality", "London")
                        .attribute("region", "Greater London")
                        .attribute("postal_code", "NW1 6XE")
                        .attribute("country", "UK");
    }

    private static ClientBuilder oidccClient(String clientId, String secret) {
        return ClientBuilder.create(clientId)
                .secret(secret)
                .serviceAccountsEnabled(false)
                .directAccessGrantsEnabled(false)
                .implicitFlowEnabled(true)
                .defaultClientScopes("basic", "acr", "roles", "web-origins")
                .optionalClientScopes("profile", "email", "address", "phone", "offline_access")
                .protocolMappers(
                        ConformanceRealmUtil.idTokenScriptMapper("conformance id token claims", "oidc-id-token-claims.js"),
                        ConformanceRealmUtil.claimsParameterMapper())
                .redirectUris(CONFORMANCE_CALLBACK)
                .webOrigins(OpenIdConformanceServer.INTERNAL_BASE_URI.toString());
    }

    public static void enableUnmanagedAttributes(RealmRepresentation rep) {
        UPConfig upConfig = UPConfigUtils.parseSystemDefaultConfig();
        upConfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);

        ComponentExportRepresentation userProfile = new ComponentExportRepresentation();
        userProfile.setProviderId(DeclarativeUserProfileProviderFactory.ID);
        userProfile.setConfig(new MultivaluedHashMap<>(Map.of(
                DeclarativeUserProfileProvider.UP_COMPONENT_CONFIG_KEY,
                List.of(JsonSerialization.valueAsString(upConfig)))));

        MultivaluedHashMap<String, ComponentExportRepresentation> components = rep.getComponents();
        if (components == null) {
            components = new MultivaluedHashMap<>();
            rep.setComponents(components);
        }
        components.add(UserProfileProvider.class.getName(), userProfile);
    }
}
