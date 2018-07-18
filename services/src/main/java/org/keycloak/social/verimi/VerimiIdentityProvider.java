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
package org.keycloak.social.verimi;

import java.io.IOException;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.UriBuilder;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * @author keycloak@rlfnb.de
 */
public class VerimiIdentityProvider extends AbstractOAuth2IdentityProvider<VerimiIdentityProviderConfig> implements SocialIdentityProvider<VerimiIdentityProviderConfig> {

    public static final String DEFAULT_SCOPE = "login";

    public VerimiIdentityProvider(KeycloakSession session, VerimiIdentityProviderConfig config) {
        super(session, config);
        config.setDefaultScope(getDefaultScopes());
    }

    protected class VerimiEndpoint extends Endpoint {

        public VerimiEndpoint(AuthenticationCallback callback, RealmModel realm, EventBuilder event) {
            super(callback, realm, event);
        }

        @Override
        protected String generateTokenRequestAsString(String authorizationCode) throws IOException {
            return super.generateTokenRequestAsString(authorizationCode);
        }

    }

    @Override
    protected UriBuilder createAuthorizationUrl(AuthenticationRequest request) {
        UriBuilder uriBuilder = super.createAuthorizationUrl(request);
        if (getConfig().isReadBasket() && getConfig().getData() != null) {
            uriBuilder.queryParam("data", getConfig().getData());
        }
        return uriBuilder;
    }

    @Override

    public BrokeredIdentityContext getFederatedIdentity(String response) {
        JsonObject tokenInfo = Json.createReader(new StringReader(response)).readObject();

        String accessToken = tokenInfo.getString(OAUTH2_PARAMETER_ACCESS_TOKEN);
        String refreshToken = tokenInfo.getString(OAUTH2_PARAMETER_REFRESH_TOKEN);
        String enc_idToken = tokenInfo.getString(OAUTH2_PARAMETER_ID_TOKEN);
        JWSInput input = null;
        try {
            input = new JWSInput(enc_idToken);
        } catch (JWSInputException ex) {
            logger.fatal("could not parse ID-TOKEN", ex);
        }
        JsonObject idToken = Json.createReader(new StringReader(new String(input.getContent()))).readObject();

        if (idToken.getString("sub") == null) {
            throw new IdentityBrokerException("No ID token available in OAuth server response: " + response);
        }
        BrokeredIdentityContext context = new BrokeredIdentityContext(idToken.getString("sub"));
        context.setBrokerUserId(idToken.getString("sub"));
        context.getContextData().put(EXTERNAL_IDENTITY_PROVIDER, VerimiIdentityProviderFactory.PROVIDER_ID);
        context.getContextData().put(FEDERATED_ACCESS_TOKEN, accessToken);
        context.getContextData().put(FEDERATED_REFRESH_TOKEN, refreshToken);

        return context;
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new VerimiEndpoint(callback, realm, event);
    }

    @Override
    protected String getDefaultScopes() {
        return DEFAULT_SCOPE;
    }
}
