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
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.mapper.OIDCClientModelMapper;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OIDCClientModelMapperTest {

    @Mock
    private KeycloakSession session;

    @Mock
    private KeycloakContext context;

    @Mock
    private RealmModel realm;

    @Mock
    private ClientModel clientModel;

    @Mock
    private UserProvider userProvider;

    @Mock
    private UserModel serviceAccount;

    @Mock
    private RoleModel role1;

    @Mock
    private RoleModel role2;

    @Mock
    private RoleModel clientRole1;

    private OIDCClientModelMapper mapper;

    @BeforeEach
    void setUp() {
        lenient().when(session.getContext()).thenReturn(context);
        lenient().when(context.getRealm()).thenReturn(realm);
        lenient().when(session.users()).thenReturn(userProvider);
        mapper = new OIDCClientModelMapper(session);
    }

    @Test
    void fromModel_mapsBasicFields() {
        when(clientModel.isEnabled()).thenReturn(true);
        when(clientModel.getClientId()).thenReturn("test-client");
        when(clientModel.getDescription()).thenReturn("Test description");
        when(clientModel.getName()).thenReturn("Test Client");
        when(clientModel.getBaseUrl()).thenReturn("http://localhost:8080");
        when(clientModel.getRedirectUris()).thenReturn(Set.of("http://localhost:8080/callback"));
        when(clientModel.getRolesStream()).thenReturn(Stream.empty());
        when(clientModel.isPublicClient()).thenReturn(true);
        when(clientModel.isStandardFlowEnabled()).thenReturn(false);
        when(clientModel.isImplicitFlowEnabled()).thenReturn(false);
        when(clientModel.isDirectAccessGrantsEnabled()).thenReturn(false);
        when(clientModel.isServiceAccountsEnabled()).thenReturn(false);
        when(clientModel.getWebOrigins()).thenReturn(Set.of());

        BaseClientRepresentation rep = mapper.fromModel(clientModel);

        assertThat(rep, instanceOf(OIDCClientRepresentation.class));
        OIDCClientRepresentation oidcRep = (OIDCClientRepresentation) rep;
        assertThat(oidcRep.getEnabled(), is(true));
        assertThat(oidcRep.getClientId(), is("test-client"));
        assertThat(oidcRep.getDescription(), is("Test description"));
        assertThat(oidcRep.getDisplayName(), is("Test Client"));
        assertThat(oidcRep.getAppUrl(), is("http://localhost:8080"));
        assertThat(oidcRep.getRedirectUris(), contains("http://localhost:8080/callback"));
    }

    @Test
    void fromModel_mapsRoles() {
        setupBasicClientModel();
        when(clientModel.getRolesStream()).thenReturn(Stream.of(clientRole1));
        when(clientRole1.getName()).thenReturn("client-role");

        BaseClientRepresentation rep = mapper.fromModel(clientModel);

        assertThat(rep.getRoles(), contains("client-role"));
    }

    @Test
    void fromModel_mapsLoginFlows() {
        setupBasicClientModel();
        when(clientModel.isStandardFlowEnabled()).thenReturn(true);
        when(clientModel.isImplicitFlowEnabled()).thenReturn(true);
        when(clientModel.isDirectAccessGrantsEnabled()).thenReturn(true);
        when(clientModel.isServiceAccountsEnabled()).thenReturn(true);
        when(userProvider.getServiceAccount(clientModel)).thenReturn(serviceAccount);
        when(serviceAccount.getRoleMappingsStream()).thenReturn(Stream.empty());

        OIDCClientRepresentation rep = (OIDCClientRepresentation) mapper.fromModel(clientModel);

        assertThat(rep.getLoginFlows(), containsInAnyOrder(
                OIDCClientRepresentation.Flow.STANDARD, OIDCClientRepresentation.Flow.IMPLICIT, OIDCClientRepresentation.Flow.DIRECT_GRANT, OIDCClientRepresentation.Flow.SERVICE_ACCOUNT
        ));
    }

    @Test
    void fromModel_mapsAuthForConfidentialClient() {
        setupBasicClientModel();
        when(clientModel.isPublicClient()).thenReturn(false);
        when(clientModel.getClientAuthenticatorType()).thenReturn("client-secret");
        when(clientModel.getSecret()).thenReturn("my-secret");

        OIDCClientRepresentation rep = (OIDCClientRepresentation) mapper.fromModel(clientModel);

        assertThat(rep.getAuth(), notNullValue());
        assertThat(rep.getAuth().getMethod(), is("client-secret"));
        assertThat(rep.getAuth().getSecret(), is("my-secret"));
    }

    @Test
    void fromModel_noAuthForPublicClient() {
        setupBasicClientModel();
        when(clientModel.isPublicClient()).thenReturn(true);

        OIDCClientRepresentation rep = (OIDCClientRepresentation) mapper.fromModel(clientModel);

        assertThat(rep.getAuth(), nullValue());
    }

    @Test
    void fromModel_mapsWebOrigins() {
        setupBasicClientModel();
        when(clientModel.getWebOrigins()).thenReturn(Set.of("http://localhost:3000", "http://localhost:4000"));

        OIDCClientRepresentation rep = (OIDCClientRepresentation) mapper.fromModel(clientModel);

        assertThat(rep.getWebOrigins(), containsInAnyOrder("http://localhost:3000", "http://localhost:4000"));
    }

    @Test
    void fromModel_mapsServiceAccountRoles() {
        setupBasicClientModel();
        when(clientModel.isServiceAccountsEnabled()).thenReturn(true);
        when(userProvider.getServiceAccount(clientModel)).thenReturn(serviceAccount);
        when(serviceAccount.getRoleMappingsStream()).thenReturn(Stream.of(role1, role2));
        when(role1.getName()).thenReturn("role-1");
        when(role2.getName()).thenReturn("role-2");

        OIDCClientRepresentation rep = (OIDCClientRepresentation) mapper.fromModel(clientModel);

        assertThat(rep.getServiceAccountRoles(), containsInAnyOrder("role-1", "role-2"));
    }

    @Test
    void fromModel_emptyServiceAccountRolesWhenServiceAccountDisabled() {
        setupBasicClientModel();
        when(clientModel.isServiceAccountsEnabled()).thenReturn(false);

        OIDCClientRepresentation rep = (OIDCClientRepresentation) mapper.fromModel(clientModel);

        assertThat(rep.getServiceAccountRoles(), empty());
    }

    @Test
    void toModel_setsBasicFields() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setEnabled(true);
        rep.setClientId("new-client");
        rep.setDescription("New description");
        rep.setDisplayName("New Client");
        rep.setAppUrl("http://example.com");
        rep.setRedirectUris(Set.of("http://example.com/callback"));
        rep.setWebOrigins(Set.of("http://example.com"));

        mapper.toModel(rep, clientModel);

        verify(clientModel).setEnabled(true);
        verify(clientModel).setClientId("new-client");
        verify(clientModel).setDescription("New description");
        verify(clientModel).setName("New Client");
        verify(clientModel).setBaseUrl("http://example.com");
        verify(clientModel).setRedirectUris(Set.of("http://example.com/callback"));
        verify(clientModel).setWebOrigins(Set.of("http://example.com"));
    }

    @Test
    void toModel_setsLoginFlows() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setEnabled(true);
        rep.setLoginFlows(Set.of(
                OIDCClientRepresentation.Flow.STANDARD, OIDCClientRepresentation.Flow.IMPLICIT, OIDCClientRepresentation.Flow.DIRECT_GRANT
        ));
        rep.setRedirectUris(Set.of());
        rep.setWebOrigins(Set.of());

        mapper.toModel(rep, clientModel);

        verify(clientModel).setStandardFlowEnabled(true);
        verify(clientModel).setImplicitFlowEnabled(true);
        verify(clientModel).setDirectAccessGrantsEnabled(true);
    }

    @Test
    void toModel_setsFlowsToFalseWhenNotInSet() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setEnabled(false);
        rep.setLoginFlows(Set.of());
        rep.setRedirectUris(Set.of());
        rep.setWebOrigins(Set.of());

        mapper.toModel(rep, clientModel);

        verify(clientModel).setStandardFlowEnabled(false);
        verify(clientModel).setImplicitFlowEnabled(false);
        verify(clientModel).setDirectAccessGrantsEnabled(false);
    }

    @Test
    void toModel_setsConfidentialClientAuth() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setEnabled(true);
        rep.setRedirectUris(Set.of());
        rep.setWebOrigins(Set.of());
        rep.setLoginFlows(Set.of());

        OIDCClientRepresentation.Auth auth = new OIDCClientRepresentation.Auth();
        auth.setMethod("client-jwt");
        auth.setSecret("jwt-secret");
        rep.setAuth(auth);

        mapper.toModel(rep, clientModel);

        verify(clientModel).setPublicClient(false);
        verify(clientModel).setClientAuthenticatorType("client-jwt");
        verify(clientModel).setSecret("jwt-secret");
    }

    @Test
    void toModel_setsPublicClientWhenNoAuth() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setEnabled(true);
        rep.setRedirectUris(Set.of());
        rep.setWebOrigins(Set.of());
        rep.setLoginFlows(Set.of());
        rep.setAuth(null);

        mapper.toModel(rep, clientModel);

        verify(clientModel).setPublicClient(true);
    }

    @Test
    void toModel_handlesNullEnabled() {
        OIDCClientRepresentation rep = new OIDCClientRepresentation();
        rep.setEnabled(null);
        rep.setRedirectUris(Set.of());
        rep.setWebOrigins(Set.of());
        rep.setLoginFlows(Set.of());

        mapper.toModel(rep, clientModel);

        verify(clientModel).setEnabled(false);
    }

    @Test
    void close_doesNotThrow() {
        // Just verify close doesn't throw any exception
        mapper.close();
    }

    private void setupBasicClientModel() {
        when(clientModel.isEnabled()).thenReturn(true);
        when(clientModel.getClientId()).thenReturn("test-client");
        when(clientModel.getDescription()).thenReturn("Test description");
        when(clientModel.getName()).thenReturn("Test Client");
        when(clientModel.getBaseUrl()).thenReturn("http://localhost:8080");
        when(clientModel.getRedirectUris()).thenReturn(Set.of());
        when(clientModel.getRolesStream()).thenReturn(Stream.empty());
        when(clientModel.isPublicClient()).thenReturn(true);
        when(clientModel.isStandardFlowEnabled()).thenReturn(false);
        when(clientModel.isImplicitFlowEnabled()).thenReturn(false);
        when(clientModel.isDirectAccessGrantsEnabled()).thenReturn(false);
        when(clientModel.isServiceAccountsEnabled()).thenReturn(false);
        when(clientModel.getWebOrigins()).thenReturn(Set.of());
    }
}
