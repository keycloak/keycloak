/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.broker;

import java.util.Map;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.IdentityProviderRepresentation;

import static org.keycloak.broker.oidc.OAuth2IdentityProviderConfig.TOKEN_ENDPOINT_URL;
import static org.keycloak.testsuite.broker.BrokerTestConstants.CLIENT_SECRET;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_ALIAS;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_PROVIDER_ID;
import static org.keycloak.testsuite.broker.BrokerTestConstants.REALM_PROV_NAME;
import static org.keycloak.testsuite.broker.BrokerTestTools.createIdentityProvider;
import static org.keycloak.testsuite.broker.BrokerTestTools.getProviderRoot;

public class KcOidcBrokerColonAliasClientSecretBasicAuthTest extends AbstractBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerColonAliasClientSecretBasicAuthTest.KcOidcBrokerColonAliasConfigurationWithBasicAuthAuthentication();
    }

    private class KcOidcBrokerColonAliasConfigurationWithBasicAuthAuthentication extends KcOidcBrokerConfiguration {

        public final static String CLIENT_ID_COLON = "https://kc-dev.general.gr/staging/realms/general";

        @Override
        public IdentityProviderRepresentation setUpIdentityProvider(IdentityProviderSyncMode syncMode) {
            IdentityProviderRepresentation idp = createIdentityProvider(IDP_OIDC_ALIAS, IDP_OIDC_PROVIDER_ID);
            Map<String, String> config = idp.getConfig();
            applyDefaultConfiguration(config, syncMode);
            config.put("clientAuthMethod", OIDCLoginProtocol.CLIENT_SECRET_BASIC);
            return idp;
        }

        @Override
        protected void applyDefaultConfiguration(final Map<String, String> config, IdentityProviderSyncMode syncMode) {
            config.put(IdentityProviderModel.SYNC_MODE, syncMode.toString());
            config.put("clientId", CLIENT_ID_COLON);
            config.put("clientSecret", CLIENT_SECRET);
            config.put("prompt", "login");
            config.put("loginHint", "true");
            config.put(OIDCIdentityProviderConfig.ISSUER, getProviderRoot() + "/auth/realms/" + REALM_PROV_NAME);
            config.put("authorizationUrl", getProviderRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/auth");
            config.put(TOKEN_ENDPOINT_URL, getProviderRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/token");
            config.put("logoutUrl", getProviderRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/logout");
            config.put("userInfoUrl", getProviderRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/userinfo");
            config.put("defaultScope", "email profile");
            config.put("backchannelSupported", "true");
            config.put(OIDCIdentityProviderConfig.JWKS_URL,
                    getProviderRoot() + "/auth/realms/" + REALM_PROV_NAME + "/protocol/openid-connect/certs");
            config.put(OIDCIdentityProviderConfig.USE_JWKS_URL, "true");
            config.put(OIDCIdentityProviderConfig.VALIDATE_SIGNATURE, "true");
        }

        @Override
        public String getIDPClientIdInProviderRealm() {
            return CLIENT_ID_COLON;
        }

    }
}
