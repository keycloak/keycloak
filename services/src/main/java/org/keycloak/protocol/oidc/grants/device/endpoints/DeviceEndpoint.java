/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.grants.device.endpoints;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.Time;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.OAuth2DeviceCodeModel;
import org.keycloak.models.OAuth2DeviceTokenStoreProvider;
import org.keycloak.models.OAuth2DeviceUserCodeModel;
import org.keycloak.models.OAuth2DeviceUserCodeProvider;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.SystemClientUtil;
import org.keycloak.protocol.AuthorizationEndpointBase;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequestParserProcessor;
import org.keycloak.protocol.oidc.grants.device.DeviceGrantType;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.representations.OAuth2DeviceAuthorizationResponse;
import org.keycloak.saml.common.util.StringUtil;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resources.SessionCodeChecks;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint.LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX;
import static org.keycloak.protocol.oidc.grants.device.DeviceGrantType.OAUTH2_DEVICE_USER_CODE;
import static org.keycloak.protocol.oidc.grants.device.DeviceGrantType.OAUTH2_USER_CODE_VERIFY;
import static org.keycloak.services.resources.LoginActionsService.SESSION_CODE;

/**
 * @author <a href="mailto:h2-wada@nri.co.jp">Hiroyuki Wada</a>
 */
public class DeviceEndpoint extends AuthorizationEndpointBase implements RealmResourceProvider {

    protected static final Logger logger = Logger.getLogger(DeviceEndpoint.class);

    public DeviceEndpoint(RealmModel realm, EventBuilder event) {
        super(realm, event);
    }

    /**
     * Handles device authorization requests.
     *
     * @return the device authorization response.
     */
    @Path("")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response handleDeviceRequest() {
        logger.trace("Processing @POST request");
        event.event(EventType.OAUTH2_DEVICE_AUTH);

        checkSsl();
        checkRealm();

        ClientModel client = authenticateClient();

        AuthorizationEndpointRequest request = AuthorizationEndpointRequestParserProcessor.parseRequest(event, session, client,
            httpRequest.getDecodedFormParameters());

        if (!TokenUtil.isOIDCRequest(request.getScope())) {
            ServicesLogger.LOGGER.oidcScopeMissing();
        }

        // So back button doesn't work
        CacheControlUtil.noBackButtonCacheControlHeader();

        if (!realm.getOAuth2DeviceConfig().isOAuth2DeviceAuthorizationGrantEnabled(client)) {
            event.error(Errors.NOT_ALLOWED);
            throw new ErrorResponseException(OAuthErrorException.INVALID_GRANT,
                "Client not allowed for OAuth 2.0 Device Authorization Grant", Response.Status.BAD_REQUEST);
        }

        int expiresIn = realm.getOAuth2DeviceConfig().getLifespan(client);
        int interval = realm.getOAuth2DeviceConfig().getPoolingInterval(client);

        OAuth2DeviceCodeModel deviceCode = OAuth2DeviceCodeModel.create(realm, client,
            Base64Url.encode(KeycloakModelUtils.generateSecret()), request.getScope(), request.getNonce(), expiresIn, interval,
            request.getAdditionalReqParams());
        OAuth2DeviceUserCodeProvider userCodeProvider = session.getProvider(OAuth2DeviceUserCodeProvider.class);
        String secret = userCodeProvider.generate();
        OAuth2DeviceUserCodeModel userCode = new OAuth2DeviceUserCodeModel(realm, deviceCode.getDeviceCode(), secret);

        // To inform "expired_token" to the client, the lifespan of the cache provider is longer than device code
        int lifespanSeconds = expiresIn + interval + 10;

        OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
        store.put(deviceCode, userCode, lifespanSeconds);

        try {
            String deviceUrl = DeviceGrantType.oauth2DeviceVerificationUrl(session.getContext().getUri()).build(realm.getName())
                .toString();

            OAuth2DeviceAuthorizationResponse response = new OAuth2DeviceAuthorizationResponse();
            response.setDeviceCode(deviceCode.getDeviceCode());
            response.setUserCode(userCodeProvider.display(secret));
            response.setExpiresIn(expiresIn);
            response.setInterval(interval);
            response.setVerificationUri(deviceUrl);
            response.setVerificationUriComplete(deviceUrl + "?user_code=" + response.getUserCode());

            return Response.ok(JsonSerialization.writeValueAsBytes(response)).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            throw new RuntimeException("Error creating OAuth 2.0 Device Authorization Response.", e);
        }
    }

