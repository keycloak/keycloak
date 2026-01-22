/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin.mapper;

import java.util.Set;

import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.mapper.ClientModelMapper;
import org.keycloak.models.mapper.SAMLClientModelMapper;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@KeycloakIntegrationTest(config = SAMLClientModelMapperTest.ServerConfig.class)
public class SAMLClientModelMapperTest {

    // SAML attribute keys (matching SAMLClientModelMapper)
    private static final String SAML_NAME_ID_FORMAT = "saml_name_id_format";
    private static final String SAML_FORCE_NAME_ID_FORMAT = "saml_force_name_id_format";
    private static final String SAML_AUTHN_STATEMENT = "saml.authnstatement";
    private static final String SAML_SERVER_SIGNATURE = "saml.server.signature";
    private static final String SAML_ASSERTION_SIGNATURE = "saml.assertion.signature";
    private static final String SAML_CLIENT_SIGNATURE = "saml.client.signature";
    private static final String SAML_FORCE_POST_BINDING = "saml.force.post.binding";
    private static final String SAML_SIGNATURE_ALGORITHM = "saml.signature.algorithm";
    private static final String SAML_SIGNATURE_CANONICALIZATION = "saml_signature_canonicalization_method";
    private static final String SAML_SIGNING_CERTIFICATE = "saml.signing.certificate";
    private static final String SAML_ALLOW_ECP_FLOW = "saml.allow.ecp.flow";

    @InjectRealm(attachTo = "master", ref = "masterRealm")
    ManagedRealm managedMasterRealm;

