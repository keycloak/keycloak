package org.keycloak.tests.scim.tck;

import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.scim.client.ScimClient;
import org.keycloak.scim.protocol.request.PatchRequest;
import org.keycloak.scim.protocol.request.SearchRequest;
import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.user.User;
import org.keycloak.testframework.annotations.InjectOrganization;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedOrganization;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.OrganizationConfig;
import org.keycloak.testframework.realm.OrganizationConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = ScimServerConfig.class)
public class OrganizationTest extends AbstractScimTest {

    @InjectRealm(config = ScimRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectOrganization
    ManagedOrganization orga;

    @Test
    public void testCreateMember() {
        OrganizationRepresentation organization = this.orga.admin().toRepresentation();
        User expected = createMember(organization, "bob");
        assertTrue(this.orga.admin().members().list(-1, -1).stream().anyMatch(u -> u.getId().equals(expected.getId())));
    }

    @Test
    public void testGet() {
        OrganizationRepresentation organization = this.orga.admin().toRepresentation();
        ScimClient client = this.client.organization(organization.getAlias());
        User expected = createMember(organization, KeycloakModelUtils.generateId());
        ListResponse<User> members = client.users().getAll();
        assertEquals(1, members.getTotalResults());
        assertTrue(members.getResources().stream().anyMatch(u -> u.getId().equals(expected.getId())));

        members = client.users().search(SearchRequest.builder().withFilter("userName eq \"" + expected.getUserName() + "\"").build().getFilter());
        assertEquals(1, members.getTotalResults());
        assertTrue(members.getResources().stream().anyMatch(u -> u.getId().equals(expected.getId())));
    }

    @Test
    public void testIgnoreRealmUsers() {
        try (Response response = realm.admin().users().create(UserConfigBuilder.create()
                        .username(KeycloakModelUtils.generateId())
                        .email(KeycloakModelUtils.generateId() + "@example.com")
                        .firstName(KeycloakModelUtils.generateId())
                        .lastName(KeycloakModelUtils.generateId())
                .build())) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
        }
        OrganizationRepresentation organization = this.orga.admin().toRepresentation();
        createMember(organization, KeycloakModelUtils.generateId());
        ScimClient client = this.client.organization(organization.getAlias());
        ListResponse<User> members = client.users().getAll();
        assertEquals(1, members.getTotalResults());
    }

    @Test
    public void testIgnoreMembersFromOtherOrganizations() {
        OrganizationRepresentation orga = this.orga.admin().toRepresentation();
        createMember(orga, KeycloakModelUtils.generateId());
        createMember(orga, KeycloakModelUtils.generateId());
        OrganizationRepresentation orgb = new OrganizationRepresentation();
        orgb.setAlias("orgb");
        orgb.setName("orgb");
        orgb.addDomain(new OrganizationDomainRepresentation("orgb.org"));
        realm.admin().organizations().create(orgb).close();
        createMember(orgb, KeycloakModelUtils.generateId());
        ScimClient client = this.client.organization(orga.getAlias());
        ListResponse<User> members = client.users().getAll();
        assertEquals(2, members.getTotalResults());
    }

    @Test
    public void testDeleteMember() {
        OrganizationRepresentation organization = this.orga.admin().toRepresentation();
        User expected = createMember(organization, "bob");
        assertTrue(this.orga.admin().members().list(-1, -1).stream().anyMatch(u -> u.getId().equals(expected.getId())));
        client.organization(organization.getAlias()).users().delete(expected.getId());
        assertFalse(this.orga.admin().members().list(-1, -1).stream().anyMatch(u -> u.getId().equals(expected.getId())));
    }

    @Test
    public void testUpdateMember() {
        OrganizationRepresentation organization = this.orga.admin().toRepresentation();
        User expected = createMember(organization, "bob");
        assertTrue(this.orga.admin().members().list(-1, -1).stream().anyMatch(u -> u.getId().equals(expected.getId())));
        client.organization(organization.getAlias()).users().patch(expected.getId(), PatchRequest.create()
                .add("name.givenName", "Bobby")
                .build());
        List<MemberRepresentation> members = orga.admin().members().search("bob", true, -1, -1);
        assertEquals(1, members.size());
        MemberRepresentation member = members.get(0);
        assertEquals("Bobby", member.getFirstName());
    }

    private User createMember(OrganizationRepresentation organization, String username) {
        User user = new User();
        user.setUserName(username);
        user.setEmail(username + "@" + organization.getAlias() + ".org");
        user.setFirstName(KeycloakModelUtils.generateId());
        user.setLastName(user.getFirstName());
        return client.organization(organization.getAlias()).users().create(user);
    }

    public static class OrganizationBConfig implements OrganizationConfig {

        @Override
        public OrganizationConfigBuilder configure(OrganizationConfigBuilder builder) {
            return builder.alias("orgb");
        }
    }
}
