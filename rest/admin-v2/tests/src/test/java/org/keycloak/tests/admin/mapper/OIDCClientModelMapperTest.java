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
import org.keycloak.models.UserModel;
import org.keycloak.models.mapper.ClientModelMapper;
import org.keycloak.models.mapper.OIDCClientModelMapper;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@KeycloakIntegrationTest(config = OIDCClientModelMapperTest.ServerConfig.class)
public class OIDCClientModelMapperTest {

    @InjectRealm
    ManagedRealm realm;

    @TestOnServer
    public void fromModel_mapsBasicFields(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-basic-client");
        try {
            clientModel.setEnabled(true);
            clientModel.setDescription("Test description");
            clientModel.setName("Test Client");
            clientModel.setBaseUrl("http://localhost:8080");
            clientModel.setRedirectUris(Set.of("http://localhost:8080/callback"));
            clientModel.setPublicClient(true);
            clientModel.setStandardFlowEnabled(false);
            clientModel.setImplicitFlowEnabled(false);
            clientModel.setDirectAccessGrantsEnabled(false);
            clientModel.setServiceAccountsEnabled(false);
            clientModel.setWebOrigins(Set.of());

            OIDCClientModelMapper mapper = (OIDCClientModelMapper) session.getProvider(ClientModelMapper.class, OIDCClientRepresentation.PROTOCOL);
            BaseClientRepresentation rep = mapper.fromModel(clientModel);

            assertThat(rep, instanceOf(OIDCClientRepresentation.class));
            OIDCClientRepresentation oidcRep = (OIDCClientRepresentation) rep;
            assertThat(oidcRep.getEnabled(), is(true));
            assertThat(oidcRep.getClientId(), is("test-basic-client"));
            assertThat(oidcRep.getDescription(), is("Test description"));
            assertThat(oidcRep.getDisplayName(), is("Test Client"));
            assertThat(oidcRep.getAppUrl(), is("http://localhost:8080"));
            assertThat(oidcRep.getRedirectUris(), contains("http://localhost:8080/callback"));
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void fromModel_mapsRoles(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-roles-client");
        try {
            setupBasicClientModel(clientModel);
            RoleModel clientRole = clientModel.addRole("client-role");

            OIDCClientModelMapper mapper = (OIDCClientModelMapper) session.getProvider(ClientModelMapper.class, OIDCClientRepresentation.PROTOCOL);
            BaseClientRepresentation rep = mapper.fromModel(clientModel);

            assertThat(rep.getRoles(), contains("client-role"));
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void fromModel_mapsLoginFlows(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-flows-client");
        try {
            setupBasicClientModel(clientModel);
            clientModel.setStandardFlowEnabled(true);
            clientModel.setImplicitFlowEnabled(true);
            clientModel.setDirectAccessGrantsEnabled(true);
            // Note: serviceAccountsEnabled is not set here as it requires a service account user to exist
            // for the mapper to work correctly. The SERVICE_ACCOUNT flow is tested separately.

            OIDCClientModelMapper mapper = (OIDCClientModelMapper) session.getProvider(ClientModelMapper.class, OIDCClientRepresentation.PROTOCOL);
            OIDCClientRepresentation rep = (OIDCClientRepresentation) mapper.fromModel(clientModel);

            assertThat(rep.getLoginFlows(), containsInAnyOrder(
                    OIDCClientRepresentation.Flow.STANDARD, OIDCClientRepresentation.Flow.IMPLICIT, OIDCClientRepresentation.Flow.DIRECT_GRANT
            ));
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void fromModel_mapsAuthForConfidentialClient(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-confidential-client");
        try {
            setupBasicClientModel(clientModel);
            clientModel.setPublicClient(false);
            clientModel.setClientAuthenticatorType("client-secret");
            clientModel.setSecret("my-secret");

            OIDCClientModelMapper mapper = (OIDCClientModelMapper) session.getProvider(ClientModelMapper.class, OIDCClientRepresentation.PROTOCOL);
            OIDCClientRepresentation rep = (OIDCClientRepresentation) mapper.fromModel(clientModel);

            assertThat(rep.getAuth(), notNullValue());
            assertThat(rep.getAuth().getMethod(), is("client-secret"));
            assertThat(rep.getAuth().getSecret(), is("my-secret"));
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void fromModel_noAuthForPublicClient(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-public-client");
        try {
            setupBasicClientModel(clientModel);
            clientModel.setPublicClient(true);

            OIDCClientModelMapper mapper = (OIDCClientModelMapper) session.getProvider(ClientModelMapper.class, OIDCClientRepresentation.PROTOCOL);
            OIDCClientRepresentation rep = (OIDCClientRepresentation) mapper.fromModel(clientModel);

            assertThat(rep.getAuth(), nullValue());
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void fromModel_mapsWebOrigins(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-origins-client");
        try {
            setupBasicClientModel(clientModel);
            clientModel.setWebOrigins(Set.of("http://localhost:3000", "http://localhost:4000"));

            OIDCClientModelMapper mapper = (OIDCClientModelMapper) session.getProvider(ClientModelMapper.class, OIDCClientRepresentation.PROTOCOL);
            OIDCClientRepresentation rep = (OIDCClientRepresentation) mapper.fromModel(clientModel);

            assertThat(rep.getWebOrigins(), containsInAnyOrder("http://localhost:3000", "http://localhost:4000"));
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void fromModel_mapsServiceAccountRoles(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-sa-roles-client");
        RoleModel role1 = null;
        RoleModel role2 = null;
        try {
            setupBasicClientModel(clientModel);
            clientModel.setServiceAccountsEnabled(true);

            // Create service account user (normally done by ClientManager.enableServiceAccount)
            String username = "service-account-" + clientModel.getClientId();
            UserModel serviceAccount = session.users().addUser(realm, username);
            serviceAccount.setEnabled(true);
            serviceAccount.setServiceAccountClientLink(clientModel.getId());

            // Create realm roles for testing
            role1 = realm.addRole("test-role-1");
            role2 = realm.addRole("test-role-2");

            // Assign roles to service account
            serviceAccount.grantRole(role1);
            serviceAccount.grantRole(role2);

            OIDCClientModelMapper mapper = (OIDCClientModelMapper) session.getProvider(ClientModelMapper.class, OIDCClientRepresentation.PROTOCOL);
            OIDCClientRepresentation rep = (OIDCClientRepresentation) mapper.fromModel(clientModel);

            assertThat(rep.getServiceAccountRoles(), hasItems("test-role-1", "test-role-2"));
        } finally {
            realm.removeClient(clientModel.getId());
            if (role1 != null) realm.removeRole(role1);
            if (role2 != null) realm.removeRole(role2);
        }
    }

    @TestOnServer
    public void fromModel_emptyServiceAccountRolesWhenServiceAccountDisabled(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-no-sa-client");
        try {
            setupBasicClientModel(clientModel);
            clientModel.setServiceAccountsEnabled(false);

            OIDCClientModelMapper mapper = (OIDCClientModelMapper) session.getProvider(ClientModelMapper.class, OIDCClientRepresentation.PROTOCOL);
            OIDCClientRepresentation rep = (OIDCClientRepresentation) mapper.fromModel(clientModel);

            assertThat(rep.getServiceAccountRoles(), empty());
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void toModel_setsBasicFields(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-tomodel-basic");
        try {
            OIDCClientRepresentation rep = new OIDCClientRepresentation();
            rep.setEnabled(true);
            rep.setClientId("new-client");
            rep.setDescription("New description");
            rep.setDisplayName("New Client");
            rep.setAppUrl("http://example.com");
            rep.setRedirectUris(Set.of("http://example.com/callback"));
            rep.setWebOrigins(Set.of("http://example.com"));
            rep.setLoginFlows(Set.of());

            OIDCClientModelMapper mapper = (OIDCClientModelMapper) session.getProvider(ClientModelMapper.class, OIDCClientRepresentation.PROTOCOL);
            mapper.toModel(rep, clientModel);

            assertThat(clientModel.isEnabled(), is(true));
            assertThat(clientModel.getClientId(), is("new-client"));
            assertThat(clientModel.getDescription(), is("New description"));
            assertThat(clientModel.getName(), is("New Client"));
            assertThat(clientModel.getBaseUrl(), is("http://example.com"));
            assertThat(clientModel.getRedirectUris(), contains("http://example.com/callback"));
            assertThat(clientModel.getWebOrigins(), contains("http://example.com"));
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void toModel_setsLoginFlows(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-tomodel-flows");
        try {
            OIDCClientRepresentation rep = new OIDCClientRepresentation();
            rep.setEnabled(true);
            rep.setClientId("test-tomodel-flows");
            rep.setLoginFlows(Set.of(
                    OIDCClientRepresentation.Flow.STANDARD, OIDCClientRepresentation.Flow.IMPLICIT, OIDCClientRepresentation.Flow.DIRECT_GRANT
            ));
            rep.setRedirectUris(Set.of());
            rep.setWebOrigins(Set.of());

            OIDCClientModelMapper mapper = (OIDCClientModelMapper) session.getProvider(ClientModelMapper.class, OIDCClientRepresentation.PROTOCOL);
            mapper.toModel(rep, clientModel);

            assertThat(clientModel.isStandardFlowEnabled(), is(true));
            assertThat(clientModel.isImplicitFlowEnabled(), is(true));
            assertThat(clientModel.isDirectAccessGrantsEnabled(), is(true));
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void toModel_setsFlowsToFalseWhenNotInSet(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-tomodel-noflows");
        try {
            // First enable all flows
            clientModel.setStandardFlowEnabled(true);
            clientModel.setImplicitFlowEnabled(true);
            clientModel.setDirectAccessGrantsEnabled(true);

            OIDCClientRepresentation rep = new OIDCClientRepresentation();
            rep.setEnabled(false);
            rep.setClientId("test-tomodel-noflows");
            rep.setLoginFlows(Set.of());
            rep.setRedirectUris(Set.of());
            rep.setWebOrigins(Set.of());

            OIDCClientModelMapper mapper = (OIDCClientModelMapper) session.getProvider(ClientModelMapper.class, OIDCClientRepresentation.PROTOCOL);
            mapper.toModel(rep, clientModel);

            assertThat(clientModel.isStandardFlowEnabled(), is(false));
            assertThat(clientModel.isImplicitFlowEnabled(), is(false));
            assertThat(clientModel.isDirectAccessGrantsEnabled(), is(false));
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void toModel_setsConfidentialClientAuth(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-tomodel-confidential");
        try {
            OIDCClientRepresentation rep = new OIDCClientRepresentation();
            rep.setEnabled(true);
            rep.setClientId("test-tomodel-confidential");
            rep.setRedirectUris(Set.of());
            rep.setWebOrigins(Set.of());
            rep.setLoginFlows(Set.of());

            OIDCClientRepresentation.Auth auth = new OIDCClientRepresentation.Auth();
            auth.setMethod("client-jwt");
            auth.setSecret("jwt-secret");
            rep.setAuth(auth);

            OIDCClientModelMapper mapper = (OIDCClientModelMapper) session.getProvider(ClientModelMapper.class, OIDCClientRepresentation.PROTOCOL);
            mapper.toModel(rep, clientModel);

            assertThat(clientModel.isPublicClient(), is(false));
            assertThat(clientModel.getClientAuthenticatorType(), is("client-jwt"));
            assertThat(clientModel.getSecret(), is("jwt-secret"));
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void toModel_setsPublicClientWhenNoAuth(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-tomodel-public");
        try {
            // Start as confidential client
            clientModel.setPublicClient(false);

            OIDCClientRepresentation rep = new OIDCClientRepresentation();
            rep.setEnabled(true);
            rep.setClientId("test-tomodel-public");
            rep.setRedirectUris(Set.of());
            rep.setWebOrigins(Set.of());
            rep.setLoginFlows(Set.of());
            rep.setAuth(null);

            OIDCClientModelMapper mapper = (OIDCClientModelMapper) session.getProvider(ClientModelMapper.class, OIDCClientRepresentation.PROTOCOL);
            mapper.toModel(rep, clientModel);

            assertThat(clientModel.isPublicClient(), is(true));
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void toModel_handlesNullEnabled(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("master");
        session.getContext().setRealm(realm);

        ClientModel clientModel = realm.addClient("test-tomodel-nullenabled");
        try {
            // Start as enabled
            clientModel.setEnabled(true);

            OIDCClientRepresentation rep = new OIDCClientRepresentation();
            rep.setEnabled(null);
            rep.setClientId("test-tomodel-nullenabled");
            rep.setRedirectUris(Set.of());
            rep.setWebOrigins(Set.of());
            rep.setLoginFlows(Set.of());

            OIDCClientModelMapper mapper = (OIDCClientModelMapper) session.getProvider(ClientModelMapper.class, OIDCClientRepresentation.PROTOCOL);
            mapper.toModel(rep, clientModel);

            assertThat(clientModel.isEnabled(), is(false));
        } finally {
            realm.removeClient(clientModel.getId());
        }
    }

    @TestOnServer
    public void close_doesNotThrow(KeycloakSession session) {
        OIDCClientModelMapper mapper = (OIDCClientModelMapper) session.getProvider(ClientModelMapper.class, OIDCClientRepresentation.PROTOCOL);
        // Just verify close doesn't throw any exception
        mapper.close();
    }

    private void setupBasicClientModel(ClientModel clientModel) {
        clientModel.setEnabled(true);
        clientModel.setDescription("Test description");
        clientModel.setName("Test Client");
        clientModel.setBaseUrl("http://localhost:8080");
        clientModel.setRedirectUris(Set.of());
        clientModel.setPublicClient(true);
        clientModel.setStandardFlowEnabled(false);
        clientModel.setImplicitFlowEnabled(false);
        clientModel.setDirectAccessGrantsEnabled(false);
        clientModel.setServiceAccountsEnabled(false);
        clientModel.setWebOrigins(Set.of());
    }

    public static class ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CLIENT_ADMIN_API_V2);
        }
    }
}
