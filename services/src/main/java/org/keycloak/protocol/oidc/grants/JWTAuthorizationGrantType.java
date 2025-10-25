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

package org.keycloak.protocol.oidc.grants;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.JWTAuthorizationGrantProvider;
import org.keycloak.broker.provider.UserAuthenticationIdentityProvider;
import org.keycloak.cache.AlternativeLookupProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import jakarta.ws.rs.core.Response;
import java.util.List;

public class JWTAuthorizationGrantType extends OAuth2GrantTypeBase {

    @Override
    public Response process(Context context) {
        setContext(context);

        String assertion = formParams.getFirst(OAuth2Constants.ASSERTION);
        String expectedAudience = Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName());

        try {

            JWTAuthorizationGrantValidator authorizationGrantContext = JWTAuthorizationGrantValidator.createValidator(
                    context.getSession(), client, assertion);

            //client must be confidential
            authorizationGrantContext.validateClient();

            //mandatory claims
            authorizationGrantContext.validateTokenAudience(List.of(expectedAudience), false);
            authorizationGrantContext.validateIssuer();
            authorizationGrantContext.validateSubject();

            //select the idp using the issuer claim
            String jwtIssuer = authorizationGrantContext.getIssuer();
            AlternativeLookupProvider lookupProvider = context.getSession().getProvider(AlternativeLookupProvider.class);
            IdentityProviderModel identityProviderModel = lookupProvider.lookupIdentityProviderFromIssuer(session, jwtIssuer);
            if (identityProviderModel == null) {
                throw new RuntimeException("No Identity Provider for provided issuer");
            }

            UserAuthenticationIdentityProvider<?> identityProvider = IdentityBrokerService.getIdentityProvider(session, identityProviderModel.getAlias());
            if (!(identityProvider instanceof JWTAuthorizationGrantProvider jwtAuthorizationGrantProvider)) {
                throw new RuntimeException("Identity Provider is not configured for JWT Authorization Grant");
            }

            // assign the provider and perform validations associated to the jwt grant provider
            authorizationGrantContext.validateTokenActive(jwtAuthorizationGrantProvider.getAllowedClockSkew(), 300, false);
            authorizationGrantContext.validateSignatureAlgorithm(jwtAuthorizationGrantProvider.getClientAssertionSigningAlg());

            //validate the JWT assertion and get the brokered identity from the idp
            BrokeredIdentityContext brokeredIdentityContext = jwtAuthorizationGrantProvider.validateAuthorizationGrantAssertion(authorizationGrantContext);
            if (brokeredIdentityContext == null) {
                throw new RuntimeException("Error validating JWT with identity provider");
            }

            //user must exist in keycloak
            FederatedIdentityModel federatedIdentityModel = new FederatedIdentityModel(identityProviderModel.getAlias(), brokeredIdentityContext.getId(), brokeredIdentityContext.getUsername(), brokeredIdentityContext.getToken());
            UserModel user = this.session.users().getUserByFederatedIdentity(realm, federatedIdentityModel);
            if (user == null) {
                throw new RuntimeException("User not found");
            }
            event.user(user);
            event.detail(Details.USERNAME, user.getUsername());

            String scopeParam = formParams.getFirst(OAuth2Constants.SCOPE);
            //TODO: scopes processing

            UserSessionModel userSession = new UserSessionManager(session).createUserSession(realm, user, user.getUsername(), clientConnection.getRemoteHost(), "authorization-grant", false, null, null);
            event.session(userSession);
            RootAuthenticationSessionModel rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, false);
            AuthenticationSessionModel authSession = createSessionModel(rootAuthSession, user, client, scopeParam);
            ClientSessionContext clientSessionCtx = TokenManager.attachAuthenticationSession(this.session, userSession, authSession);
            return createTokenResponse(user, userSession, clientSessionCtx, scopeParam, true, null);
        }
        catch (Exception e) {
            event.detail(Details.REASON, e.getMessage());
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, e.getMessage(), Response.Status.BAD_REQUEST);
        }
    }

    protected AuthenticationSessionModel createSessionModel(RootAuthenticationSessionModel rootAuthSession, UserModel targetUser, ClientModel client, String scope) {
        AuthenticationSessionModel authSession = rootAuthSession.createAuthenticationSession(client);
        authSession.setAuthenticatedUser(targetUser);
        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));
        authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, scope);
        return authSession;
    }

    @Override
    public EventType getEventType() {
        return EventType.LOGIN;
    }
}
