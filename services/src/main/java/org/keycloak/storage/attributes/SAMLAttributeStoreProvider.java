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
package org.keycloak.storage.attributes;

import jakarta.ws.rs.ProcessingException;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.protocol.saml.attributeQuery.client.SAMLAttributeQueryClient;
import org.keycloak.protocol.saml.attributeQuery.client.SAMLAttributeQueryClientConfig;

import java.util.*;

/**
 * @author Ben Cresitello-Dittmar
 *
 * This class implements the {@link AttributeStoreProvider} interface to provide SAML attribute query
 * functionality to keyclaok. It fetches attributes using the SAML attribute query protocol for a given user.
 */
public class SAMLAttributeStoreProvider implements AttributeStoreProvider
{
    protected final KeycloakSession session;
    protected final ComponentModel component;
    protected final SAMLAttributeQueryClientConfig config;

    public SAMLAttributeStoreProvider(SAMLAttributeStoreProviderFactory factory, KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.component = model;
        this.config = SAMLAttributeQueryClientConfig.load(model);

        List<String> errors = this.config.verify(session);
        if (!errors.isEmpty()){
            throw new RuntimeException(String.format("failed to parse component configuration: %s", errors));
        }
    }

    public void close() {}

    /**
     * Perform a SAML attribute query request to the configured endpoint for the given user
     * @param session the keycloak session
     * @param realm The realm the request is taking place in
     * @param user the user to send the SAML attribute query request for
     * @return the attributes received in the SAML attribute query response
     * @throws ProcessingException thrown if the SAML attribute query request fails
     */
    public Map<String, Object> getAttributes(KeycloakSession session, RealmModel realm, UserModel user) throws ProcessingException {
        // get attribute query subject
        String attrQuerySubject = config.getSubjectAttribute() == null || config.getSubjectAttribute().isEmpty() ? user.getUsername() : user.getFirstAttribute(config.getSubjectAttribute());
        if (attrQuerySubject == null || attrQuerySubject.isEmpty()){
            throw new ProcessingException(String.format("failed to get subject attribute for attribute query request: attribute %s could not be found for user %s", config.getSubjectAttribute(), user.getUsername()));
        }

        // request attributes from the external server
        Map<String, String> attributes;
        try {
            attributes = new SAMLAttributeQueryClient(session, realm, attrQuerySubject, config).request();
        } catch (org.keycloak.saml.common.exceptions.ProcessingException ex){
            throw new ProcessingException(ex);
        }

        return new HashMap<>(attributes);
    }
}
