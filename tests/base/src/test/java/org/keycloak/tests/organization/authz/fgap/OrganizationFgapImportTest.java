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

package org.keycloak.tests.organization.authz.fgap;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.exportimport.util.ExportUtils;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.MembershipType;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.admin.authz.fgap.PermissionTestUtils;
import org.keycloak.tests.utils.admin.AdminApiUtil;

import org.junit.jupiter.api.Test;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.ORGANIZATIONS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.VIEW;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest
public class OrganizationFgapImportTest {

    private static final String IDP_ALIAS = "org-broker";

    @InjectRealm(config = OrganizationFgapConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    @Test
    public void testImportRealmWithPermissionForSpecificOrganization() {
        String orgId = createOrg("orgA");
        ScopePermissionRepresentation permission = createViewPermission(orgId);

        exportAndReimportRealm();

        OrganizationRepresentation org = realm.admin().organizations().get(orgId).toRepresentation();
        assertEquals("orgA", org.getName());
        assertPermissionImported(permission, orgId);
    }

    @Test
    public void testImportRealmWithPermissionForOrganizationLinkedToIdentityProvider() {
        String orgId = createOrg("orgA");
        OrganizationResource orgResource = realm.admin().organizations().get(orgId);

        IdentityProviderRepresentation idp = IdentityProviderBuilder.create()
                .providerId(OIDCIdentityProviderFactory.PROVIDER_ID)
                .alias(IDP_ALIAS)
                .attribute("clientId", "broker-app")
                .attribute("clientSecret", "broker-secret")
                .build();
        realm.admin().identityProviders().create(idp).close();
        try (Response response = orgResource.identityProviders().addIdentityProvider(IDP_ALIAS)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        String memberId;
        try (Response response = realm.admin().users().create(UserBuilder.create().username("member").email("member@orga.org").build())) {
            memberId = ApiUtil.getCreatedId(response);
        }
        FederatedIdentityRepresentation identity = new FederatedIdentityRepresentation();
        identity.setUserId("external-member-id");
        identity.setUserName("member");
        try (Response response = realm.admin().users().get(memberId).addFederatedIdentity(IDP_ALIAS, identity)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
        try (Response response = orgResource.members().addMember(memberId)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
        try (Response response = orgResource.members().member(memberId).updateMembershipType(MembershipType.MANAGED)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        ScopePermissionRepresentation permission = createViewPermission(orgId);

        exportAndReimportRealm();

        orgResource = realm.admin().organizations().get(orgId);
        assertThat(orgResource.identityProviders().getIdentityProviders().stream().map(IdentityProviderRepresentation::getAlias).toList(), contains(IDP_ALIAS));
        MemberRepresentation member = orgResource.members().member(memberId).toRepresentation();
        assertEquals(MembershipType.MANAGED, member.getMembershipType());
        assertThat(realm.admin().users().get(memberId).getFederatedIdentity().stream().map(FederatedIdentityRepresentation::getIdentityProvider).toList(), contains(IDP_ALIAS));
        assertPermissionImported(permission, orgId);
    }

    private ScopePermissionRepresentation createViewPermission(String orgId) {
        ClientResource permissionsClient = AdminApiUtil.findClientByClientId(realm.admin(), Constants.ADMIN_PERMISSIONS_CLIENT_ID);
        UserRepresentation myadmin = realm.admin().users().search("myadmin").get(0);
        UserPolicyRepresentation policy = PermissionTestUtils.createUserPolicy(realm, permissionsClient, "Allow My Admin", myadmin.getId());
        return PermissionTestUtils.createPermission(permissionsClient, orgId, ORGANIZATIONS_RESOURCE_TYPE, Set.of(VIEW), policy);
    }

    private void exportAndReimportRealm() {
        String realmName = realm.getName();
        RealmRepresentation exported = runOnServer.fetch(session -> ExportUtils.exportRealm(session, session.realms().getRealmByName(realmName), true, true), RealmRepresentation.class);

        // import the export as if into a fresh server, where the organization does not exist yet
        realm.admin().remove();
        adminClient.realms().create(exported);
    }

    private void assertPermissionImported(ScopePermissionRepresentation permission, String orgId) {
        ClientResource permissionsClient = AdminApiUtil.findClientByClientId(realm.admin(), Constants.ADMIN_PERMISSIONS_CLIENT_ID);
        ScopePermissionRepresentation imported = permissionsClient.authorization().permissions().scope().findByName(permission.getName());
        assertNotNull(imported);
        assertThat(imported.getResourceType(), equalTo(ORGANIZATIONS_RESOURCE_TYPE));
        List<String> resources = permissionsClient.authorization().permissions().scope().findById(imported.getId()).resources()
                .stream().map(ResourceRepresentation::getName).toList();
        assertThat(resources, contains(orgId));
    }

    private String createOrg(String name) {
        OrganizationRepresentation orgRep = new OrganizationRepresentation();
        orgRep.setName(name);
        orgRep.setAlias(name);
        OrganizationDomainRepresentation domain = new OrganizationDomainRepresentation();
        domain.setName(name.toLowerCase() + ".org");
        orgRep.addDomain(domain);

        try (Response response = realm.admin().organizations().create(orgRep)) {
            assertThat(response.getStatus(), equalTo(Response.Status.CREATED.getStatusCode()));
            return ApiUtil.getCreatedId(response);
        }
    }
}