    @TestOnServer
    public void fromModel_mapsBasicFields(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-saml-basic-client");
        try {
            clientModel.setProtocol(SAMLClientRepresentation.PROTOCOL);
            clientModel.setEnabled(true);
            clientModel.setDescription("Test SAML description");
            clientModel.setName("Test SAML Client");
            clientModel.setBaseUrl("http://localhost:8080/saml");
            clientModel.setRedirectUris(Set.of("http://localhost:8080/saml/callback"));

            SAMLClientModelMapper mapper = (SAMLClientModelMapper) session.getProvider(ClientModelMapper.class, SAMLClientRepresentation.PROTOCOL);
            BaseClientRepresentation rep = mapper.fromModel(clientModel);

            assertThat(rep, instanceOf(SAMLClientRepresentation.class));
            SAMLClientRepresentation samlRep = (SAMLClientRepresentation) rep;
            assertThat(samlRep.getEnabled(), is(true));
            assertThat(samlRep.getClientId(), is("test-saml-basic-client"));
            assertThat(samlRep.getDescription(), is("Test SAML description"));
            assertThat(samlRep.getDisplayName(), is("Test SAML Client"));
            assertThat(samlRep.getAppUrl(), is("http://localhost:8080/saml"));
            assertThat(samlRep.getRedirectUris(), contains("http://localhost:8080/saml/callback"));
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void fromModel_mapsRoles(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-saml-roles-client");
        try {
            setupBasicSamlClientModel(clientModel);
            RoleModel clientRole = clientModel.addRole("saml-client-role");

            SAMLClientModelMapper mapper = (SAMLClientModelMapper) session.getProvider(ClientModelMapper.class, SAMLClientRepresentation.PROTOCOL);
            BaseClientRepresentation rep = mapper.fromModel(clientModel);

            assertThat(rep.getRoles(), contains("saml-client-role"));
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void fromModel_mapsNameIdSettings(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-saml-nameid-client");
        try {
            setupBasicSamlClientModel(clientModel);
            clientModel.setAttribute(SAML_NAME_ID_FORMAT, "username");
            clientModel.setAttribute(SAML_FORCE_NAME_ID_FORMAT, "true");

            SAMLClientModelMapper mapper = (SAMLClientModelMapper) session.getProvider(ClientModelMapper.class, SAMLClientRepresentation.PROTOCOL);
            SAMLClientRepresentation rep = (SAMLClientRepresentation) mapper.fromModel(clientModel);

            assertThat(rep.getNameIdFormat(), is("username"));
            assertThat(rep.getForceNameIdFormat(), is(true));
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void fromModel_mapsSignatureSettings(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-saml-signature-client");
        try {
            setupBasicSamlClientModel(clientModel);
            clientModel.setAttribute(SAML_AUTHN_STATEMENT, "true");
            clientModel.setAttribute(SAML_SERVER_SIGNATURE, "true");
            clientModel.setAttribute(SAML_ASSERTION_SIGNATURE, "true");
            clientModel.setAttribute(SAML_CLIENT_SIGNATURE, "true");
            clientModel.setAttribute(SAML_SIGNATURE_ALGORITHM, "RSA_SHA256");
            clientModel.setAttribute(SAML_SIGNATURE_CANONICALIZATION, "http://www.w3.org/2001/10/xml-exc-c14n#");
            clientModel.setAttribute(SAML_SIGNING_CERTIFICATE, "MIICertificate");

            SAMLClientModelMapper mapper = (SAMLClientModelMapper) session.getProvider(ClientModelMapper.class, SAMLClientRepresentation.PROTOCOL);
            SAMLClientRepresentation rep = (SAMLClientRepresentation) mapper.fromModel(clientModel);

            assertThat(rep.getIncludeAuthnStatement(), is(true));
            assertThat(rep.getSignDocuments(), is(true));
            assertThat(rep.getSignAssertions(), is(true));
            assertThat(rep.getClientSignatureRequired(), is(true));
            assertThat(rep.getSignatureAlgorithm(), is("RSA_SHA256"));
            assertThat(rep.getSignatureCanonicalizationMethod(), is("http://www.w3.org/2001/10/xml-exc-c14n#"));
            assertThat(rep.getSigningCertificate(), is("MIICertificate"));
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void fromModel_mapsBindingAndLogoutSettings(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-saml-binding-client");
        try {
            setupBasicSamlClientModel(clientModel);
            clientModel.setAttribute(SAML_FORCE_POST_BINDING, "true");
            clientModel.setFrontchannelLogout(true);

            SAMLClientModelMapper mapper = (SAMLClientModelMapper) session.getProvider(ClientModelMapper.class, SAMLClientRepresentation.PROTOCOL);
            SAMLClientRepresentation rep = (SAMLClientRepresentation) mapper.fromModel(clientModel);

            assertThat(rep.getForcePostBinding(), is(true));
            assertThat(rep.getFrontChannelLogout(), is(true));
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void fromModel_mapsEcpFlow(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-saml-ecp-client");
        try {
            setupBasicSamlClientModel(clientModel);
            clientModel.setAttribute(SAML_ALLOW_ECP_FLOW, "true");

            SAMLClientModelMapper mapper = (SAMLClientModelMapper) session.getProvider(ClientModelMapper.class, SAMLClientRepresentation.PROTOCOL);
            SAMLClientRepresentation rep = (SAMLClientRepresentation) mapper.fromModel(clientModel);

            assertThat(rep.getAllowEcpFlow(), is(true));
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void fromModel_nullBooleanAttributesReturnNull(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-saml-null-attrs-client");
        try {
            setupBasicSamlClientModel(clientModel);
            // Don't set any SAML-specific attributes

            SAMLClientModelMapper mapper = (SAMLClientModelMapper) session.getProvider(ClientModelMapper.class, SAMLClientRepresentation.PROTOCOL);
            SAMLClientRepresentation rep = (SAMLClientRepresentation) mapper.fromModel(clientModel);

            assertThat(rep.getNameIdFormat(), nullValue());
            assertThat(rep.getForceNameIdFormat(), nullValue());
            assertThat(rep.getIncludeAuthnStatement(), nullValue());
            assertThat(rep.getSignDocuments(), nullValue());
            assertThat(rep.getSignAssertions(), nullValue());
            assertThat(rep.getClientSignatureRequired(), nullValue());
            assertThat(rep.getSignatureAlgorithm(), nullValue());
            assertThat(rep.getAllowEcpFlow(), nullValue());
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void toModel_setsBasicFields(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-saml-tomodel-basic");
        try {
            SAMLClientRepresentation rep = new SAMLClientRepresentation();
            rep.setEnabled(true);
            rep.setClientId("new-saml-client");
            rep.setDescription("New SAML description");
            rep.setDisplayName("New SAML Client");
            rep.setAppUrl("http://example.com/saml");
            rep.setRedirectUris(Set.of("http://example.com/saml/callback"));

            SAMLClientModelMapper mapper = (SAMLClientModelMapper) session.getProvider(ClientModelMapper.class, SAMLClientRepresentation.PROTOCOL);
            mapper.toModel(rep, clientModel);

            assertThat(clientModel.isEnabled(), is(true));
            assertThat(clientModel.getClientId(), is("new-saml-client"));
            assertThat(clientModel.getDescription(), is("New SAML description"));
            assertThat(clientModel.getName(), is("New SAML Client"));
            assertThat(clientModel.getBaseUrl(), is("http://example.com/saml"));
            assertThat(clientModel.getRedirectUris(), contains("http://example.com/saml/callback"));
            assertThat(clientModel.getProtocol(), is(SAMLClientRepresentation.PROTOCOL));
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void toModel_setsNameIdSettings(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-saml-tomodel-nameid");
        try {
            SAMLClientRepresentation rep = new SAMLClientRepresentation();
            rep.setEnabled(true);
            rep.setClientId("test-saml-tomodel-nameid");
            rep.setRedirectUris(Set.of());
            rep.setNameIdFormat("email");
            rep.setForceNameIdFormat(true);

            SAMLClientModelMapper mapper = (SAMLClientModelMapper) session.getProvider(ClientModelMapper.class, SAMLClientRepresentation.PROTOCOL);
            mapper.toModel(rep, clientModel);

            assertThat(clientModel.getAttribute(SAML_NAME_ID_FORMAT), is("email"));
            assertThat(clientModel.getAttribute(SAML_FORCE_NAME_ID_FORMAT), is("true"));
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void toModel_setsSignatureSettings(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-saml-tomodel-signature");
        try {
            SAMLClientRepresentation rep = new SAMLClientRepresentation();
            rep.setEnabled(true);
            rep.setClientId("test-saml-tomodel-signature");
            rep.setRedirectUris(Set.of());
            rep.setIncludeAuthnStatement(true);
            rep.setSignDocuments(true);
            rep.setSignAssertions(true);
            rep.setClientSignatureRequired(true);
            rep.setSignatureAlgorithm("RSA_SHA512");
            rep.setSignatureCanonicalizationMethod("http://www.w3.org/2001/10/xml-exc-c14n#WithComments");
            rep.setSigningCertificate("MIINewCertificate");

            SAMLClientModelMapper mapper = (SAMLClientModelMapper) session.getProvider(ClientModelMapper.class, SAMLClientRepresentation.PROTOCOL);
            mapper.toModel(rep, clientModel);

            assertThat(clientModel.getAttribute(SAML_AUTHN_STATEMENT), is("true"));
            assertThat(clientModel.getAttribute(SAML_SERVER_SIGNATURE), is("true"));
            assertThat(clientModel.getAttribute(SAML_ASSERTION_SIGNATURE), is("true"));
            assertThat(clientModel.getAttribute(SAML_CLIENT_SIGNATURE), is("true"));
            assertThat(clientModel.getAttribute(SAML_SIGNATURE_ALGORITHM), is("RSA_SHA512"));
            assertThat(clientModel.getAttribute(SAML_SIGNATURE_CANONICALIZATION), is("http://www.w3.org/2001/10/xml-exc-c14n#WithComments"));
            assertThat(clientModel.getAttribute(SAML_SIGNING_CERTIFICATE), is("MIINewCertificate"));
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void toModel_setsBindingAndLogoutSettings(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-saml-tomodel-binding");
        try {
            SAMLClientRepresentation rep = new SAMLClientRepresentation();
            rep.setEnabled(true);
            rep.setClientId("test-saml-tomodel-binding");
            rep.setRedirectUris(Set.of());
            rep.setForcePostBinding(true);
            rep.setFrontChannelLogout(true);

            SAMLClientModelMapper mapper = (SAMLClientModelMapper) session.getProvider(ClientModelMapper.class, SAMLClientRepresentation.PROTOCOL);
            mapper.toModel(rep, clientModel);

            assertThat(clientModel.getAttribute(SAML_FORCE_POST_BINDING), is("true"));
            assertThat(clientModel.isFrontchannelLogout(), is(true));
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void toModel_setsEcpFlow(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-saml-tomodel-ecp");
        try {
            SAMLClientRepresentation rep = new SAMLClientRepresentation();
            rep.setEnabled(true);
            rep.setClientId("test-saml-tomodel-ecp");
            rep.setRedirectUris(Set.of());
            rep.setAllowEcpFlow(true);

            SAMLClientModelMapper mapper = (SAMLClientModelMapper) session.getProvider(ClientModelMapper.class, SAMLClientRepresentation.PROTOCOL);
            mapper.toModel(rep, clientModel);

            assertThat(clientModel.getAttribute(SAML_ALLOW_ECP_FLOW), is("true"));
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void toModel_doesNotSetNullAttributes(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-saml-tomodel-null");
        try {
            // Pre-set some attributes
            clientModel.setAttribute(SAML_NAME_ID_FORMAT, "existing-format");
            clientModel.setAttribute(SAML_SIGNATURE_ALGORITHM, "existing-algorithm");

            SAMLClientRepresentation rep = new SAMLClientRepresentation();
            rep.setEnabled(true);
            rep.setClientId("test-saml-tomodel-null");
            rep.setRedirectUris(Set.of());
            // Leave SAML-specific fields null

            SAMLClientModelMapper mapper = (SAMLClientModelMapper) session.getProvider(ClientModelMapper.class, SAMLClientRepresentation.PROTOCOL);
            mapper.toModel(rep, clientModel);

            // Existing attributes should remain unchanged when rep values are null
            assertThat(clientModel.getAttribute(SAML_NAME_ID_FORMAT), is("existing-format"));
            assertThat(clientModel.getAttribute(SAML_SIGNATURE_ALGORITHM), is("existing-algorithm"));
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void toModel_handlesFalseBooleansCorrectly(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-saml-tomodel-false");
        try {
            SAMLClientRepresentation rep = new SAMLClientRepresentation();
            rep.setEnabled(false);
            rep.setClientId("test-saml-tomodel-false");
            rep.setRedirectUris(Set.of());
            rep.setForceNameIdFormat(false);
            rep.setIncludeAuthnStatement(false);
            rep.setSignDocuments(false);
            rep.setSignAssertions(false);
            rep.setClientSignatureRequired(false);
            rep.setForcePostBinding(false);
            rep.setFrontChannelLogout(false);
            rep.setAllowEcpFlow(false);

            SAMLClientModelMapper mapper = (SAMLClientModelMapper) session.getProvider(ClientModelMapper.class, SAMLClientRepresentation.PROTOCOL);
            mapper.toModel(rep, clientModel);

            assertThat(clientModel.isEnabled(), is(false));
            assertThat(clientModel.getAttribute(SAML_FORCE_NAME_ID_FORMAT), is("false"));
            assertThat(clientModel.getAttribute(SAML_AUTHN_STATEMENT), is("false"));
            assertThat(clientModel.getAttribute(SAML_SERVER_SIGNATURE), is("false"));
            assertThat(clientModel.getAttribute(SAML_ASSERTION_SIGNATURE), is("false"));
            assertThat(clientModel.getAttribute(SAML_CLIENT_SIGNATURE), is("false"));
            assertThat(clientModel.getAttribute(SAML_FORCE_POST_BINDING), is("false"));
            assertThat(clientModel.isFrontchannelLogout(), is(false));
            assertThat(clientModel.getAttribute(SAML_ALLOW_ECP_FLOW), is("false"));
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void close_doesNotThrow(KeycloakSession session) {
        SAMLClientModelMapper mapper = (SAMLClientModelMapper) session.getProvider(ClientModelMapper.class, SAMLClientRepresentation.PROTOCOL);
        // Just verify close doesn't throw any exception
        mapper.close();
    }

    private void setupBasicSamlClientModel(ClientModel clientModel) {
        clientModel.setProtocol(SAMLClientRepresentation.PROTOCOL);
        clientModel.setEnabled(true);
        clientModel.setDescription("Test SAML description");
        clientModel.setName("Test SAML Client");
        clientModel.setBaseUrl("http://localhost:8080/saml");
        clientModel.setRedirectUris(Set.of());
    }

    public static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_ADMIN_API_V2);
        }
    }
}
