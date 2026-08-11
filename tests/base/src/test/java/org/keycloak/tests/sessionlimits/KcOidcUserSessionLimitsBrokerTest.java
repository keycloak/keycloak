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
package org.keycloak.tests.sessionlimits;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;

import org.junit.jupiter.api.Assertions;

import static org.keycloak.broker.oidc.OAuth2IdentityProviderConfig.TOKEN_ENDPOINT_URL;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.CONSUMER_CLIENT_ID;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.CONSUMER_CLIENT_SECRET;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.CONSUMER_REALM;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.PROVIDER_REALM;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.USER_EMAIL;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.USER_LOGIN;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.USER_PASSWORD;

@KeycloakIntegrationTest
public class KcOidcUserSessionLimitsBrokerTest extends AbstractUserSessionLimitsBrokerTest {

    private static final String IDP_ALIAS = "kc-oidc-idp";
    private static final String BROKER_CLIENT_ID = "brokerapp";
    private static final String BROKER_CLIENT_SECRET = "secret";

    @InjectRealm(ref = "provider", lifecycle = LifeCycle.METHOD, config = ProviderRealmConfig.class)
    ManagedRealm providerRealm;

    @InjectRealm(ref = "consumer", lifecycle = LifeCycle.METHOD, config = ConsumerRealmConfig.class)
    ManagedRealm consumerRealm;

    @Override
    protected String getIdpAlias() {
        return IDP_ALIAS;
    }

    @Override
    protected ManagedRealm getConsumerRealm() {
        return consumerRealm;
    }

    @Override
    protected ManagedRealm getProviderRealm() {
        return providerRealm;
    }

    @Override
    protected void logInAsUserInIDPForFirstTime() {
        logInAsUserInIDP();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
    }

    public static class ProviderRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name(PROVIDER_REALM);
            realm.users(UserBuilder.create(USER_LOGIN)
                    .name("Firstname", "Lastname")
                    .email(USER_EMAIL)
                    .emailVerified(true)
                    .password(USER_PASSWORD)
                    .enabled(true));
            realm.clients(ClientBuilder.create(BROKER_CLIENT_ID)
                    .secret(BROKER_CLIENT_SECRET)
                    .redirectUris("http://localhost:8080/realms/" + CONSUMER_REALM + "/broker/" + IDP_ALIAS + "/endpoint/*"));
            return realm;
        }
    }

    public static class ConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name(CONSUMER_REALM);
            realm.identityProviders(IdentityProviderBuilder.create()
                    .providerId(OIDCIdentityProviderFactory.PROVIDER_ID)
                    .alias(IDP_ALIAS)
                    .attribute(IdentityProviderModel.SYNC_MODE, "IMPORT")
                    .attribute("clientId", BROKER_CLIENT_ID)
                    .attribute("clientSecret", BROKER_CLIENT_SECRET)
                    .attribute(OIDCIdentityProviderConfig.ISSUER, "http://localhost:8080/realms/" + PROVIDER_REALM)
                    .attribute("authorizationUrl", "http://localhost:8080/realms/" + PROVIDER_REALM + "/protocol/openid-connect/auth")
                    .attribute(TOKEN_ENDPOINT_URL, "http://localhost:8080/realms/" + PROVIDER_REALM + "/protocol/openid-connect/token")
                    .attribute("logoutUrl", "http://localhost:8080/realms/" + PROVIDER_REALM + "/protocol/openid-connect/logout")
                    .attribute("userInfoUrl", "http://localhost:8080/realms/" + PROVIDER_REALM + "/protocol/openid-connect/userinfo")
                    .attribute(OIDCIdentityProviderConfig.JWKS_URL, "http://localhost:8080/realms/" + PROVIDER_REALM + "/protocol/openid-connect/certs")
                    .attribute(OIDCIdentityProviderConfig.USE_JWKS_URL, "true")
                    .attribute(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE, "true")
                    .attribute("backchannelSupported", "true")
                    .attribute("defaultScope", "email profile")
                    .build());
            realm.clients(ClientBuilder.create(CONSUMER_CLIENT_ID)
                    .secret(CONSUMER_CLIENT_SECRET)
                    .directAccessGrantsEnabled(true)
                    .redirectUris("*"));
            return realm;
        }
    }
}
