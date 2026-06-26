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

package org.keycloak.protocol.oidc.endpoints;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.xml.namespace.QName;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.http.HttpRequest;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCProviderConfig;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.grants.OAuth2GrantType;
import org.keycloak.protocol.oidc.token.TokenInterceptorException;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.protocol.saml.JaxrsSAML2BindingBuilder;
import org.keycloak.protocol.saml.SamlClient;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.util.DPoPUtil;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.keycloak.OAuth2Constants.UMA_GRANT_TYPE;
import static org.keycloak.protocol.oid4vc.model.PreAuthorizedCodeGrant.PRE_AUTH_GRANT_TYPE;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TokenEndpoint {

    private static final Logger LOGGER = Logger.getLogger(TokenEndpoint.class);
    private MultivaluedMap<String, String> formParams;
    private ClientModel client;
    private Map<String, String> clientAuthAttributes;
    private OIDCAdvancedConfigWrapper clientConfig;

    private final KeycloakSession session;

    private final HttpRequest request;

    private final HttpResponse httpResponse;

    private final HttpHeaders headers;

    private final ClientConnection clientConnection;

    private final TokenManager tokenManager;
    private final RealmModel realm;
    private final EventBuilder event;

    private String grantType;
    private OAuth2GrantType grant;

    private Cors cors;

    public TokenEndpoint(KeycloakSession session, TokenManager tokenManager, EventBuilder event) {
        this.session = session;
        this.clientConnection = session.getContext().getConnection();
        this.tokenManager = tokenManager;
        this.realm = session.getContext().getRealm();
        this.event = event;
        this.request = session.getContext().getHttpRequest();
        this.httpResponse = session.getContext().getHttpResponse();
        this.headers = session.getContext().getRequestHeaders();
    }

    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @POST
    public Response processGrantRequest() {
        cors = Cors.builder().auth().allowedMethods("POST").auth().exposedHeaders(Cors.ACCESS_CONTROL_ALLOW_METHODS);

        formParams = Optional.ofNullable(request.getDecodedFormParameters())
                .map(this::sanitizeFormParameters)
                .orElse(new MultivaluedHashMap<>());

        grantType = formParams.getFirst(OIDCLoginProtocol.GRANT_TYPE_PARAM);

        // https://tools.ietf.org/html/rfc6749#section-5.1
        // The authorization server MUST include the HTTP "Cache-Control" response header field
        // with a value of "no-store" as well as the "Pragma" response header field with a value of "no-cache".
        httpResponse.setHeader("Cache-Control", "no-store");
        httpResponse.setHeader("Pragma", "no-cache");

        checkSsl();
        checkRealm();
        checkGrantType();

        try {
            grant.preProcess(session, formParams);
        } catch (ClientPolicyException cpe) {
            event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
            event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
            event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
            event.error(cpe.getError());
            throw new CorsErrorResponseException(cors, cpe.getError(), cpe.getErrorDetail(), cpe.getErrorStatus());
        }

        // pre-authorized grants are not necessarily used by known clients.
        if (!grantType.equals(UMA_GRANT_TYPE) && !grantType.equals(PRE_AUTH_GRANT_TYPE)) {
            checkClient();
            checkParameterDuplicated();
        }

        checkParameters();

        /*
         * To request an access token that is bound to a public key using DPoP, the client MUST provide a valid DPoP
         * proof JWT in a DPoP header when making an access token request to the authorization server's token endpoint.
         * This is applicable for all access token requests regardless of grant type (e.g., the common
         * authorization_code and refresh_token grant types and extension grants such as the JWT
         * authorization grant [RFC7523])
         */
        DPoPUtil.handleDPoPHeader(session, event, cors, clientConfig);

        OAuth2GrantType.Context context = new OAuth2GrantType.Context(session, clientConfig, clientAuthAttributes,
                                                                      formParams, event, cors, tokenManager);

        try {
            return grant.process(context);
        } catch (TokenInterceptorException e) {
            throw new CorsErrorResponseException(cors, e.getError(), e.getDescription(), Response.Status.BAD_REQUEST);
        }
    }

    @Path("introspect")
    public Object introspect() {
        return new TokenIntrospectionEndpoint(this.session, this.event);
    }

    @OPTIONS
    public Response preflight() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugv("CORS preflight from: {0}", headers.getRequestHeaders().getFirst("Origin"));
        }
        return Cors.builder().auth().preflight().allowedMethods("POST", "OPTIONS").add(Response.ok());
    }

    private void checkSsl() {
        if (!session.getContext().getUri().getBaseUri().getScheme().equals("https") && realm.getSslRequired().isRequired(clientConnection)) {
            throw new CorsErrorResponseException(cors.allowAllOrigins(), OAuthErrorException.INVALID_REQUEST, "HTTPS required", Response.Status.FORBIDDEN);
        }
    }

    private void checkRealm() {
        if (!realm.isEnabled()) {
            throw new CorsErrorResponseException(cors.allowAllOrigins(), "access_denied", "Realm not enabled", Response.Status.FORBIDDEN);
        }
    }

    private void checkClient() {
        AuthorizeClientUtil.ClientAuthResult clientAuth = AuthorizeClientUtil.authorizeClient(session, event, cors);
        client = clientAuth.getClient();
        clientAuthAttributes = clientAuth.getClientAuthAttributes();
        clientConfig = OIDCAdvancedConfigWrapper.fromClientModel(client);

        cors.checkAllowedOrigins(session, client);

        if (client.isBearerOnly()) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_CLIENT, "Bearer-only not allowed", Response.Status.BAD_REQUEST);
        }
    }

    private void checkGrantType() {
        if (grantType == null) {
            throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "Missing form parameter: " + OIDCLoginProtocol.GRANT_TYPE_PARAM, Response.Status.BAD_REQUEST);
        }

        grant = session.getProvider(OAuth2GrantType.class, grantType);
        if (grant == null) {
            throw newUnsupportedGrantTypeException();
        }

        event.event(grant.getEventType());
        event.detail(Details.GRANT_TYPE, grantType);
    }

    private void checkParameterDuplicated() {
        for (var entry : formParams.entrySet()) {
            if (entry.getValue().size() != 1 && !grant.getSupportedMultivaluedRequestParameters().contains(entry.getKey())) {
                throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "duplicated parameter",
                        Response.Status.BAD_REQUEST);
            }
        }
    }

    private CorsErrorResponseException newUnsupportedGrantTypeException() {
        return new CorsErrorResponseException(cors, OAuthErrorException.UNSUPPORTED_GRANT_TYPE,
                "Unsupported " + OIDCLoginProtocol.GRANT_TYPE_PARAM, Status.BAD_REQUEST);
    }

    private MultivaluedMap<String, String> sanitizeFormParameters(MultivaluedMap<String, String> formParams) {
        MultivaluedMap<String, String> sanitized = new MultivaluedHashMap<>();
        for (Map.Entry<String, List<String>> entry : formParams.entrySet()) {
            for (String value : entry.getValue()) {
                sanitized.add(entry.getKey(), StringUtil.removeControlCharacters(value));
            }
        }
        return sanitized;
    }


    protected void checkParameters() {
        OIDCLoginProtocol loginProtocol = (OIDCLoginProtocol) session.getProvider(LoginProtocol.class, OIDCLoginProtocol.LOGIN_PROTOCOL);
        OIDCProviderConfig config = loginProtocol.getConfig();
        Set<String> tokenParamNames = grant.getTokenParameterNames();

        Map<String, List<String>> paramsCopy = new HashMap<>(formParams);
        for (Map.Entry<String, List<String>> param : paramsCopy.entrySet()) {
            String paramName = param.getKey();
            int totalLengthOfParamValues = param.getValue().stream()
                    .map(String::length)
                    .reduce(0, Integer::sum);

            // Does this parameter represent a token (typically JWT or SAML assertion)?
            boolean isTokenParam = tokenParamNames.contains(paramName);

            int maxLength = config.getMaxLengthForTheParameter(paramName, isTokenParam);
            if (totalLengthOfParamValues > maxLength) {
                LOGGER.warnf("The size of OIDC parameter '%s' is longer (%d) than allowed (%d). %s", paramName, totalLengthOfParamValues, maxLength, config.isAdditionalReqParamsFailFast(isTokenParam) ? "Request not allowed." : "Ignoring the parameter.");
                if (config.isAdditionalReqParamsFailFast(isTokenParam)) {
                    throw new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, "The size of OIDC parameter '" + paramName + "' is longer than allowed.",
                            Response.Status.BAD_REQUEST);
                } else {
                    formParams.remove(paramName);
                }
            }
        }
    }

    public static class TokenExchangeSamlProtocol extends SamlProtocol {

        final SamlClient samlClient;

        public TokenExchangeSamlProtocol(SamlClient samlClient) {
            this.samlClient = samlClient;
        }

        @Override
        protected Response buildAuthenticatedResponse(AuthenticatedClientSessionModel clientSession, String redirectUri,
                                                      Document samlDocument, JaxrsSAML2BindingBuilder bindingBuilder)
                throws ConfigurationException, ProcessingException, IOException {
            JaxrsSAML2BindingBuilder.PostBindingBuilder builder = bindingBuilder.postBinding(samlDocument);

            Element assertionElement;
            if (samlClient.requiresEncryption()) {
                assertionElement = DocumentUtil.getElement(builder.getDocument(), new QName(JBossSAMLURIConstants.ASSERTION_NSURI.get(), JBossSAMLConstants.ENCRYPTED_ASSERTION.get()));
            } else {
                assertionElement = DocumentUtil.getElement(builder.getDocument(), new QName(JBossSAMLURIConstants.ASSERTION_NSURI.get(), JBossSAMLConstants.ASSERTION.get()));
            }
            if (assertionElement == null) {
                return Response.status(Status.BAD_REQUEST).build();
            }
            String assertion = DocumentUtil.getNodeAsString(assertionElement);
            return Response.ok(assertion, MediaType.APPLICATION_XML_TYPE).build();
        }

        @Override
        protected Response buildErrorResponse(boolean isPostBinding, String destination, JaxrsSAML2BindingBuilder binding, Document document) throws ConfigurationException, ProcessingException, IOException {
            return Response.status(Status.BAD_REQUEST).build();
        }
    }
}
