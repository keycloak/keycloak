/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy.executor;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.events.Errors;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyLogger;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;
import org.keycloak.services.clientpolicy.context.LogoutRequestContext;
import org.keycloak.services.clientpolicy.context.TokenRefreshContext;
import org.keycloak.services.clientpolicy.context.TokenRevokeContext;
import org.keycloak.services.clientpolicy.context.UserInfoRequestContext;
import org.keycloak.services.util.MtlsHoKTokenUtil;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class HolderOfKeyEnforceExecutor implements ClientPolicyExecutorProvider {

    private static final Logger logger = Logger.getLogger(HolderOfKeyEnforceExecutor.class);
    private static final String LOGMSG_PREFIX = "CLIENT-POLICY";
    private String logMsgPrefix() {
        return LOGMSG_PREFIX + "@" + session.hashCode() + " :: EXECUTOR";
    }

    private final KeycloakSession session;
    private Configuration configuration;

    public HolderOfKeyEnforceExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setupConfiguration(Object config) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            configuration = mapper.convertValue(config, Configuration.class);
        } catch (IllegalArgumentException iae) {
            ClientPolicyLogger.logv(logger, "{0} :: failed for Configuration Setup :: error = {1}", logMsgPrefix(), iae.getMessage());
            return;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Configuration {
        @JsonProperty("is-augment")
        protected Boolean augment;

        public Boolean isAugment() {
            return augment;
        }

        public void setAugment(Boolean augment) {
            this.augment = augment;
        }
    }

    @Override
    public String getProviderId() {
        return HolderOfKeyEnforceExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        HttpRequest request = session.getContext().getContextObject(HttpRequest.class);
        switch (context.getEvent()) {
            case REGISTER:
            case UPDATE:
                ClientCRUDContext clientUpdateContext = (ClientCRUDContext)context;
                augment(clientUpdateContext.getProposedClientRepresentation());
                validate(clientUpdateContext.getProposedClientRepresentation());
                break;
            case TOKEN_REQUEST:
                AccessToken.CertConf certConf = MtlsHoKTokenUtil.bindTokenWithClientCertificate(request, session);
                if (certConf == null) {
                    throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Client Certification missing for MTLS HoK Token Binding");
                }
                break;
            case TOKEN_REFRESH:
                checkTokenRefresh((TokenRefreshContext) context, request);
                break;
            case TOKEN_REVOKE:
                checkTokenRevoke((TokenRevokeContext) context, request);
                break;
            case USERINFO_REQUEST:
                checkUserInfo((UserInfoRequestContext) context, request);
                break;
            case LOGOUT_REQUEST:
                checkLogout((LogoutRequestContext) context, request);
                break;
            default:
                return;
        }
    }

    private void augment(ClientRepresentation rep) {
        if (configuration.isAugment()) {
            OIDCAdvancedConfigWrapper.fromClientRepresentation(rep).setUseMtlsHoKToken(true);
        }
    }

    private void validate(ClientRepresentation rep) throws ClientPolicyException {
        boolean useMtlsHokToken = OIDCAdvancedConfigWrapper.fromClientRepresentation(rep).isUseMtlsHokToken();
        if (!useMtlsHokToken) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT_METADATA, "Invalid client metadata: MTLS token in disabled");
        }
    }

    private void checkLogout(LogoutRequestContext context, HttpRequest request) throws ClientPolicyException {
        MultivaluedMap<String, String> formParameters = context.getParams();
        String encodedRefreshToken = formParameters.getFirst(OAuth2Constants.REFRESH_TOKEN);

        RefreshToken refreshToken = session.tokens().decode(encodedRefreshToken, RefreshToken.class);
        if (refreshToken == null) {
            // this executor does not treat this error case.
            return;
        }

        if (!MtlsHoKTokenUtil.verifyTokenBindingWithClientCertificate(refreshToken, request, session)) {
            throw new ClientPolicyException(Errors.NOT_ALLOWED, MtlsHoKTokenUtil.CERT_VERIFY_ERROR_DESC, Response.Status.UNAUTHORIZED);
        }
    }

    private void checkUserInfo(UserInfoRequestContext context, HttpRequest request) throws ClientPolicyException {
        String encodedAccessToken = context.getTokenString();

        AccessToken accessToken = session.tokens().decode(encodedAccessToken, AccessToken.class);
        if (accessToken == null) {
            // this executor does not treat this error case.
            return;
        }

        if (!MtlsHoKTokenUtil.verifyTokenBindingWithClientCertificate(accessToken, request, session)) {
            throw new ClientPolicyException(Errors.NOT_ALLOWED, MtlsHoKTokenUtil.CERT_VERIFY_ERROR_DESC, Response.Status.UNAUTHORIZED);
        }
    }

    private void checkTokenRevoke(TokenRevokeContext context, HttpRequest request) throws ClientPolicyException {
        MultivaluedMap<String, String> revokeParameters = context.getParams();
        String encodedRevokeToken = revokeParameters.getFirst("token");

        RefreshToken refreshToken = session.tokens().decode(encodedRevokeToken, RefreshToken.class);
        if (refreshToken == null) {
            // this executor does not treat this error case.
            return;
        }

        if (!MtlsHoKTokenUtil.verifyTokenBindingWithClientCertificate(refreshToken, request, session)) {
            throw new ClientPolicyException(Errors.NOT_ALLOWED, MtlsHoKTokenUtil.CERT_VERIFY_ERROR_DESC, Response.Status.UNAUTHORIZED);
        }
    }

    private void checkTokenRefresh(TokenRefreshContext context, HttpRequest request) throws ClientPolicyException {
        MultivaluedMap<String, String> formParameters = context.getParams();
        String encodedRefreshToken = formParameters.getFirst(OAuth2Constants.REFRESH_TOKEN);

        RefreshToken refreshToken = session.tokens().decode(encodedRefreshToken, RefreshToken.class);
        if (refreshToken == null) {
            // this executor does not treat this error case.
            return;
        }

        if (!MtlsHoKTokenUtil.verifyTokenBindingWithClientCertificate(refreshToken, request, session)) {
            throw new ClientPolicyException(Errors.NOT_ALLOWED, MtlsHoKTokenUtil.CERT_VERIFY_ERROR_DESC, Response.Status.UNAUTHORIZED);
        }
    }

}
