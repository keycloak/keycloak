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
import org.keycloak.constants.Oid4VciConstants;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
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
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;

/**
 * Factory for creating all OID4VC related endpoints and the default mappers.
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCLoginProtocolFactory implements LoginProtocolFactory, OID4VCEnvironmentProviderFactory {

	private static final Logger LOGGER = Logger.getLogger(OID4VCLoginProtocolFactory.class);

	private static final String CLIENT_ROLES_MAPPER = "client-roles";
	private static final String USERNAME_MAPPER = "username";
	private static final String SUBJECT_ID_MAPPER = "subject-id";
	private static final String EMAIL_MAPPER = "email";
	private static final String LAST_NAME_MAPPER = "last-name";
	private static final String FIRST_NAME_MAPPER = "first-name";

	public static final String PROTOCOL_ID = Oid4VciConstants.OID4VC_PROTOCOL;

	private Map<String, ProtocolMapperModel> builtins = new HashMap<>();

	@Override
	public void init(Config.Scope config) {
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
			naturalPersonScope = newRealm.addClientScope(String.format("%s_%s", Oid4VciConstants.OID4VC_PROTOCOL, "natural_person"));
			naturalPersonScope.setDescription("OIDC$VP Scope, that adds all properties required for a natural person.");
			naturalPersonScope.setProtocol(Oid4VciConstants.OID4VC_PROTOCOL);
			naturalPersonScope.addProtocolMapper(builtins.get(SUBJECT_ID_MAPPER));
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
	public void addClientScopeDefaults(ClientScopeRepresentation clientScope) {
		clientScope.getAttributes().computeIfAbsent(CredentialScopeModel.CONFIGURATION_ID, k -> clientScope.getName());
		clientScope.getAttributes().computeIfAbsent(CredentialScopeModel.CREDENTIAL_IDENTIFIER,
				k -> clientScope.getName());
		clientScope.getAttributes().computeIfAbsent(CredentialScopeModel.TYPES, k -> clientScope.getName());
		clientScope.getAttributes().computeIfAbsent(CredentialScopeModel.CONTEXTS, k -> clientScope.getName());
		clientScope.getAttributes().computeIfAbsent(CredentialScopeModel.VCT, k -> clientScope.getName());
		clientScope.getAttributes().computeIfAbsent(CredentialScopeModel.ISSUER_DID, k -> clientScope.getName());
		clientScope.getAttributes().computeIfAbsent(CredentialScopeModel.FORMAT,
				k -> CredentialScopeModel.FORMAT_DEFAULT);
		clientScope.getAttributes().computeIfAbsent(CredentialScopeModel.CRYPTOGRAPHIC_BINDING_METHODS,
				k -> CredentialScopeModel.CRYPTOGRAPHIC_BINDING_METHODS_DEFAULT);
		clientScope.getAttributes().computeIfAbsent(CredentialScopeModel.SD_JWT_NUMBER_OF_DECOYS,
				k -> String.valueOf(CredentialScopeModel.SD_JWT_DECOYS_DEFAULT));
		clientScope.getAttributes().computeIfAbsent(CredentialScopeModel.SD_JWT_VISIBLE_CLAIMS,
				k -> CredentialScopeModel.SD_JWT_VISIBLE_CLAIMS_DEFAULT);
		clientScope.getAttributes().computeIfAbsent(CredentialScopeModel.HASH_ALGORITHM,
				k -> CredentialScopeModel.HASH_ALGORITHM_DEFAULT);
		clientScope.getAttributes().computeIfAbsent(CredentialScopeModel.TOKEN_JWS_TYPE,
				k -> CredentialScopeModel.TOKEN_TYPE_DEFAULT);
		clientScope.getAttributes().computeIfAbsent(CredentialScopeModel.EXPIRY_IN_SECONDS,
				k -> String.valueOf(CredentialScopeModel.EXPIRY_IN_SECONDS_DEFAULT));
	}

	@Override
	public LoginProtocol create(KeycloakSession session) {
		return null;
	}

	@Override
	public String getId() {
		return Oid4VciConstants.OID4VC_PROTOCOL;
	}

	/**
	 * defines the option-order in the admin-ui
	 */
	@Override
	public int order() {
		return OIDCLoginProtocolFactory.UI_ORDER - 20;
	}
}
