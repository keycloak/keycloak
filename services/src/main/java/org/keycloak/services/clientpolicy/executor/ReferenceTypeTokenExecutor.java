/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Response.Status;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ReferenceTypeTokenExecutor extends AbstractReferenceTypeTokenExecutor<ReferenceTypeTokenExecutor.Configuration> {

    private static final Logger logger = Logger.getLogger(ReferenceTypeTokenExecutor.class);

    private Configuration configuration;

    public ReferenceTypeTokenExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setupConfiguration(ReferenceTypeTokenExecutor.Configuration config) {
        this.configuration = Optional.ofNullable(config).orElse(createDefaultConfiguration());
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {

        @JsonProperty(ReferenceTypeTokenExecutorFactory.SELFCONTAINED_TYPE_TOKEN_BIND_ENDPOINT)
        protected String selfcontainedTypeTokenBindEndpoint;

        @JsonProperty(ReferenceTypeTokenExecutorFactory.SELFCONTAINED_TYPE_TOKEN_GET_ENDPOINT)
        protected String selfcontainedTypeTokenGetEndpoint;

        public String getSelfcontainedTypeTokenBindEndpoint() {
            return selfcontainedTypeTokenBindEndpoint;
        }

        public void setSelfcontainedTypeTokenBindEndpoint(String selfcontainedTypeTokenBindEndpoint) {
            this.selfcontainedTypeTokenBindEndpoint = selfcontainedTypeTokenBindEndpoint;
        }

        public String getSelfcontainedTypeTokenGetEndpoint() {
            return selfcontainedTypeTokenGetEndpoint;
        }

        public void setSelfcontainedTypeTokenGetEndpoint(String selfcontainedTypeTokenGetEndpoint) {
            this.selfcontainedTypeTokenGetEndpoint = selfcontainedTypeTokenGetEndpoint;
        }
    }

    private Configuration createDefaultConfiguration() {
        Configuration conf = new Configuration();
        return conf;
    }

    @Override
    public String getProviderId() {
        return ReferenceTypeTokenExecutorFactory.PROVIDER_ID;
    }

    @Override
    protected String createReferenceTypeAccessToken(TokenManager.AccessTokenResponseBuilder builder) {
        return builder.getAccessToken().getId();
    }

    @Override
    protected String createReferenceTypeRefreshToken(TokenManager.AccessTokenResponseBuilder builder) {
        return builder.getRefreshToken().getId();
    }

    @Override
    protected String getSelfcontainedTypeToken(String referenceTypeToken, ClientPolicyException exceptionOnInvalidToken) throws ClientPolicyException {

        if (referenceTypeToken == null || referenceTypeToken.isEmpty()) {
            logger.warnv("no reference type token.");
            if (exceptionOnInvalidToken != null) {
                throw exceptionOnInvalidToken;
            } else {
                throw new ClientPolicyException(OAuthErrorException.INVALID_TOKEN, "Invalid token");
            }
        }

        if (!isValidSelfcontainedTypeTokenStoreUrl(configuration.getSelfcontainedTypeTokenGetEndpoint())) {
            logger.warnv("getting self-contained token failed due to invalid self-contained type token get endpoint configuration.");
            throw new ClientPolicyException(OAuthErrorException.SERVER_ERROR, "Internal problem", Status.INTERNAL_SERVER_ERROR);
        }

        UriBuilder uri = UriBuilder.fromUri(configuration.getSelfcontainedTypeTokenGetEndpoint())
                .queryParam(ReferenceTypeTokenExecutorFactory.SELFCONTAINED_TYPE_TOKEN_GET_ENDPOINT_QUERY_PARAM, referenceTypeToken);
        final String url = uri.build().toString();
        ReferenceTypeTokenBindResponse response = null;

        try {
            SimpleHttp simpleHttp = SimpleHttp.doGet(url, session);
            SimpleHttp.Response res = simpleHttp.asResponse();

            if (res.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
                logger.warnv("getting self-contained token failed. referenceTypeToken = {0}", referenceTypeToken);
                if (exceptionOnInvalidToken != null) {
                    throw exceptionOnInvalidToken;
                } else {
                    throw new ClientPolicyException(OAuthErrorException.INVALID_TOKEN, "Invalid token");
                }
            } else if (res.getStatus() != Status.OK.getStatusCode()) {
                String error = Optional.ofNullable(res.asJson()).map(t->t.get(OAuth2Constants.ERROR_DESCRIPTION)).map(t->t.asText()).orElse(null);
                logger.warnv("getting self-contained token failed due to internal problems. error = {0}", error);
                throw new ClientPolicyException(OAuthErrorException.SERVER_ERROR, "Internal problem", Status.INTERNAL_SERVER_ERROR);
            }

            response = res.asJson(ReferenceTypeTokenBindResponse.class);

        } catch (IOException ioe) {
            logger.warnv("getting self-contained token failed due to network errror. error = {0}", ioe.getMessage());
            throw new ClientPolicyException(OAuthErrorException.SERVER_ERROR, "Internal problem", Status.INTERNAL_SERVER_ERROR);
        }

        return response.getSelfcontainedTypeToken();
    }

    @Override
    protected void bindSelfcontainedTypeToken(String selfcontainedTypeToken, String referenceTypeToken) throws ClientPolicyException {

        if (!isValidBindRequest(selfcontainedTypeToken, referenceTypeToken)) {
            logger.warnv("invalid bind tokens request");
            throw new ClientPolicyException(OAuthErrorException.SERVER_ERROR,"Internal problem", Status.INTERNAL_SERVER_ERROR);
        }

        if (!isValidSelfcontainedTypeTokenStoreUrl(configuration.getSelfcontainedTypeTokenBindEndpoint())) {
            logger.warnv("getting self-contained token failed due to invalid self-contained type token bind endopoint configuration.");
            throw new ClientPolicyException(OAuthErrorException.SERVER_ERROR, "Internal problem", Status.INTERNAL_SERVER_ERROR);
        }

        ReferenceTypeTokenBindRequest request = new ReferenceTypeTokenBindRequest();
        request.setSelfcontainedTypeToken(selfcontainedTypeToken);
        request.setReferenceTypeToken(referenceTypeToken);

        logger.tracev("----- BEFORE BIND  referenceTypeToken = {0}, selfcontainedTypeToken = {1}", referenceTypeToken, selfcontainedTypeToken);

        try {
            SimpleHttp simpleHttp = SimpleHttp.doPost(configuration.getSelfcontainedTypeTokenBindEndpoint(), session)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .json(request);

            if (simpleHttp.asStatus() != Status.NO_CONTENT.getStatusCode()) {
                logger.warnv("binding reference type token with self-contained token failed. referenceTypeToken = {0}", referenceTypeToken);
                throw new ClientPolicyException(OAuthErrorException.SERVER_ERROR, "Internal problem", Status.INTERNAL_SERVER_ERROR);
            }

        } catch (IOException ioe) {
            logger.warnv("binding reference type token with self-contained token failed due to network error error =  {0}", ioe.getMessage());
            throw new ClientPolicyException(OAuthErrorException.SERVER_ERROR, "Internal problem", Status.INTERNAL_SERVER_ERROR);
        }
    }

    protected boolean isValidBindRequest(String selfcontainedTypeToken, String referenceTypeToken) {
        if (selfcontainedTypeToken == null || selfcontainedTypeToken.isEmpty()) return false;
        if (referenceTypeToken == null || referenceTypeToken.isEmpty()) return false;
        return true;
    }

    protected boolean isValidSelfcontainedTypeTokenStoreUrl(String url) {
        if (url == null) return false;
        if (!url.startsWith("http://") && !url.startsWith("https://")) return false;
        return true;
    }

    public static class ReferenceTypeTokenBindRequest implements Serializable {

        @JsonProperty("referenceTypeToken")
        private String referenceTypeToken;

        @JsonProperty("selfcontainedTypeToken")
        private String selfcontainedTypeToken;

        public String getReferenceTypeToken() {
            return referenceTypeToken;
        }

        public void setReferenceTypeToken(String referenceTypeToken) {
            this.referenceTypeToken = referenceTypeToken;
        }

        public String getSelfcontainedTypeToken() {
            return selfcontainedTypeToken;
        }

        public void setSelfcontainedTypeToken(String selfcontainedTypeToken) {
            this.selfcontainedTypeToken = selfcontainedTypeToken;
        }
    }

    public static class ReferenceTypeTokenBindResponse implements Serializable {

        @JsonProperty("selfcontainedTypeToken")
        private String selfcontainedTypeToken;

        public String getSelfcontainedTypeToken() {
            return selfcontainedTypeToken;
        }

        public void setSelfcontainedTypeToken(String selfcontainedTypeToken) {
            this.selfcontainedTypeToken = selfcontainedTypeToken;
        }
    }
}