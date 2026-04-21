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

package org.keycloak.tests.organization.admin;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.OrganizationModel.IdentityProviderRedirectMode;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.CredentialBuilder;
import org.keycloak.testsuite.util.IdentityProviderBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Base class for organization tests in the new test framework.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractOrganizationTest {

    protected String organizationName = "neworg";
    protected String memberEmail = "jdoe@neworg.org";
    protected String memberPassword = "password";

    @InjectRealm(config = OrganizationRealmConfig.class)
    protected ManagedRealm realm;

    protected OrganizationRepresentation createOrganization() {
        return createOrganization(organizationName);
    }

    protected OrganizationRepresentation createOrganization(String name) {
        return createOrganization(name, name + ".org");
    }

    protected OrganizationRepresentation createOrganization(String name, String... orgDomains) {
        return createOrganization(realm.admin(), name, orgDomains);
    }

    protected OrganizationRepresentation createOrganization(RealmResource realmResource, String name, String... orgDomains) {
        return createOrganization(realmResource, name, createOrgBroker(name), orgDomains);
    }

    protected OrganizationRepresentation createOrganization(String name, boolean isBrokerPublic) {
        IdentityProviderRepresentation broker = createOrgBroker(name);
        broker.setHideOnLogin(!isBrokerPublic);
        return createOrganization(realm.admin(), name, broker, name + ".org");
    }

    protected OrganizationRepresentation createOrganization(RealmResource realmResource, String name,
                                                            IdentityProviderRepresentation broker, String... orgDomains) {
        OrganizationRepresentation org = createRepresentation(name, orgDomains);
        String id;

        try (Response response = realmResource.organizations().create(org)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            id = ApiUtil.getCreatedId(response);
        }

        if (orgDomains != null && orgDomains.length > 0) {
            broker.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, orgDomains[0]);
            broker.getConfig().put(IdentityProviderRedirectMode.EMAIL_MATCH.getKey(), Boolean.TRUE.toString());
        }
        realmResource.identityProviders().create(broker).close();

        String brokerAlias = broker.getAlias();
        realm.cleanup().add(r -> {
            try {
                r.identityProviders().get(brokerAlias).remove();
            } catch (NotFoundException ignored) {}
        });

        realmResource.organizations().get(id).identityProviders().addIdentityProvider(broker.getAlias()).close();
        org = realmResource.organizations().get(id).toRepresentation();

        String orgId = id;
        realm.cleanup().add(r -> {
            try {
                r.organizations().get(orgId).delete().close();
            } catch (NotFoundException ignored) {}
        });

        return org;
    }

    protected OrganizationRepresentation createRepresentation(String name, String... orgDomains) {
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setName(name);
        org.setAlias(name);
        org.setDescription(name + " is a test organization!");

        if (orgDomains != null) {
            for (String orgDomain : orgDomains) {
                OrganizationDomainRepresentation domainRep = new OrganizationDomainRepresentation();
                domainRep.setName(orgDomain);
                org.addDomain(domainRep);
            }
        }

        org.setAttributes(Map.of("key", List.of("value1", "value2")));

        return org;
    }

    protected MemberRepresentation addMember(OrganizationResource organization) {
        return addMember(organization, memberEmail);
    }

    protected MemberRepresentation addMember(OrganizationResource organization, String email) {
        return addMember(organization, null, email, null, null, true);
    }

    protected MemberRepresentation addMember(OrganizationResource organization, String email, String firstName, String lastName) {
        return addMember(organization, null, email, firstName, lastName, true);
    }

    protected MemberRepresentation addMember(OrganizationResource organization, String username, String email,
                                             String firstName, String lastName, boolean isSetCredentials) {
        UserRepresentation expected = new UserRepresentation();

        expected.setEmail(email);
        expected.setUsername(username == null ? expected.getEmail() : username);
        expected.setEnabled(true);
        expected.setFirstName(firstName);
        expected.setLastName(lastName);
        try (Response response = realm.admin().users().create(expected)) {
            expected.setId(ApiUtil.getCreatedId(response));
        }

        if (isSetCredentials) {
            realm.admin().users().get(expected.getId()).resetPassword(
                    CredentialBuilder.create().password(memberPassword).build());
        }

        String userId = expected.getId();
        realm.cleanup().add(r -> r.users().get(userId).remove());

        try (Response response = organization.members().addMember(userId)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            MemberRepresentation actual = organization.members().member(userId).toRepresentation();

            assertNotNull(expected);
            assertEquals(userId, actual.getId());
            assertEquals(expected.getUsername(), actual.getUsername());
            assertEquals(expected.getEmail(), actual.getEmail());

            return actual;
        }
    }

    protected UserRepresentation getUserRepresentation(String userEmail) {
        UsersResource users = realm.admin().users();
        List<UserRepresentation> reps = users.searchByEmail(userEmail, true);
        assertFalse(reps.isEmpty());
        assertEquals(1, reps.size());
        return reps.get(0);
    }

    protected GroupRepresentation createGroup(RealmResource realmResource, String name) {
        GroupRepresentation group = new GroupRepresentation();
        group.setName(name);
        try (Response response = realmResource.groups().add(group)) {
            String groupId = ApiUtil.getCreatedId(response);
            group.setId(groupId);
            return group;
        }
    }

    protected void setMapperConfig(String key, String value) {
        ClientScopeRepresentation orgScope = realm.admin().clientScopes().findAll().stream()
                .filter(s -> OIDCLoginProtocolFactory.ORGANIZATION.equals(s.getName()))
                .findAny()
                .orElseThrow();
        var orgScopeResource = realm.admin().clientScopes().get(orgScope.getId());

        ProtocolMapperRepresentation orgMapper = orgScopeResource.getProtocolMappers().getMappers().stream()
                .filter(m -> OIDCLoginProtocolFactory.ORGANIZATION.equals(m.getName()))
                .findAny()
                .orElseThrow();

        Map<String, String> config = orgMapper.getConfig();

        if (value == null) {
            config.remove(key);
        } else {
            config.put(key, value);
        }

        orgScopeResource.getProtocolMappers().update(orgMapper.getId(), orgMapper);
    }

    /**
     * Creates an OIDC identity provider representation for the given organization name.
     */
    protected IdentityProviderRepresentation createOrgBroker(String orgName) {
        return IdentityProviderBuilder.create()
                .providerId(OIDCIdentityProviderFactory.PROVIDER_ID)
                .alias(orgName + "-identity-provider")
                .setAttribute("clientId", "broker-app")
                .setAttribute("clientSecret", "broker-secret")
                .setAttribute(IdentityProviderModel.SYNC_MODE, "IMPORT")
                .hideOnLoginPage()
                .build();
    }

    /**
     * Realm configuration with organizations enabled.
     */
    public static class OrganizationRealmConfig implements RealmConfig {
        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            return realm.organizationsEnabled(true);
        }
    }
}
