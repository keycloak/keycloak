/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.account;

import java.util.Map;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.tests.utils.admin.AdminApiUtil;

/**
 * Shared base for Account REST API tests.
 */
public abstract class AbstractRestServiceTest {

    static final String APP_ROOT = "http://localhost:8500/app";
    protected static final String IN_USE_CLIENT_APP_URI = APP_ROOT + "/in-use-client";
    protected static final String OFFLINE_CLIENT_APP_URI = APP_ROOT + "/offline-client";
    protected static final String ALWAYS_DISPLAY_CLIENT_APP_URI = APP_ROOT + "/always-display-client";

    @InjectRealm(config = AccountRestRealmConfig.class)
    protected ManagedRealm managedRealm;

    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    protected OAuthClient oauth;

    @InjectSimpleHttp
    protected SimpleHttp simpleHttp;

    @InjectEvents
    protected Events events;

    protected String apiVersion;

    protected String getAccountRootUrl() {
        return managedRealm.getBaseUrl() + "/account";
    }

    protected String getAccountUrl(String resource) {
        String url = getAccountRootUrl();
        if (apiVersion != null) {
            url += "/" + apiVersion;
        }
        if (resource != null) {
            url += "/" + resource;
        }
        return url;
    }

    protected static void setRequiredActionEnabled(RealmResource realm, UserModel.RequiredAction action, boolean enabled) {
        RequiredActionProviderRepresentation requiredAction = realm.flows().getRequiredActions().stream()
                .filter(a -> action.name().equals(a.getAlias()))
                .findAny().orElseThrow(() -> new IllegalStateException("Required action not found: " + action.name()));
        requiredAction.setEnabled(enabled);
        realm.flows().updateRequiredAction(requiredAction.getAlias(), requiredAction);
    }

    protected void registerUserCleanup(String username) {
        UserResource userResource = AdminApiUtil.findUserByUsernameId(managedRealm.admin(), username);
        UserRepresentation original = userResource.toRepresentation();
        managedRealm.cleanup().add(r -> r.users().get(original.getId()).update(original));
    }

