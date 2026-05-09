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

import jakarta.ws.rs.core.Response;

import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.OrganizationModel.IdentityProviderRedirectMode;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginUsernamePage;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;

@KeycloakIntegrationTest
@DisplayName("AIA Re-authentication with Brokered Users when organizations are enabled")
public class OrganizationBrokerReAuthTest extends AbstractBrokerReAuthTest {

    private static final String ORG_NAME = "neworg";
    private static final String ORG_DOMAIN = "neworg.org"; // must match USER_EMAIL domain

    @InjectRealm(ref = CONSUMER_REALM_NAME, config = ConsumerRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm consumerRealm;

    @InjectPage
    LoginUsernamePage loginUsernamePage;

    @Override
    protected ManagedRealm getConsumerRealm() {
        return consumerRealm;
    }

    @Override
    protected Instant performFirstBrokerLogin(boolean hideOnLogin) {
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setName(ORG_NAME);
        org.setAlias(ORG_NAME);
        org.addDomain(new OrganizationDomainRepresentation(ORG_DOMAIN));
        String orgId;
        try (Response response = consumerRealm.admin().organizations().create(org)) {
            orgId = ApiUtil.getCreatedId(response);
        }
        consumerRealm.cleanup().add(r -> {
            try { r.organizations().get(orgId).delete().close(); } catch (Exception ignored) {}
        });

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
                .attribute(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, ORG_DOMAIN)
                .attribute(IdentityProviderRedirectMode.EMAIL_MATCH.getKey(), Boolean.TRUE.toString())
                .build();
        idp.setHideOnLogin(hideOnLogin);

        consumerRealm.admin().identityProviders().create(idp).close();
        consumerRealm.cleanup().add(r -> r.identityProviders().get(IDP_ALIAS).remove());
        consumerRealm.admin().organizations().get(orgId).identityProviders().addIdentityProvider(IDP_ALIAS).close();

        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(USER_EMAIL);
        loginUsernamePage.submit();
        Assertions.assertTrue(driver.getCurrentUrl().contains("/realms/" + PROVIDER_REALM_NAME + "/"),
                "Should be redirected to provider realm for first broker login");
        loginPage.fillLogin(USER_LOGIN, USER_PASSWORD);
        loginPage.submit();
        return Instant.now();
    }

    static class ConsumerRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.organizationsEnabled(true);
        }
    }
}
