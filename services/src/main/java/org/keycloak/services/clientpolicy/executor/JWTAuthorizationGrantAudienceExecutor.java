/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.util.Set;

import org.keycloak.OAuthErrorException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.JWTAuthorizationGrantContext;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;

/**
 *
 * @author rmartinc
 */
public class JWTAuthorizationGrantAudienceExecutor implements ClientPolicyExecutorProvider<JWTAuthorizationGrantAudienceExecutor.Configuration> {

    private static final Logger logger = Logger.getLogger(JWTAuthorizationGrantAudienceExecutor.class);
    private final KeycloakSession session;
    private Configuration configuration;

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {

        @JsonProperty(JWTAuthorizationGrantAudienceExecutorFactory.ALLOWED_AUDIENCE)
        protected Set<String> allowedAudience;

        public Set<String> getAllowedAudience() {
            return allowedAudience;
        }

        public void setAllowedAudience(Set<String> allowedAudience) {
            this.allowedAudience = allowedAudience;
        }
    }

    public JWTAuthorizationGrantAudienceExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getProviderId() {
        return JWTAuthorizationGrantAudienceExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void setupConfiguration(Configuration config) {
        this.configuration = config;
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case JWT_AUTHORIZATION_GRANT -> {
                JWTAuthorizationGrantContext jwtAuthnGrantContext = ((JWTAuthorizationGrantContext) context);
                validateAudience(jwtAuthnGrantContext);
            }
        }
    }

    private void validateAudience(JWTAuthorizationGrantContext jwtAuthnGrantContext) throws ClientPolicyException {
        final JsonWebToken jwt = jwtAuthnGrantContext.getAuthorizationGrantContext().getJWT();
        final String[] audience = jwt.getAudience();
        if (audience == null || audience.length != 1 || configuration == null || configuration.getAllowedAudience() == null) {
            // just continue with normal processing in this situations
            return;
        }

        if (configuration.getAllowedAudience().contains(audience[0])) {
            // set the audience as validated by this executor
            logger.tracef("Allowing extra audience '%s' for the jwt authorization grant request.", audience[0]);
            jwtAuthnGrantContext.getAuthorizationGrantContext().setAudienceAlreadyValidated();
            return;
        }

        // the audience is not the ones defined in this executor, throw error
        logger.tracef("Rejecting invalid audience '%s' for the jwt authorization grant request.", audience[0]);
        throw new ClientPolicyException(OAuthErrorException.INVALID_GRANT, "Invalid token audience");
    }
}
