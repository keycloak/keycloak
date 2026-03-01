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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.keycloak.OAuthErrorException;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientRegisterContext;
import org.keycloak.services.clientpolicy.context.AdminClientUpdateContext;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;
import org.keycloak.services.clientpolicy.context.DynamicClientRegisterContext;
import org.keycloak.services.clientpolicy.context.DynamicClientUpdateContext;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;

public class SecureClientUrisPatternExecutor implements ClientPolicyExecutorProvider<SecureClientUrisPatternExecutor.Configuration> {

    private static final Logger logger = Logger.getLogger(SecureClientUrisPatternExecutor.class);

    private final KeycloakSession session;
    private List<Pattern> allowedPatterns;
    private List<String> clientUriFields;

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
            this.clientUriFields = new ArrayList<>(SecureClientUrisPatternExecutorFactory.ALL_CLIENT_URI_FIELDS);
        } else {
            this.clientUriFields = new ArrayList<>();
            for (String field : configuredFields) {
                if (SecureClientUrisPatternExecutorFactory.ALL_CLIENT_URI_FIELDS.contains(field)) {
                    this.clientUriFields.add(field);
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
        return SecureClientUrisExecutorFactory.PROVIDER_ID;
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

        for (String fieldName : clientUriFields) {
            List<String> valuesToValidate = getValuesForField(clientRep, fieldName);
            if (valuesToValidate != null && !valuesToValidate.isEmpty()) {
                confirmSecureUris(valuesToValidate, fieldName);
            }
        }
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

    private List<String> getValuesForField(ClientRepresentation client, String fieldName) {
        if (client == null || fieldName == null) return Collections.emptyList();

        Map<String, String> attributes = Optional.ofNullable(client.getAttributes()).orElse(Collections.emptyMap());

        switch (fieldName) {
            case "rootUrl":
                return singletonOrEmpty(client.getRootUrl());
            case "adminUrl":
                return singletonOrEmpty(client.getAdminUrl());
            case "baseUrl":
                return singletonOrEmpty(client.getBaseUrl());
            case "redirectUris":
                return client.getRedirectUris();
            case "webOrigins":
                return client.getWebOrigins();

            //attributes
            case "jwksUri":
                return singletonOrEmpty(attributes.get(OIDCConfigAttributes.JWKS_URL));
            case "requestUris":
                return getAttributeMultivalued(attributes, OIDCConfigAttributes.REQUEST_URIS);
            case "backchannelLogoutUrl":
                return singletonOrEmpty(attributes.get(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL));
            case "postLogoutRedirectUris":
                return getAttributeMultivalued(attributes, OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS);
            case "cibaClientNotificationEndpoint":
                return singletonOrEmpty(attributes.get(CibaConfig.CIBA_BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT));
            case OIDCConfigAttributes.LOGO_URI:
                return singletonOrEmpty(attributes.get(OIDCConfigAttributes.LOGO_URI));
            case OIDCConfigAttributes.POLICY_URI:
                return singletonOrEmpty(attributes.get(OIDCConfigAttributes.POLICY_URI));
            case OIDCConfigAttributes.TOS_URI:
                return singletonOrEmpty(attributes.get(OIDCConfigAttributes.TOS_URI));
            case OIDCConfigAttributes.SECTOR_IDENTIFIER_URI:
                return singletonOrEmpty(attributes.get(OIDCConfigAttributes.SECTOR_IDENTIFIER_URI));
            default:
                logger.debugv("Field extraction not implemented for: {0}", fieldName);
                return Collections.emptyList();
        }
    }

    private List<String> singletonOrEmpty(String value) {
        return (value != null && !value.isEmpty()) ? Collections.singletonList(value) : Collections.emptyList();
    }

    private List<String> getAttributeMultivalued(Map<String, String> attributes, String attrKey) {
        String attrValue = attributes.get(attrKey);
        if (attrValue == null || attrValue.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(Constants.CFG_DELIMITER_PATTERN.split(attrValue));
    }
}
