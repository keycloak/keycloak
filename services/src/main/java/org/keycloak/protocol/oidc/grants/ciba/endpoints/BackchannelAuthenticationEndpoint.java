/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oidc.grants.ciba.endpoints;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuth2DeviceCodeModel;
import org.keycloak.models.OAuth2DeviceUserCodeModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantType;
import org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelProvider;
import org.keycloak.protocol.oidc.grants.ciba.channel.CIBAAuthenticationRequest;
import org.keycloak.protocol.oidc.grants.ciba.clientpolicy.context.BackchannelAuthenticationRequestContext;
import org.keycloak.protocol.oidc.grants.ciba.endpoints.request.BackchannelAuthenticationEndpointRequest;
import org.keycloak.protocol.oidc.grants.ciba.endpoints.request.BackchannelAuthenticationEndpointRequestParserProcessor;
import org.keycloak.protocol.oidc.grants.ciba.resolvers.CIBALoginUserResolver;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import java.util.Collections;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.keycloak.protocol.oidc.OIDCLoginProtocol.ID_TOKEN_HINT;
import static org.keycloak.protocol.oidc.OIDCLoginProtocol.LOGIN_HINT_PARAM;

public class BackchannelAuthenticationEndpoint extends AbstractCibaEndpoint {

    private final RealmModel realm;

    private static final Pattern BINDING_MESSAGE_VALIDATION = Pattern.compile("^[a-zA-Z0-9-._+/!?#]{1,50}$");

    public BackchannelAuthenticationEndpoint(KeycloakSession session, EventBuilder event) {
        super(session, event);
        this.realm = session.getContext().getRealm();
        event.event(EventType.LOGIN);
    }

    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response processGrantRequest(@Context HttpRequest httpRequest) {
        CIBAAuthenticationRequest request = authorizeClient(httpRequest.getDecodedFormParameters());

        try {
            String authReqId = request.serialize(session);
            AuthenticationChannelProvider provider = session.getProvider(AuthenticationChannelProvider.class);

            if (provider == null) {
                throw new RuntimeException("Authentication Channel Provider not found.");
            }

            CIBALoginUserResolver resolver = session.getProvider(CIBALoginUserResolver.class);

            if (resolver == null) {
                throw new RuntimeException("CIBA Login User Resolver not setup properly.");
            }

            UserModel user = request.getUser();

            String infoUsedByAuthentication = resolver.getInfoUsedByAuthentication(user);

            if (provider.requestAuthentication(request, infoUsedByAuthentication)) {
                CibaConfig cibaPolicy = realm.getCibaPolicy();
                int poolingInterval = cibaPolicy.getPoolingInterval();

                storeAuthenticationRequest(request, cibaPolicy, authReqId);

                ObjectNode response = JsonSerialization.createObjectNode();

                response.put(CibaGrantType.AUTH_REQ_ID, authReqId)
                        .put(OAuth2Constants.EXPIRES_IN, cibaPolicy.getExpiresIn());

                if (poolingInterval > 0) {
                    response.put(OAuth2Constants.INTERVAL, poolingInterval);
                }

                return Response.ok(JsonSerialization.writeValueAsBytes(response))
                        .build();
            }
        } catch (Exception e) {
            throw new ErrorResponseException(OAuthErrorException.SERVER_ERROR, "Failed to send authentication request", Response.Status.SERVICE_UNAVAILABLE);
        }

        throw new ErrorResponseException(OAuthErrorException.SERVER_ERROR, "Unexpected response from authentication device", Response.Status.SERVICE_UNAVAILABLE);
    }

    /**
     * TODO: Leverage the device code storage for tracking authentication requests. Not sure if we need a specific storage,
     * or we can leverage the {@link SingleUseObjectProvider} for ciba, device, or any other use case
     * that relies on cross-references for unsolicited user authentication requests from devices.
     */
    private void storeAuthenticationRequest(CIBAAuthenticationRequest request, CibaConfig cibaConfig, String authReqId) {
        ClientModel client = request.getClient();
        int expiresIn = cibaConfig.getExpiresIn();
        int poolingInterval = cibaConfig.getPoolingInterval();
        String cibaMode = cibaConfig.getBackchannelTokenDeliveryMode(client);

        // Set authReqId just for the ping mode as it is relatively big and not necessarily needed in the infinispan cache for the "poll" mode
        if (!CibaConfig.CIBA_PING_MODE.equals(cibaMode)) {
            authReqId = null;
        }

        OAuth2DeviceCodeModel deviceCode = OAuth2DeviceCodeModel.create(realm, client,
                request.getId(), request.getScope(), null, expiresIn, poolingInterval, request.getClientNotificationToken(), authReqId,
                Collections.emptyMap(), null, null);
        String authResultId = request.getAuthResultId();
        OAuth2DeviceUserCodeModel userCode = new OAuth2DeviceUserCodeModel(realm, deviceCode.getDeviceCode(),
                authResultId);

        // To inform "expired_token" to the client, the lifespan of the cache provider is longer than device code
        int lifespanSeconds = expiresIn + poolingInterval + 10;

        SingleUseObjectProvider singleUseStore = session.getProvider(SingleUseObjectProvider.class);

        singleUseStore.put(deviceCode.serializeKey(), lifespanSeconds, deviceCode.toMap());
        singleUseStore.put(userCode.serializeKey(), lifespanSeconds, userCode.serializeValue());
    }