    /**
     * This endpoint is used by end-users to start the flow to authorize a device.
     *
     * @param userCode the user code to authorize
     * @return
     */
    @GET
    public Response verifyUserCode(@QueryParam("user_code") String userCode) {
        event.event(EventType.OAUTH2_DEVICE_VERIFY_USER_CODE);

        checkSsl();
        checkRealm();

        // So back button doesn't work
        CacheControlUtil.noBackButtonCacheControlHeader();

        // code is not known, we can infer the client neither. ask the user to provide the code.
        if (StringUtil.isNullOrEmpty(userCode)) {
            return createVerificationPage(null);
        } else {
            // code exists, probably due to using a verification_uri_complete. Start the authentication considering the client
            // that started the flow.
            OAuth2DeviceTokenStoreProvider store = session.getProvider(OAuth2DeviceTokenStoreProvider.class);
            OAuth2DeviceUserCodeProvider userCodeProvider = session.getProvider(OAuth2DeviceUserCodeProvider.class);
            String formattedUserCode = userCodeProvider.format(userCode);
            OAuth2DeviceCodeModel deviceCode = store.getByUserCode(realm, formattedUserCode);

            if (deviceCode == null) {
                event.error(Errors.INVALID_OAUTH2_USER_CODE);
                return createVerificationPage(Messages.OAUTH2_DEVICE_INVALID_USER_CODE);
            }

            if (deviceCode.isExpired()) {
                event.error(Errors.INVALID_OAUTH2_USER_CODE);
                return createVerificationPage(Messages.OAUTH2_DEVICE_EXPIRED_USER_CODE);
            }

            return processVerification(deviceCode, formattedUserCode);
        }
    }

    /**
     * Verifies the code provided by the end-user and start the authentication.
     *
     * @return
     */
    @Path("/")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response verifyUserCode() {
        MultivaluedMap<String, String> formData = httpRequest.getDecodedFormParameters();
        return verifyUserCode(formData.getFirst(OAUTH2_DEVICE_USER_CODE));
    }

    /**
     * Showing the result of verification process for OAuth 2.0 Device Authorization Grant. This outputs login success or
     * failure messages.
     *
     * @param error
     * @return
     */
    @Path("status")
    @GET
    public Response status(@QueryParam("error") String error) {
        if (!StringUtil.isNullOrEmpty(error)) {
            String message;
            switch (error) {
                case OAuthErrorException.ACCESS_DENIED:
                    // cased by CANCELLED_BY_USER or CONSENT_DENIED:
                    message = Messages.OAUTH2_DEVICE_CONSENT_DENIED;
                    break;
                case OAuthErrorException.EXPIRED_TOKEN:
                    message = Messages.OAUTH2_DEVICE_EXPIRED_USER_CODE;
                    break;
                default:
                    message = Messages.OAUTH2_DEVICE_VERIFICATION_FAILED;
            }
            LoginFormsProvider forms = session.getProvider(LoginFormsProvider.class);
            String restartUri = DeviceGrantType.oauth2DeviceVerificationUrl(session.getContext().getUri())
                .build(realm.getName()).toString();
            return forms.setAttribute("messageHeader", forms.getMessage(Messages.OAUTH2_DEVICE_VERIFICATION_FAILED_HEADER))
                .setAttribute(Constants.TEMPLATE_ATTR_ACTION_URI, restartUri).setError(message).createInfoPage();
        } else {
            LoginFormsProvider forms = session.getProvider(LoginFormsProvider.class);
            return forms.setAttribute("messageHeader", forms.getMessage(Messages.OAUTH2_DEVICE_VERIFICATION_COMPLETE_HEADER))
                .setAttribute(Constants.SKIP_LINK, true).setSuccess(Messages.OAUTH2_DEVICE_VERIFICATION_COMPLETE)
                .createInfoPage();
        }
    }

    private Response createVerificationPage(String errorMessage) {
        String execution = AuthenticatedClientSessionModel.Action.USER_CODE_VERIFICATION.name();

        LoginFormsProvider provider = session.getProvider(LoginFormsProvider.class)
            .setExecution(execution);

        if (errorMessage != null) {
            provider = provider.setError(errorMessage);
        }

        return provider.createOAuth2DeviceVerifyUserCodePage();
    }

