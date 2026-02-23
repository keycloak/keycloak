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

import jakarta.ws.rs.core.Response;

import org.keycloak.Config;
import org.keycloak.VCFormat;
import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCSubjectIdMapper;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCUserAttributeMapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorResponseException;

import org.jboss.logging.Logger;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_SUBJECT_ID;
import static org.keycloak.VCFormat.SD_JWT_VC;
import static org.keycloak.constants.OID4VCIConstants.OID4VC_PROTOCOL;
import static org.keycloak.models.ClientScopeModel.INCLUDE_IN_TOKEN_SCOPE;
import static org.keycloak.models.oid4vci.CredentialScopeModel.CONFIGURATION_ID;
import static org.keycloak.models.oid4vci.CredentialScopeModel.CONTEXTS;
import static org.keycloak.models.oid4vci.CredentialScopeModel.CRYPTOGRAPHIC_BINDING_METHODS;
import static org.keycloak.models.oid4vci.CredentialScopeModel.CRYPTOGRAPHIC_BINDING_METHODS_DEFAULT;
import static org.keycloak.models.oid4vci.CredentialScopeModel.EXPIRY_IN_SECONDS;
import static org.keycloak.models.oid4vci.CredentialScopeModel.EXPIRY_IN_SECONDS_DEFAULT;
import static org.keycloak.models.oid4vci.CredentialScopeModel.FORMAT;
import static org.keycloak.models.oid4vci.CredentialScopeModel.HASH_ALGORITHM;
import static org.keycloak.models.oid4vci.CredentialScopeModel.HASH_ALGORITHM_DEFAULT;
import static org.keycloak.models.oid4vci.CredentialScopeModel.INCLUDE_IN_METADATA;
import static org.keycloak.models.oid4vci.CredentialScopeModel.SD_JWT_DECOYS_DEFAULT;
import static org.keycloak.models.oid4vci.CredentialScopeModel.SD_JWT_NUMBER_OF_DECOYS;
import static org.keycloak.models.oid4vci.CredentialScopeModel.SD_JWT_VISIBLE_CLAIMS;
import static org.keycloak.models.oid4vci.CredentialScopeModel.SD_JWT_VISIBLE_CLAIMS_DEFAULT;
import static org.keycloak.models.oid4vci.CredentialScopeModel.TOKEN_JWS_TYPE;
import static org.keycloak.models.oid4vci.CredentialScopeModel.TOKEN_TYPE_DEFAULT;
import static org.keycloak.models.oid4vci.CredentialScopeModel.TYPES;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VCT;

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

	public static final String PROTOCOL_ID = OID4VCIConstants.OID4VC_PROTOCOL;
    public static final String CREDENTIAL_TYPE_NATURAL_PERSON = "natural_person";

	private final Map<String, ProtocolMapperModel> builtins = new HashMap<>();

	@Override
	public void init(Config.Scope config) {
		builtins.put(SUBJECT_ID_MAPPER, OID4VCSubjectIdMapper.create(SUBJECT_ID_MAPPER, CLAIM_NAME_SUBJECT_ID, UserModel.DID));
		builtins.put(USERNAME_MAPPER, OID4VCUserAttributeMapper.create(USERNAME_MAPPER, "username", "username", false));
		builtins.put(EMAIL_MAPPER, OID4VCUserAttributeMapper.create(EMAIL_MAPPER, "email", "email", false));
		builtins.put(FIRST_NAME_MAPPER, OID4VCUserAttributeMapper.create(FIRST_NAME_MAPPER, "firstName", "firstName", false));
		builtins.put(LAST_NAME_MAPPER, OID4VCUserAttributeMapper.create(LAST_NAME_MAPPER, "familyName", "lastName", false));
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

        for (String format : VCFormat.SUPPORTED_FORMATS) {
            String scopeName = CREDENTIAL_TYPE_NATURAL_PERSON + VCFormat.getScopeSuffix(format);
            ClientScopeModel clientScope = KeycloakModelUtils.getClientScopeByName(newRealm, scopeName);
            if (clientScope == null) {
                LOGGER.debugf("Add client scope: %s", scopeName);
                clientScope = newRealm.addClientScope(String.format("%s_%s", OID4VC_PROTOCOL, scopeName));
                clientScope.setDescription("OID4VCI credential scope that represents a natural person in format: " + format);
                clientScope.setProtocol(OID4VC_PROTOCOL);
                clientScope.addProtocolMapper(builtins.get(SUBJECT_ID_MAPPER));
                clientScope.addProtocolMapper(builtins.get(EMAIL_MAPPER));
                clientScope.addProtocolMapper(builtins.get(FIRST_NAME_MAPPER));
                clientScope.addProtocolMapper(builtins.get(LAST_NAME_MAPPER));

                ClientScopeRepresentation clientScopeRep = ModelToRepresentation.toRepresentation(clientScope);
                addClientScopeDefaults(clientScopeRep);
                RepresentationToModel.updateClientScope(clientScopeRep, clientScope);
            }
        }
    }

    @Override
    public void setupClientDefaults(ClientRepresentation rep, ClientModel newClient) {
        //no-op
    }

    @Override
    public void addClientScopeDefaults(ClientScopeRepresentation clientScope) {
        String scopeName = clientScope.getName();

        clientScope.getAttributes().computeIfAbsent(FORMAT, k -> VCFormat.getFromScope(scopeName));
        String format = clientScope.getAttributes().get(FORMAT);

        int idx = scopeName.lastIndexOf(VCFormat.getScopeSuffix(format));
        String credentialType = idx > 0 ? scopeName.substring(0, idx) : scopeName;

        // Note, there is no sensible default for the Issuer's DID unless we generate a did:key:* from the signing key
        // Leaving vc.issuer_did undefined results in the realm's url being used as the value for the Issuer's ID (iss).
        // clientScope.getAttributes().computeIfAbsent(ISSUER_DID, k -> <generate did or use the realm url>)

        clientScope.getAttributes().putIfAbsent(INCLUDE_IN_TOKEN_SCOPE, "true");
        clientScope.getAttributes().putIfAbsent(INCLUDE_IN_METADATA, "true");
        clientScope.getAttributes().putIfAbsent(CONFIGURATION_ID, scopeName);
        clientScope.getAttributes().putIfAbsent(TYPES, credentialType);
        clientScope.getAttributes().putIfAbsent(CONTEXTS, credentialType);
        clientScope.getAttributes().putIfAbsent(VCT, credentialType);
        clientScope.getAttributes().putIfAbsent(CRYPTOGRAPHIC_BINDING_METHODS, CRYPTOGRAPHIC_BINDING_METHODS_DEFAULT);
        clientScope.getAttributes().putIfAbsent(HASH_ALGORITHM, HASH_ALGORITHM_DEFAULT);
        clientScope.getAttributes().putIfAbsent(TOKEN_JWS_TYPE, TOKEN_TYPE_DEFAULT);
        clientScope.getAttributes().putIfAbsent(EXPIRY_IN_SECONDS, String.valueOf(EXPIRY_IN_SECONDS_DEFAULT));

        if (SD_JWT_VC.equals(format)) {
            clientScope.getAttributes().putIfAbsent(SD_JWT_NUMBER_OF_DECOYS, String.valueOf(SD_JWT_DECOYS_DEFAULT));
            clientScope.getAttributes().putIfAbsent(SD_JWT_VISIBLE_CLAIMS, SD_JWT_VISIBLE_CLAIMS_DEFAULT);
        }
    }

    @Override
    public void validateClientScope(KeycloakSession session, ClientScopeRepresentation clientScope) throws ErrorResponseException {
        LoginProtocolFactory.super.validateClientScope(session, clientScope);

        RealmModel realm = session.getContext().getRealm();
        if (!realm.isVerifiableCredentialsEnabled()) {
            throw ErrorResponse.error(
                    "OID4VC protocol cannot be used when Verifiable Credentials is disabled for the realm",
                    Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public boolean isValidClientScope(KeycloakSession session, ClientModel client, ClientScopeModel clientScope) {
        return session.getContext().getRealm().isVerifiableCredentialsEnabled();
    }

    @Override
    public LoginProtocol create(KeycloakSession session) {
        return null;
    }

    @Override
    public String getId() {
        return OID4VC_PROTOCOL;
    }

    /**
     * defines the option-order in the admin-ui
     */
    @Override
    public int order() {
        return OIDCLoginProtocolFactory.UI_ORDER - 20;
    }

    @Override
    public void validateClientScopeAssignment(KeycloakSession session, ClientScopeModel clientScope, boolean defaultScope, RealmModel realm) {
        if (!OID4VC_PROTOCOL.equals(clientScope.getProtocol())) {
            return;
        }

        if (!realm.isVerifiableCredentialsEnabled()) {
            throw new ErrorResponseException("invalid_request",
                    "OID4VCI client scopes cannot be assigned when Verifiable Credentials is disabled for the realm",
                    Response.Status.BAD_REQUEST);
        }

        if (defaultScope) {
            throw new ErrorResponseException("invalid_request",
                    "OID4VCI client scopes cannot be assigned as Default scopes. Only Optional scope assignment is supported.",
                    Response.Status.BAD_REQUEST);
        }
    }

    // Private ---------------------------------------------------------------------------------------------------------
}
