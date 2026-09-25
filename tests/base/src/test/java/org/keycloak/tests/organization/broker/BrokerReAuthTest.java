/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.organization.broker;

import java.time.Instant;

import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;

@KeycloakIntegrationTest
@DisplayName("AIA Re-authentication with Brokered Users when organizations are disabled")
public class BrokerReAuthTest extends AbstractBrokerReAuthTest {

    @InjectRealm(ref = CONSUMER_REALM_NAME, lifecycle = LifeCycle.METHOD)
    ManagedRealm consumerRealm;

    @Override
    protected ManagedRealm getConsumerRealm() {
        return consumerRealm;
    }

    @Override
    protected Instant performFirstBrokerLogin(boolean hideIdpAfterFirstLogin) {
        // IdP is always created visible; hiding happens after the first login if requested
        IdentityProviderRepresentation idp = IdentityProviderBuilder.create()
                .providerId(OIDCIdentityProviderFactory.PROVIDER_ID)
                .alias(IDP_ALIAS)
                .attribute("clientId", IDP_CLIENT_ID)
                .attribute("clientSecret", IDP_CLIENT_SECRET)
                .attribute(IdentityProviderModel.SYNC_MODE, "IMPORT")
                .attribute("authorizationUrl", BASE_URL + "/realms/" + PROVIDER_REALM_NAME + "/protocol/openid-connect/auth")
                .attribute("tokenUrl", BASE_URL + "/realms/" + PROVIDER_REALM_NAME + "/protocol/openid-connect/token")
                .attribute("jwksUrl", BASE_URL + "/realms/" + PROVIDER_REALM_NAME + "/protocol/openid-connect/certs")
                .attribute("logoutUrl", BASE_URL + "/realms/" + PROVIDER_REALM_NAME + "/protocol/openid-connect/logout")
                .build();

        consumerRealm.admin().identityProviders().create(idp).close();
        consumerRealm.cleanup().add(r -> r.identityProviders().get(IDP_ALIAS).remove());

        oauth.openLoginForm();
        loginPage.assertCurrent();
        loginPage.findSocialButton(IDP_ALIAS).click();
        Assertions.assertTrue(driver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM_NAME + "/"),
                "Should be redirected to provider realm for first broker login");
        loginPage.fillLogin(USER_LOGIN, USER_PASSWORD);
        loginPage.submit();
        Instant instant = Instant.now();

        // Simulates an admin hiding an IdP after users are already federated to it
        if (hideIdpAfterFirstLogin) {
            IdentityProviderRepresentation rep = consumerRealm.admin().identityProviders().get(IDP_ALIAS).toRepresentation();
            rep.setHideOnLogin(true);
            consumerRealm.admin().identityProviders().get(IDP_ALIAS).update(rep);
        }

        return instant;
    }
}