    private Response processVerification(OAuth2DeviceCodeModel deviceCode, String userCode) {
        long expiresIn = deviceCode.getExpiration() - Time.currentTime();
        if (expiresIn < 0) {
            event.error(Errors.EXPIRED_OAUTH2_USER_CODE);
            return createVerificationPage(Messages.OAUTH2_DEVICE_INVALID_USER_CODE);
        }

        ClientModel client = realm.getClientByClientId(deviceCode.getClientId());
        AuthenticationSessionModel authenticationSession = createAuthenticationSession(client);

        // Verification OK
        authenticationSession.setClientNote(DeviceGrantType.OAUTH2_DEVICE_VERIFIED_USER_CODE, userCode);

        // Event logging for the verification
        event.client(deviceCode.getClientId()).detail(Details.SCOPE, deviceCode.getScope()).success();

        OIDCLoginProtocol protocol = new OIDCLoginProtocol(session, realm, session.getContext().getUri(), headers, event);
        return handleBrowserAuthenticationRequest(authenticationSession, protocol, false, true);
    }

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {

    }

    private ClientModel authenticateClient() {
        // https://tools.ietf.org/html/draft-ietf-oauth-device-flow-15#section-3.1
        // The spec says "The client authentication requirements of Section 3.2.1 of [RFC6749]
        // apply to requests on this endpoint".
        AuthorizeClientUtil.ClientAuthResult clientAuth = AuthorizeClientUtil.authorizeClient(session, event, null);
        ClientModel client = clientAuth.getClient();

        if (client == null) {
            event.error(Errors.INVALID_REQUEST);
            throw new ErrorPageException(session, null, Response.Status.BAD_REQUEST, Messages.MISSING_PARAMETER,
                OIDCLoginProtocol.CLIENT_ID_PARAM);
        }

        checkClient(client.getClientId());

        return client;
    }

    private ClientModel checkClient(String clientId) {
        if (clientId == null) {
            event.error(Errors.INVALID_REQUEST);
            throw new ErrorPageException(session, null, Response.Status.BAD_REQUEST,
                Messages.MISSING_PARAMETER, OIDCLoginProtocol.CLIENT_ID_PARAM);
        }

        event.client(clientId);

        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) {
            event.error(Errors.CLIENT_NOT_FOUND);
            throw new ErrorPageException(session, null, Response.Status.BAD_REQUEST,
                Messages.CLIENT_NOT_FOUND);
        }

        if (!client.isEnabled()) {
            event.error(Errors.CLIENT_DISABLED);
            throw new ErrorPageException(session, null, Response.Status.BAD_REQUEST, Messages.CLIENT_DISABLED);
        }

        if (!realm.getOAuth2DeviceConfig().isOAuth2DeviceAuthorizationGrantEnabled(client)) {
            event.error(Errors.NOT_ALLOWED);
            throw new ErrorPageException(session, null, Response.Status.BAD_REQUEST,
                Messages.OAUTH2_DEVICE_AUTHORIZATION_GRANT_DISABLED);
        }

        if (client.isBearerOnly()) {
            event.error(Errors.NOT_ALLOWED);
            throw new ErrorPageException(session, null, Response.Status.FORBIDDEN, Messages.BEARER_ONLY);
        }

        String protocol = client.getProtocol();
        if (protocol == null) {
            logger.warnf("Client '%s' doesn't have protocol set. Fallback to openid-connect. Please fix client configuration",
                clientId);
            protocol = OIDCLoginProtocol.LOGIN_PROTOCOL;
        }
        if (!protocol.equals(OIDCLoginProtocol.LOGIN_PROTOCOL)) {
            event.error(Errors.INVALID_CLIENT);
            throw new ErrorPageException(session, null, Response.Status.BAD_REQUEST, "Wrong client protocol.");
        }

        session.getContext().setClient(client);

        return client;
    }

    protected AuthenticationSessionModel createAuthenticationSession(ClientModel client) {
        AuthenticationSessionModel authenticationSession = super.createAuthenticationSession(client, null);

        authenticationSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authenticationSession.setAction(AuthenticationSessionModel.Action.AUTHENTICATE.name());
        authenticationSession.setClientNote(OIDCLoginProtocol.ISSUER,
            Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));

        return authenticationSession;
    }
}