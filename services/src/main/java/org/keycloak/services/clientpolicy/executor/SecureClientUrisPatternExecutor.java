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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.keycloak.OAuthErrorException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientRegisterContext;
import org.keycloak.services.clientpolicy.context.AdminClientUpdateContext;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;
import org.keycloak.services.clientpolicy.context.DynamicClientRegisterContext;
import org.keycloak.services.clientpolicy.context.DynamicClientUpdateContext;
import org.keycloak.validation.ClientUriField;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;

public class SecureClientUrisPatternExecutor implements ClientPolicyExecutorProvider<SecureClientUrisPatternExecutor.Configuration> {

    private static final Logger logger = Logger.getLogger(SecureClientUrisPatternExecutor.class);

    private final KeycloakSession session;
    private List<Pattern> allowedPatterns;
    private List<ClientUriField> clientUriFields;

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {
        @JsonProperty("allowed-patterns")
        protected List<String> allowedPatterns;

        @JsonProperty("client-uri-fields")
        protected List<String> clientUriFields;

        public List<String> getAllowedPatterns() {
            return allowedPatterns;
        }

        public void setAllowedPatterns(List<String> allowedPatterns) {
            this.allowedPatterns = allowedPatterns;
        }

        public List<String> getClientUriFields() {
            return clientUriFields;
        }

        public void setClientUriFields(List<String> clientUriFields) {
            this.clientUriFields = clientUriFields;
        }
    }

    public SecureClientUrisPatternExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setupConfiguration(SecureClientUrisPatternExecutor.Configuration config) {
        // 1. Setup Patterns
        List<String> patternsAsStrings = config.getAllowedPatterns();
        this.allowedPatterns = new ArrayList<>();
        if (patternsAsStrings != null) {
            for (String p : patternsAsStrings) {
                try {
                    this.allowedPatterns.add(Pattern.compile(p));
                } catch (PatternSyntaxException e) {
                    logger.warnv("Ignoring invalid regex pattern in configuration: {0}", p);
                }
            }
        }

        // if empty, validate all the fields
        List<String> configuredFields = config.getClientUriFields();

        if (configuredFields == null || configuredFields.isEmpty()) {
            logger.debug("No specific URI fields configured. Validating ALL known URI fields.");
            this.clientUriFields = List.of(ClientUriField.values());
        } else {
            this.clientUriFields = new ArrayList<>();
            for (String field : configuredFields) {
                if (SecureClientUrisPatternExecutorFactory.ALL_CLIENT_URI_FIELDS.contains(field)) {
                    this.clientUriFields.add(ClientUriField.fromName(field).orElseThrow());
                } else {
                    logger.warnv("Ignored unknown or unsupported field in configuration: {0}", field);
                }
            }
        }
    }

    @Override
    public Class<SecureClientUrisPatternExecutor.Configuration> getExecutorConfigurationClass() {
        return SecureClientUrisPatternExecutor.Configuration.class;
    }

    @Override
    public String getProviderId() {
        return SecureClientUrisPatternExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case REGISTER:
                if (context instanceof AdminClientRegisterContext || context instanceof DynamicClientRegisterContext) {
                    ClientRepresentation clientRep = ((ClientCRUDContext)context).getProposedClientRepresentation();
                    validateClientUris(clientRep);
                } else {
                    throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "not allowed input format.");
                }
                return;
            case UPDATE:
                if (context instanceof AdminClientUpdateContext || context instanceof DynamicClientUpdateContext) {
                    validateClientUris(((ClientCRUDContext)context).getProposedClientRepresentation());
                } else {
                    throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "not allowed input format.");
                }
                return;
            default:
        }
    }

    private void validateClientUris(ClientRepresentation clientRep) throws ClientPolicyException {
        //skip validation if empty
        if (clientUriFields == null || clientUriFields.isEmpty()) {
            return;
        }

        ClientUriField.validate(clientRep, clientUriFields, (field, values) -> confirmSecureUris(values, field.getName()));
    }

    private void confirmSecureUris(List<String> uris, String uriType) throws ClientPolicyException {
        if (uris == null || uris.isEmpty()) {
            return;
        }

        if (allowedPatterns == null || allowedPatterns.isEmpty()) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT_METADATA, "Invalid " + uriType + ": No valid allowed patterns configured");
        }

        for (String uri : uris) {
            if (uri != null && !uri.isEmpty()) {
                logger.tracev("Validating {0} = {1}", uriType, uri);

                boolean matchFound = allowedPatterns.stream().anyMatch(p -> p.matcher(uri).matches());

                if (!matchFound) {
                    logger.warnv("Blocked URI {0} for field {1} - does not match allowed patterns.", uri, uriType);
                    throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT_METADATA, "Invalid " + uriType);
                }
            }
        }
    }

}
