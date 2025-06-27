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

package org.keycloak.protocol.oidc.grants;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.http.HttpRequest;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider;
import org.keycloak.protocol.oid4vc.model.AuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.AuthorizationDetailResponse;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.rar.AuthorizationRequestContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.util.AuthorizationContextUtil;
import org.keycloak.services.util.MtlsHoKTokenUtil;
import org.keycloak.util.TokenUtil;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Base class for OAuth 2.0 grant types
 *
 * @author <a href="mailto:demetrio@carretti.pro">Dmitry Telegin</a> (et al.)
 */
public abstract class OAuth2GrantTypeBase implements OAuth2GrantType {

    private static final Logger logger = Logger.getLogger(OAuth2GrantTypeBase.class);

    protected static final ObjectMapper objectMapper = new ObjectMapper();

    protected static final String AUTHORIZATION_DETAILS_PARAM = "authorization_details";
    protected static final String AUTHORIZATION_DETAILS_RESPONSE_KEY = "authorization_details_response";

    public static final String OPENID_CREDENTIAL_TYPE = "openid_credential";

    protected OAuth2GrantType.Context context;

    protected KeycloakSession session;
    protected RealmModel realm;
    protected ClientModel client;
    protected OIDCAdvancedConfigWrapper clientConfig;
    protected ClientConnection clientConnection;
    protected Map<String, String> clientAuthAttributes;
    protected MultivaluedMap<String, String> formParams;
    protected EventBuilder event;
    protected Cors cors;
    protected TokenManager tokenManager;
    protected HttpRequest request;
    protected HttpResponse response;
    protected HttpHeaders headers;

    protected void setContext(Context context) {
        this.context = context;
        this.session = context.session;
        this.realm = context.realm;
        this.client = context.client;
        this.clientConfig = (OIDCAdvancedConfigWrapper) context.clientConfig;
        this.clientConnection = context.clientConnection;
        this.clientAuthAttributes = context.clientAuthAttributes;
        this.request = context.request;
        this.response = context.response;
        this.headers = context.headers;
        this.formParams = context.formParams;
        this.event = context.event;
        this.cors = context.cors;
        this.tokenManager = (TokenManager) context.tokenManager;
    }

    protected Response createTokenResponse(UserModel user, UserSessionModel userSession, ClientSessionContext clientSessionCtx,
        String scopeParam, boolean code, Function<TokenManager.AccessTokenResponseBuilder, ClientPolicyContext> clientPolicyContextGenerator) {
        clientSessionCtx.setAttribute(Constants.GRANT_TYPE, context.getGrantType());
        AccessToken token = tokenManager.createClientAccessToken(session, realm, client, user, userSession, clientSessionCtx);

        TokenManager.AccessTokenResponseBuilder responseBuilder = tokenManager
            .responseBuilder(realm, client, event, session, userSession, clientSessionCtx).accessToken(token);
        boolean useRefreshToken = clientConfig.isUseRefreshToken();
        if (useRefreshToken) {
            responseBuilder.generateRefreshToken();
            if (TokenUtil.TOKEN_TYPE_OFFLINE.equals(responseBuilder.getRefreshToken().getType())
                    && clientSessionCtx.getClientSession().getNote(AuthenticationProcessor.FIRST_OFFLINE_ACCESS) != null) {
                // the online session can be removed if first created for offline access
                session.sessions().removeUserSession(realm, userSession);
            }
        }

        checkAndBindMtlsHoKToken(responseBuilder, useRefreshToken);

        if (TokenUtil.isOIDCRequest(scopeParam)) {
            responseBuilder.generateIDToken().generateAccessTokenHash();
        }

        if (clientPolicyContextGenerator != null) {
            try {
                session.clientPolicy().triggerOnEvent(clientPolicyContextGenerator.apply(responseBuilder));
            } catch (ClientPolicyException cpe) {
                event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
                event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
                event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
                event.error(cpe.getError());
                throw new CorsErrorResponseException(cors, cpe.getError(), cpe.getErrorDetail(), cpe.getErrorStatus());
            }
        }

        AccessTokenResponse res = null;
        if (code) {
            try {
                res = responseBuilder.build();
            } catch (RuntimeException re) {
                if ("can not get encryption KEK".equals(re.getMessage())) {
                    throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                        "can not get encryption KEK", Response.Status.BAD_REQUEST);
                } else {
                    throw re;
                }
            }
        } else {
            res = responseBuilder.build();
        }

        event.success();