    private CIBAAuthenticationRequest authorizeClient(MultivaluedMap<String, String> params) {
        ClientModel client = null;
        try {
            client = authenticateClient();
        } catch (WebApplicationException wae) {
            OAuth2ErrorRepresentation errorRep = (OAuth2ErrorRepresentation)wae.getResponse().getEntity();
            throw new ErrorResponseException(errorRep.getError(), errorRep.getErrorDescription(), Response.Status.UNAUTHORIZED);
        }
        BackchannelAuthenticationEndpointRequest endpointRequest = BackchannelAuthenticationEndpointRequestParserProcessor.parseRequest(event, session, client, params, realm.getCibaPolicy());
        UserModel user = resolveUser(endpointRequest, realm.getCibaPolicy().getAuthRequestedUserHint());

        CIBAAuthenticationRequest request = new CIBAAuthenticationRequest(session, user, client);

        request.setClient(client);

        String scope = endpointRequest.getScope();
        if (scope == null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "missing parameter : scope",
                    Response.Status.BAD_REQUEST);
        }
        if (!TokenManager.isValidScope(scope, client)) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Invalid scopes: " + scope,
                    Response.Status.BAD_REQUEST);
        }
        request.setScope(scope);

        // optional parameters
        if (endpointRequest.getBindingMessage() != null) {
            validateBindingMessage(endpointRequest.getBindingMessage());
            request.setBindingMessage(endpointRequest.getBindingMessage());
        }
        if (endpointRequest.getAcr() != null) request.setAcrValues(endpointRequest.getAcr());

        CibaConfig policy = realm.getCibaPolicy();

        // create JWE encoded auth_req_id from Auth Req ID.
        Integer expiresIn = Optional.ofNullable(endpointRequest.getRequestedExpiry()).orElse(policy.getExpiresIn());

        request.exp(request.getIat() + expiresIn.longValue());

        StringBuilder scopes = new StringBuilder(Optional.ofNullable(request.getScope()).orElse(""));
        client.getClientScopes(true)
                .forEach((key, value) -> {
                    if (value.isDisplayOnConsentScreen())
                        scopes.append(" ").append(value.getName());
                });
        request.setScope(scopes.toString());

        if (endpointRequest.getClientNotificationToken() != null) {
            if (!policy.getBackchannelTokenDeliveryMode(client).equals(CibaConfig.CIBA_PING_MODE)) {
                throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST,
                        "Client Notification token supported only for the ping mode", Response.Status.BAD_REQUEST);
            }
            if (endpointRequest.getClientNotificationToken().length() > 1024) {
                throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST,
                        "Client Notification token length is limited to 1024 characters", Response.Status.BAD_REQUEST);
            }
            request.setClientNotificationToken(endpointRequest.getClientNotificationToken());
        }
        if (endpointRequest.getClientNotificationToken() == null && policy.getBackchannelTokenDeliveryMode(client).equals(CibaConfig.CIBA_PING_MODE)) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST,
                    "Client Notification token needs to be provided with the ping mode", Response.Status.BAD_REQUEST);
        }

        if (endpointRequest.getUserCode() != null) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "User code not supported",
                    Response.Status.BAD_REQUEST);
        }

        extractAdditionalParams(endpointRequest, request);

        try {
            session.clientPolicy().triggerOnEvent(new BackchannelAuthenticationRequestContext(endpointRequest, request, params));
        } catch (ClientPolicyException cpe) {
            throw new ErrorResponseException(cpe.getError(), cpe.getErrorDetail(), Response.Status.BAD_REQUEST);
        }

        return request;
    }

    protected void extractAdditionalParams(BackchannelAuthenticationEndpointRequest endpointRequest, CIBAAuthenticationRequest request) {
        for (String paramName : endpointRequest.getAdditionalReqParams().keySet()) {
            request.setOtherClaims(paramName, endpointRequest.getAdditionalReqParams().get(paramName));
        }
    }

    protected void validateBindingMessage(String bindingMessage) {
        if (!BINDING_MESSAGE_VALIDATION.matcher(bindingMessage).matches()) {
            throw new ErrorResponseException(OAuthErrorException.INVALID_BINDING_MESSAGE, "the binding_message value has to be max 50 characters in length and must contain only basic plain-text characters without spaces",
                    Response.Status.BAD_REQUEST);
        }
    }

    private UserModel resolveUser(BackchannelAuthenticationEndpointRequest endpointRequest, String authRequestedUserHint) {
        CIBALoginUserResolver resolver = session.getProvider(CIBALoginUserResolver.class);

        if (resolver == null) {
            throw new RuntimeException("CIBA Login User Resolver not setup properly.");
        }

        String userHint;
        UserModel user;

        if (authRequestedUserHint.equals(LOGIN_HINT_PARAM)) {
            userHint = endpointRequest.getLoginHint();
            if (userHint == null)
                throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "missing parameter : login_hint",
                        Response.Status.BAD_REQUEST);
            user = resolver.getUserFromLoginHint(userHint);
        } else if (authRequestedUserHint.equals(ID_TOKEN_HINT)) {
            userHint = endpointRequest.getIdTokenHint();
            if (userHint == null)
                throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "missing parameter : id_token_hint",
                        Response.Status.BAD_REQUEST);
            user = resolver.getUserFromIdTokenHint(userHint);
        } else if (authRequestedUserHint.equals(CibaGrantType.LOGIN_HINT_TOKEN)) {
            userHint = endpointRequest.getLoginHintToken();
            if (userHint == null)
                throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "missing parameter : login_hint_token",
                        Response.Status.BAD_REQUEST);
            user = resolver.getUserFromLoginHintToken(userHint);
        } else {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST,
                    "invalid user hint", Response.Status.BAD_REQUEST);
        }

        if (user == null || !user.isEnabled())
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "invalid user", Response.Status.BAD_REQUEST);

        return user;
    }
}
