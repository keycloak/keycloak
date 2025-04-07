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

package org.keycloak.protocol.oid4vc;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCSubjectIdMapper;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCTargetRoleMapper;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCUserAttributeMapper;
import org.keycloak.representations.idm.ClientRepresentation;

/**
 * Factory for creating all OID4VC related endpoints and the default mappers.
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCLoginProtocolFactory implements LoginProtocolFactory, OID4VCEnvironmentProviderFactory {

    private static final Logger LOGGER = Logger.getLogger(OID4VCLoginProtocolFactory.class);

    public static final String PROTOCOL_ID = "oid4vc";

    private static final String CLIENT_ROLES_MAPPER = "client-roles";
    private static final String USERNAME_MAPPER = "username";
    private static final String SUBJECT_ID_MAPPER = "subject-id";
    private static final String EMAIL_MAPPER = "email";
    private static final String LAST_NAME_MAPPER = "last-name";
    private static final String FIRST_NAME_MAPPER = "first-name";

    private Map<String, ProtocolMapperModel> builtins = new HashMap<>();

    @Override
    public void init(Config.Scope config) {
        builtins.put(CLIENT_ROLES_MAPPER, OID4VCTargetRoleMapper.create("id", "client roles"));
        builtins.put(SUBJECT_ID_MAPPER, OID4VCSubjectIdMapper.create("subject id", "id"));
        builtins.put(USERNAME_MAPPER, OID4VCUserAttributeMapper.create(USERNAME_MAPPER, "username", "username", false));
        builtins.put(EMAIL_MAPPER, OID4VCUserAttributeMapper.create(EMAIL_MAPPER, "email", "email", false));
        builtins.put(FIRST_NAME_MAPPER, OID4VCUserAttributeMapper.create(FIRST_NAME_MAPPER, "firstName", "firstName", false));
        builtins.put(LAST_NAME_MAPPER, OID4VCUserAttributeMapper.create(LAST_NAME_MAPPER, "lastName", "familyName", false));
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public Map<String, ProtocolMapperModel> getBuiltinMappers() {
        return builtins;
    }


    @Override
    public Object createProtocolEndpoint(KeycloakSession keycloakSession, EventBuilder event) {
        return new OID4VCIssuerEndpoint(keycloakSession);
    }

    @Override
    public void createDefaultClientScopes(RealmModel newRealm, boolean addScopesToExistingClients) {
        LOGGER.debugf("Create default scopes for realm %s", newRealm.getName());

        ClientScopeModel naturalPersonScope = KeycloakModelUtils.getClientScopeByName(newRealm, "natural_person");
        if (naturalPersonScope == null) {
            LOGGER.debug("Add natural person scope");
            naturalPersonScope = newRealm.addClientScope(String.format("%s_%s", PROTOCOL_ID, "natural_person"));
            naturalPersonScope.setDescription("OIDC$VP Scope, that adds all properties required for a natural person.");
            naturalPersonScope.setProtocol(PROTOCOL_ID);
            naturalPersonScope.addProtocolMapper(builtins.get(SUBJECT_ID_MAPPER));
            naturalPersonScope.addProtocolMapper(builtins.get(CLIENT_ROLES_MAPPER));
            naturalPersonScope.addProtocolMapper(builtins.get(EMAIL_MAPPER));
            naturalPersonScope.addProtocolMapper(builtins.get(FIRST_NAME_MAPPER));
            naturalPersonScope.addProtocolMapper(builtins.get(LAST_NAME_MAPPER));
            newRealm.addDefaultClientScope(naturalPersonScope, true);
        }
    }

    @Override
    public void setupClientDefaults(ClientRepresentation rep, ClientModel newClient) {
        //no-op
    }

    @Override
    public LoginProtocol create(KeycloakSession session) {
        return null;
    }

    @Override
    public String getId() {
        return PROTOCOL_ID;
    }

}
