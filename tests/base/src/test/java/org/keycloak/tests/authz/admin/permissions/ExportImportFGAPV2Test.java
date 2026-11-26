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
package org.keycloak.tests.authz.admin.permissions;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.exportimport.util.ExportUtils;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.MembershipType;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectOrganization;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedOrganization;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.admin.authz.fgap.PermissionTestUtils;
import org.keycloak.tests.organization.authz.fgap.OrganizationFgapConfig;
import org.keycloak.tests.utils.admin.AdminApiUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.ORGANIZATIONS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = ExportImportFGAPV2Test.KeycloakAdminPermissionsV2ServerConfig.class)
public class ExportImportFGAPV2Test {

    @InjectAdminClient(mode = InjectAdminClient.Mode.BOOTSTRAP)
    Keycloak adminClient;

    @InjectRealm(config = OrganizationFgapConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectOrganization(lifecycle = LifeCycle.METHOD)
    ManagedOrganization organization;

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    private final String REALM_NAME = "fgap";
    private final String CUSTOM_CLIENT_ID = "imported-permission-client";

    @AfterEach
    public void cleanup() {
        try {
            adminClient.realm(REALM_NAME).remove();
        } catch (NotFoundException ignore) {
            // tests using the injected realm do not create the "fgap" realm
        }
    }

    @Test
    public void testCreateRealmWithSwitchOnAndNoAdminPermissionsClient() {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm(REALM_NAME);
        rep.setAdminPermissionsEnabled(Boolean.TRUE);

        adminClient.realms().create(rep);

        assertTrue(adminClient.realm(REALM_NAME).toRepresentation().isAdminPermissionsEnabled());

        //admin-permissions client should be created
        ClientRepresentation client = adminClient.realm(REALM_NAME).toRepresentation().getAdminPermissionsClient();

        assertThat(client, notNullValue());

        ResourceServerRepresentation authorizationSettings = adminClient.realm(REALM_NAME).clients().get(client.getId()).authorization().getSettings();
        assertThat(authorizationSettings, notNullValue());
        assertThat(authorizationSettings.getAuthorizationSchema(), notNullValue());
    }

    @Test
    public void testCreateRealmWithSwitchOnAndAdminPermissionsClient() {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm(REALM_NAME);
        rep.setAdminPermissionsEnabled(Boolean.TRUE);

        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId(CUSTOM_CLIENT_ID);
        clientRep.setServiceAccountsEnabled(Boolean.TRUE);

        rep.setAdminPermissionsClient(clientRep);
        rep.setClients(List.of(clientRep));

        adminClient.realms().create(rep);

        assertTrue(adminClient.realm(REALM_NAME).toRepresentation().isAdminPermissionsEnabled());

        //admin-permissions client (imported-permission-client) should be created
        ClientRepresentation client = adminClient.realm(REALM_NAME).toRepresentation().getAdminPermissionsClient();

        assertThat(client, notNullValue());
        assertThat(client.getClientId(), equalTo(CUSTOM_CLIENT_ID));

        ResourceServerRepresentation authorizationSettings = adminClient.realm(REALM_NAME).clients().get(client.getId()).authorization().getSettings();
        assertThat(authorizationSettings, notNullValue());
        assertThat(authorizationSettings.getAuthorizationSchema(), notNullValue());
    }

    @Test
    public void testCreateRealmWithSwitchOffAndAdminPermissionsClient() {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm(REALM_NAME);

        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId(CUSTOM_CLIENT_ID);
        clientRep.setServiceAccountsEnabled(Boolean.TRUE);

        rep.setAdminPermissionsClient(clientRep);
        rep.setClients(List.of(clientRep));

        adminClient.realms().create(rep);

        assertFalse(adminClient.realm(REALM_NAME).toRepresentation().isAdminPermissionsEnabled());

        //admin-permissions client (imported-permission-client) should be created
        ClientRepresentation client = adminClient.realm(REALM_NAME).toRepresentation().getAdminPermissionsClient();

        assertThat(client, notNullValue());
        assertThat(client.getClientId(), equalTo(CUSTOM_CLIENT_ID));

        ResourceServerRepresentation authorizationSettings = adminClient.realm(REALM_NAME).clients().get(client.getId()).authorization().getSettings();
        assertThat(authorizationSettings, notNullValue());

        // schema should not be available as the realm switch is off
        assertThat(authorizationSettings.getAuthorizationSchema(), nullValue());
    }

    @Test
    public void testImportRealmWithPermissionForOrganizationLinkedToIdentityProvider() {
        String idpAlias = "org-broker";
        String orgId = organization.getId();
        OrganizationResource orgResource = organization.admin();

        IdentityProviderRepresentation idp = IdentityProviderBuilder.create()
                .providerId(OIDCIdentityProviderFactory.PROVIDER_ID)
                .alias(idpAlias)
                .attribute("clientId", "broker-app")
                .attribute("clientSecret", "broker-secret")
                .build();
        realm.admin().identityProviders().create(idp).close();
        try (Response response = orgResource.identityProviders().addIdentityProvider(idpAlias)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        String memberId;
        try (Response response = realm.admin().users().create(UserBuilder.create().username("member").email("member@orga.org").build())) {
            memberId = ApiUtil.getCreatedId(response);
        }
        FederatedIdentityRepresentation identity = new FederatedIdentityRepresentation();
        identity.setUserId("external-member-id");
        identity.setUserName("member");
        try (Response response = realm.admin().users().get(memberId).addFederatedIdentity(idpAlias, identity)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
        try (Response response = orgResource.members().addMember(memberId)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
        try (Response response = orgResource.members().member(memberId).updateMembershipType(MembershipType.MANAGED)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        ClientResource permissionsClient = AdminApiUtil.findClientByClientId(realm.admin(), Constants.ADMIN_PERMISSIONS_CLIENT_ID);
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation policy = PermissionTestUtils.createUserPolicy(realm, permissionsClient, "Allow My Admin", myadmin.getId());
        ScopePermissionRepresentation permission = PermissionTestUtils.createPermission(permissionsClient, orgId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW), policy);

        String realmName = realm.getName();
        RealmRepresentation exported = runOnServer.fetch(session -> ExportUtils.exportRealm(session, session.realms().getRealmByName(realmName), true, true), RealmRepresentation.class);

        // import the export as if into a fresh server, where the organization does not exist yet
        realm.admin().remove();
        adminClient.realms().create(exported);

        orgResource = realm.admin().organizations().get(orgId);
        assertThat(orgResource.identityProviders().getIdentityProviders().stream().map(IdentityProviderRepresentation::getAlias).toList(), contains(idpAlias));
        MemberRepresentation member = orgResource.members().member(memberId).toRepresentation();
        assertEquals(MembershipType.MANAGED, member.getMembershipType());
        assertThat(realm.admin().users().get(memberId).getFederatedIdentity().stream().map(FederatedIdentityRepresentation::getIdentityProvider).toList(), contains(idpAlias));

        permissionsClient = AdminApiUtil.findClientByClientId(realm.admin(), Constants.ADMIN_PERMISSIONS_CLIENT_ID);
        ScopePermissionRepresentation imported = permissionsClient.authorization().permissions().scope().findByName(permission.getName());
        assertNotNull(imported);
        assertThat(imported.getResourceType(), equalTo(ORGANIZATIONS_RESOURCE_TYPE));
        List<String> resources = permissionsClient.authorization().permissions().scope().findById(imported.getId()).resources()
                .stream().map(ResourceRepresentation::getName).toList();
        assertThat(resources, contains(orgId));
    }

    public static class KeycloakAdminPermissionsV2ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2);
        }
    }
}