    public static class AccountRestRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .name("test")
                    .editUsernameAllowed(true)
                    .users(
                            // default user. testGetProfile asserts emailVerified == false.
                            // The attr_required/* attributes satisfy UP_CONFIG_FOR_METADATA so
                            // setting that UP config doesn't add an UPDATE_PROFILE required action
                            // that would block subsequent password-grant logins.
                            UserBuilder.create().username("test-user@localhost")
                                    .email("test-user@localhost")
                                    .name("Tom", "Brady")
                                    .attribute("attr_required", "value")
                                    .attribute("attr_required_by_role", "value")
                                    .attribute("attr_required_by_scope", "value")
                                    .realmRoles("user", "offline_access")
                                    .clientRoles("account", "view-profile", "manage-account")
                                    .password("password"),
                            // used by testUpdateProfile for the email/username clash check
                            UserBuilder.create().username("john-doh@localhost")
                                    .email("john-doh@localhost")
                                    .name("John", "Doh")
                                    .password("password"),
                            UserBuilder.create().username("no-account-access")
                                    .email("no-account-access@localhost")
                                    .name("No", "Access")
                                    .emailVerified(true)
                                    .password("password"),
                            UserBuilder.create().username("view-account-access")
                                    .email("view-account-access@localhost")
                                    .name("View", "Account")
                                    .emailVerified(true)
                                    .clientRoles("account", "view-profile")
                                    .password("password"),
                            UserBuilder.create().username("view-applications-access")
                                    .email("view-applications-access@localhost")
                                    .name("View", "Applications")
                                    .emailVerified(true)
                                    .realmRoles("user", "offline_access")
                                    .clientRoles("account", "view-applications", "manage-consent")
                                    .password("password"),
                            UserBuilder.create().username("view-consent-access")
                                    .email("view-consent-access@localhost")
                                    .name("View", "Consent")
                                    .emailVerified(true)
                                    .clientRoles("account", "view-consent")
                                    .password("password"),
                            UserBuilder.create().username("manage-consent-access")
                                    .email("manage-consent-access@localhost")
                                    .name("Manage", "Consent")
                                    .emailVerified(true)
                                    .clientRoles("account", "manage-consent", "view-profile")
                                    .password("password"),
                            UserBuilder.create().username("manage-account-access")
                                    .email("manage-account-access@localhost")
                                    .name("Manage", "Account")
                                    .emailVerified(true)
                                    .realmRoles("user", "offline_access")
                                    .clientRoles("account", "view-profile", "manage-account")
                                    .password("password"),
                            // used by testCRUDCredentialOfDifferentUser
                            UserBuilder.create().username("user-with-one-configured-otp")
                                    .email("otp1@redhat.com")
                                    .password("password")
                                    .totpSecret("DJmQfC73VGFhw7D4QJ8A")
                    )
                    .clients(
                            ClientBuilder.create("in-use-client")
                                    .id(KeycloakModelUtils.generateId())
                                    .name("In Use Client")
                                    .baseUrl(IN_USE_CLIENT_APP_URI)
                                    .directAccessGrantsEnabled()
                                    .secret("secret1"),
                            ClientBuilder.create("offline-client")
                                    .id(KeycloakModelUtils.generateId())
                                    .name("Offline Client")
                                    .baseUrl(OFFLINE_CLIENT_APP_URI)
                                    .directAccessGrantsEnabled()
                                    .secret("secret1"),
                            ClientBuilder.create("offline-client-without-base-url")
                                    .id(KeycloakModelUtils.generateId())
                                    .name("Offline Client Without Base URL")
                                    .directAccessGrantsEnabled()
                                    .secret("secret1"),
                            ClientBuilder.create("always-display-client")
                                    .id(KeycloakModelUtils.generateId())
                                    .name("Always Display Client")
                                    .baseUrl(ALWAYS_DISPLAY_CLIENT_APP_URI)
                                    .directAccessGrantsEnabled()
                                    .alwaysDisplayInConsole(true)
                                    .secret("secret1"),
                            ClientBuilder.create("direct-grant")
                                    .id(KeycloakModelUtils.generateId())
                                    .directAccessGrantsEnabled()
                                    .secret("password")
                                    // testCors needs this origin whitelisted on the token issuer
                                    .webOrigins("http://localtest.me:8180")
                                    .protocolMappers(
                                            audienceMapper("aud-account", "account"),
                                            audienceMapper("aud-admin", "security-admin-console")
                                    ),
                            ClientBuilder.create("root-url-client")
                                    .id(KeycloakModelUtils.generateId())
                                    .rootUrl("http://localhost:8180/foo/bar")
                                    .adminUrl("http://localhost:8180/foo/bar")
                                    .baseUrl("/baz")
                                    .redirectUris(
                                            "http://localhost:8180/foo/bar/*",
                                            "https://localhost:8543/foo/bar/*"
                                    )
                                    .directAccessGrantsEnabled()
                                    .secret("password"),
                            ClientBuilder.create("third-party")
                                    .id(KeycloakModelUtils.generateId())
                                    .description("A third party application")
                                    .baseUrl("http://localhost:8180/auth/realms/master/app/auth")
                                    .redirectUris(
                                            "http://localhost:8180/auth/realms/master/app/*",
                                            "https://localhost:8543/auth/realms/master/app/*"
                                    )
                                    .consentRequired(true)
                                    .secret("password"),
                            ClientBuilder.create("custom-audience")
                                    .id(KeycloakModelUtils.generateId())
                                    .directAccessGrantsEnabled()
                                    .secret("password")
                                    // drop "roles" scope so audience comes only from the custom mapper;
                                    // add role mappers back so role claims still reach the token
                                    .defaultClientScopes("web-origins", "profile", "email")
                                    .protocolMappers(
                                            customAudienceMapper("aud", "foo-bar"),
                                            clientRoleMapper(),
                                            realmRoleMapper()
                                    )
                    );
        }

        private static ProtocolMapperRepresentation audienceMapper(String name, String includedClientAudience) {
            ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
            mapper.setName(name);
            mapper.setProtocol("openid-connect");
            mapper.setProtocolMapper("oidc-audience-mapper");
            mapper.setConfig(Map.of(
                    "included.client.audience", includedClientAudience,
                    "id.token.claim", "true",
                    "access.token.claim", "true"
            ));
            return mapper;
        }

        private static ProtocolMapperRepresentation customAudienceMapper(String name, String includedCustomAudience) {
            ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
            mapper.setName(name);
            mapper.setProtocol("openid-connect");
            mapper.setProtocolMapper("oidc-audience-mapper");
            mapper.setConfig(Map.of(
                    "included.custom.audience", includedCustomAudience,
                    "id.token.claim", "true",
                    "access.token.claim", "true"
            ));
            return mapper;
        }

        private static ProtocolMapperRepresentation clientRoleMapper() {
            ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
            mapper.setName("client roles");
            mapper.setProtocol("openid-connect");
            mapper.setProtocolMapper("oidc-usermodel-client-role-mapper");
            mapper.setConfig(Map.of(
                    "user.attribute", "foo",
                    "access.token.claim", "true",
                    "claim.name", "resource_access.${client_id}.roles",
                    "jsonType.label", "String",
                    "multivalued", "true"
            ));
            return mapper;
        }

        private static ProtocolMapperRepresentation realmRoleMapper() {
            ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
            mapper.setName("realm roles");
            mapper.setProtocol("openid-connect");
            mapper.setProtocolMapper("oidc-usermodel-realm-role-mapper");
            mapper.setConfig(Map.of(
                    "user.attribute", "foo",
                    "access.token.claim", "true",
                    "claim.name", "realm_access.roles",
                    "jsonType.label", "String",
                    "multivalued", "true"
            ));
            return mapper;
        }
    }
}