        return cors.add(Response.ok(res).type(MediaType.APPLICATION_JSON_TYPE));
    }

    protected void checkAndBindMtlsHoKToken(TokenManager.AccessTokenResponseBuilder responseBuilder, boolean useRefreshToken) {
        // KEYCLOAK-6771 Certificate Bound Token
        // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-3
        if (clientConfig.isUseMtlsHokToken()) {
            AccessToken.Confirmation confirmation = MtlsHoKTokenUtil.bindTokenWithClientCertificate(request, session);
            if (confirmation != null) {
                responseBuilder.getAccessToken().setConfirmation(confirmation);
                if (useRefreshToken) {
                    responseBuilder.getRefreshToken().setConfirmation(confirmation);
                }
            } else {
                String errorMessage = "Client Certification missing for MTLS HoK Token Binding";
                event.detail(Details.REASON, errorMessage);
                event.error(Errors.INVALID_REQUEST);
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST,
                        errorMessage, Response.Status.BAD_REQUEST);
            }
        }
    }

    protected void updateClientSession(AuthenticatedClientSessionModel clientSession) {

        if(clientSession == null) {
            ServicesLogger.LOGGER.clientSessionNull();
            return;
        }

        String adapterSessionId = formParams.getFirst(AdapterConstants.CLIENT_SESSION_STATE);
        if (adapterSessionId != null) {
            String adapterSessionHost = formParams.getFirst(AdapterConstants.CLIENT_SESSION_HOST);
            logger.debugf("Adapter Session '%s' saved in ClientSession for client '%s'. Host is '%s'", adapterSessionId, client.getClientId(), adapterSessionHost);

            String oldClientSessionState = clientSession.getNote(AdapterConstants.CLIENT_SESSION_STATE);
            if (!adapterSessionId.equals(oldClientSessionState)) {
                clientSession.setNote(AdapterConstants.CLIENT_SESSION_STATE, adapterSessionId);
            }

            String oldClientSessionHost = clientSession.getNote(AdapterConstants.CLIENT_SESSION_HOST);
            if (!Objects.equals(adapterSessionHost, oldClientSessionHost)) {
                clientSession.setNote(AdapterConstants.CLIENT_SESSION_HOST, adapterSessionHost);
            }
        }
    }

    protected void updateUserSessionFromClientAuth(UserSessionModel userSession) {
        for (Map.Entry<String, String> attr : clientAuthAttributes.entrySet()) {
            userSession.setNote(attr.getKey(), attr.getValue());
        }
    }

    protected String getRequestedScopes() {
        String scope = formParams.getFirst(OAuth2Constants.SCOPE);

        boolean validScopes;
        if (Profile.isFeatureEnabled(Profile.Feature.DYNAMIC_SCOPES)) {
            AuthorizationRequestContext authorizationRequestContext = AuthorizationContextUtil.getAuthorizationRequestContextFromScopes(session, scope);
            validScopes = TokenManager.isValidScope(session, scope, authorizationRequestContext, client, null);
        } else {
            validScopes = TokenManager.isValidScope(session, scope, client, null);
        }

        if (!validScopes) {
            String errorMessage = "Invalid scopes: " + scope;
            event.detail(Details.REASON, errorMessage);
            event.error(Errors.INVALID_REQUEST);
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_SCOPE, errorMessage, Response.Status.BAD_REQUEST);
        }

        return scope;
    }

    protected void checkClient() {
        AuthorizeClientUtil.ClientAuthResult clientAuth = AuthorizeClientUtil.authorizeClient(session, event, cors);
        client = clientAuth.getClient();
        clientAuthAttributes = clientAuth.getClientAuthAttributes();
        clientConfig = OIDCAdvancedConfigWrapper.fromClientModel(client);

        cors.allowedOrigins(session, client);

        if (client.isBearerOnly()) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, "Bearer-only not allowed", Response.Status.BAD_REQUEST);
        }
    }

    protected List<AuthorizationDetailResponse> processAuthorizationDetails(UserSessionModel userSession) {
        String authorizationDetailsParam = formParams.getFirst(AUTHORIZATION_DETAILS_PARAM);
        if (authorizationDetailsParam == null) {
            return null; // authorization_details is optional
        }

        List<AuthorizationDetail> authDetails = parseAuthorizationDetails(authorizationDetailsParam);
        List<String> supportedFormats = OID4VCIssuerWellKnownProvider.getSupportedFormats(session);
        Map<String, SupportedCredentialConfiguration> supportedCredentials = OID4VCIssuerWellKnownProvider.getSupportedCredentials(session);
        List<AuthorizationDetailResponse> authDetailsResponse = new ArrayList<>();

        // Retrieve issuer metadata and identifier for locations check
        CredentialIssuer issuerMetadata = (CredentialIssuer) new OID4VCIssuerWellKnownProvider(session).getConfig();
        List<String> authorizationServers = issuerMetadata.getAuthorizationServers();
        String issuerIdentifier = OID4VCIssuerWellKnownProvider.getIssuer(session.getContext());

        for (AuthorizationDetail detail : authDetails) {
            validateAuthorizationDetail(detail, supportedFormats, supportedCredentials, authorizationServers, issuerIdentifier);
            AuthorizationDetailResponse responseDetail = buildAuthorizationDetailResponse(detail, userSession, supportedCredentials, supportedFormats);
            authDetailsResponse.add(responseDetail);
        }

        return authDetailsResponse;
    }

    private List<AuthorizationDetail> parseAuthorizationDetails(String authorizationDetailsParam) {
        try {
            return objectMapper.readValue(authorizationDetailsParam, new TypeReference<List<AuthorizationDetail>>() {
            });
        } catch (Exception e) {
            logger.warnf(e, "Invalid authorization_details format: %s", authorizationDetailsParam);
            throwInvalidRequest("Invalid authorization_details format");
            throw new IllegalStateException("Unreachable");
        }
    }

    private void validateAuthorizationDetail(AuthorizationDetail detail, List<String> supportedFormats, Map<String, SupportedCredentialConfiguration> supportedCredentials, List<String> authorizationServers, String issuerIdentifier) {
        String type = detail.getType();
        String credentialConfigurationId = detail.getCredentialConfigurationId();
        String format = detail.getFormat();
        String vct = detail.getVct();

        // If authorization_servers is present, locations must be set to issuer identifier
        if (authorizationServers != null && !authorizationServers.isEmpty() && OPENID_CREDENTIAL_TYPE.equals(type)) {
            List<String> locations = detail.getLocations();
            if (locations == null || locations.size() != 1 || !issuerIdentifier.equals(locations.get(0))) {
                logger.warnf("Invalid locations field in authorization_details: %s, expected: %s", locations, issuerIdentifier);
                throwInvalidRequest("Invalid authorization_details");
            }
        }

        // Validate type
        if (!OPENID_CREDENTIAL_TYPE.equals(type)) {
            logger.warnf("Invalid authorization_details type: %s", type);
            throwInvalidRequest("Invalid authorization_details");
        }

        // Ensure exactly one of credential_configuration_id or format is present
        if ((credentialConfigurationId == null && format == null) || (credentialConfigurationId != null && format != null)) {
            logger.warnf("Exactly one of credential_configuration_id or format must be present. credentialConfigurationId: %s, format: %s", credentialConfigurationId, format);
            throwInvalidRequest("Invalid authorization_details");
        }

        if (credentialConfigurationId != null) {
            // Validate credential_configuration_id
            SupportedCredentialConfiguration config = supportedCredentials.get(credentialConfigurationId);
            if (config == null) {
                logger.warnf("Unsupported credential_configuration_id: %s", credentialConfigurationId);
                throwInvalidRequest("Invalid credential configuration");
            }
        } else {
            // Validate format
            if (!supportedFormats.contains(format)) {
                logger.warnf("Unsupported format: %s", format);
                throwInvalidRequest("Invalid credential format");
            }

            // Validate vct
            if (vct != null) {
                boolean vctSupported = supportedCredentials.values().stream()
                        .filter(config -> format.equals(config.getFormat()))
                        .anyMatch(config -> vct.equals(config.getVct()));
                if (!vctSupported) {
                    logger.warnf("Unsupported vct for format %s: %s", format, vct);
                    throwInvalidRequest("Invalid credential configuration");
                }
            }
        }
    }

    private AuthorizationDetailResponse buildAuthorizationDetailResponse(AuthorizationDetail detail, UserSessionModel userSession, Map<String, SupportedCredentialConfiguration> supportedCredentials, List<String> supportedFormats) {
        String credentialConfigurationId = detail.getCredentialConfigurationId();
        String format = detail.getFormat();
        String vct = detail.getVct();

        if (credentialConfigurationId != null) {
            // Check for existing credential identifier
            String noteKey = "credential_identifier_" + credentialConfigurationId;
            List<String> credentialIdentifiers = new ArrayList<>();
            synchronized (userSession) {
                String existingIdentifier = userSession.getNote(noteKey);
                if (existingIdentifier != null) {
                    credentialIdentifiers.add(existingIdentifier);
                } else {
                    String newIdentifier = UUID.randomUUID().toString();
                    credentialIdentifiers.add(newIdentifier);
                    userSession.setNote(noteKey, newIdentifier);
                }
            }
            // Prepare response details
            AuthorizationDetailResponse responseDetail = new AuthorizationDetailResponse();
            responseDetail.setType(OPENID_CREDENTIAL_TYPE);
            responseDetail.setCredentialConfigurationId(credentialConfigurationId);
            responseDetail.setCredentialIdentifiers(credentialIdentifiers);
            return responseDetail;
        } else {
            // Check for existing credential identifier
            String noteKey = "credential_identifier_" + format;
            List<String> credentialIdentifiers = new ArrayList<>();
            synchronized (userSession) {
                String existingIdentifier = userSession.getNote(noteKey);
                if (existingIdentifier != null) {
                    credentialIdentifiers.add(existingIdentifier);
                } else {
                    String newIdentifier = UUID.randomUUID().toString();
                    credentialIdentifiers.add(newIdentifier);
                    userSession.setNote(noteKey, newIdentifier);
                }
            }
            // Prepare response details with validated format
            AuthorizationDetailResponse responseDetail = new AuthorizationDetailResponse();
            responseDetail.setType(OPENID_CREDENTIAL_TYPE);
            responseDetail.setFormat(format);
            responseDetail.setVct(vct);
            responseDetail.setCredentialIdentifiers(credentialIdentifiers);
            return responseDetail;
        }
    }

    private void throwInvalidRequest(String errorDescription) {
        event.error(Errors.INVALID_REQUEST);
        throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, errorDescription, Response.Status.BAD_REQUEST);
    }

    @Override
    public void close() {
    }

}
