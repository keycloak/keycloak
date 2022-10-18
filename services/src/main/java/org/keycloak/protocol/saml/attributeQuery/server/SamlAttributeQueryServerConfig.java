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
package org.keycloak.protocol.saml.attributeQuery.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.attributeQuery.BaseSamlAttributeQueryConfig;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author Ben Cresitello-Dittmar
 *
 * An extended configuration to support attribute query server configuration.
 */
public class SamlAttributeQueryServerConfig extends BaseSamlAttributeQueryConfig {

    protected static final Logger logger = Logger.getLogger(SamlAttributeQueryServerConfig.class);

    private String audience;
    private List<String> filters;

    /**
     * Get the audience restriction set on the generated SAML attribute query response assertion
     * @return The audience
     */
    public String getAudience() {
        return audience;
    }

    /**
     * Set the audience restriction that will be set on the generated SAML attribute query response assertion
     * @param audience The audience
     */
    public void setAudience(String audience) {
        this.audience = audience;
    }

    /**
     * Get the filters that are used to filter attributes from users that will be included in the SAML attribute query
     * response assertion. Each filter should be a regex expression, a user attribute that matches ANY of the specified
     * regex filters will be included in the attribute query response.
     * @return The list of filters
     */
    public List<String> getFilters() {
        return filters;
    }

    /**
     * Set the filters used to filter attributes from users that will be included in the SAML attribute query
     * response assertion. Each filter should be a regex expression, a user attribute that matches ANY of the specified
     * regex filters will be included in the attribute query response.
     * @param filters The filters
     */
    public void setFilters(List<String> filters) {
        this.filters = filters;
    }

    @Override
    public List<String> verify(KeycloakSession session) {
        List<String> errors = super.verify(session);

        // verify required values are set
        if (getAudience() == null || getAudience().isEmpty()){
            errors.add("audience cannot be empty");
        }

        return errors;
    }

    /**
     * Load config from all clients in the given realm that support the attribute query feature
     * @param session the keycloak session
     * @param realm the keycloak realm
     * @return a stream of initialized configurations
     */
    public static Stream<SamlAttributeQueryServerConfig> loadAllAndVerify(KeycloakSession session, RealmModel realm) {
        return realm.getClientsStream()
                .filter(c -> Boolean.parseBoolean(c.getAttribute(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_SUPPORTED)))
                .map(SamlAttributeQueryServerConfig::load)
                .filter(c -> c.verify(session).isEmpty());
    }

    /**
     * Load the configuration from the specified client. This does not verify the configurations, call verify to verify
     * the config is valid
     * @param client the client to load the config from
     * @return the attribute query server config loaded from the provided client
     */
    private static SamlAttributeQueryServerConfig load(ClientModel client){
        SamlAttributeQueryServerConfig config = new SamlAttributeQueryServerConfig();
        //required
        config.setExpectedIssuer(client.getAttribute(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_ISSUER));
        config.setAudience(client.getAttribute(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_TARGET_AUDIENCE));
        config.setClientId(client.getClientId());
        // optional
        config.setIdpSigningCert(client.getAttribute(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_SIGNING_CERT));
        config.setIdpEncryptionCert(client.getAttribute(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_ENCRYPTION_CERT));
        config.setSubjectAttribute(client.getAttribute(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_USER_LOOKUP_ATTRIBUTE));
        config.setFilters(deserializeFilters(client.getAttribute(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_FILTERS), client.getClientId()));
        config.setRequireDocumentSignature(Boolean.parseBoolean(client.getAttribute(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_REQUIRE_SIGNED_REQ)));
        config.setRequireEncryption(Boolean.parseBoolean(client.getAttribute(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_REQUIRE_ENCRYPTED_REQ)));
        config.setSignDocument(Boolean.parseBoolean(client.getAttribute(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_SIGN_DOC)));
        config.setSignAssertion(Boolean.parseBoolean(client.getAttribute(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_SIGN_ASSERTION)));
        config.setEncryptAssertion(Boolean.parseBoolean(client.getAttribute(SamlConfigAttributes.SAML_ATTRIBUTE_QUERY_ENCRYPT_RESPONSE)));

        return config;
    }

    /**
     * Helper function to deserialize the provided filters string. If the JSON is invalid, an empty list is returned
     *
     * @param json The JSON string to deserialize
     * @return the deserialized filters
     */
    private static List<String> deserializeFilters(String json, String clientId) {
        if (json == null){
            return Collections.emptyList();
        }

        try {
            return (new ObjectMapper()).readValue(json, new TypeReference<List<String>>(){});
        } catch (JsonProcessingException e){
            logger.warnf("got invalid filter value '%s' on attribute query client '%s'", json, clientId);
            return Collections.emptyList();
        }
    }

}
