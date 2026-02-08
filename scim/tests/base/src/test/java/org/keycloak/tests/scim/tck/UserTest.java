package org.keycloak.tests.scim.tck;

import java.util.Map;

import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.scim.client.ScimClient;
import org.keycloak.scim.client.ScimClientException;
import org.keycloak.scim.protocol.response.ErrorResponse;
import org.keycloak.scim.resource.common.Email;
import org.keycloak.scim.resource.common.Name;
import org.keycloak.scim.resource.user.EnterpriseUser;
import org.keycloak.scim.resource.user.EnterpriseUser.Manager;
import org.keycloak.scim.resource.user.User;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.scim.client.annotations.InjectScimClient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest
public class UserTest {

    @InjectScimClient
    ScimClient client;

    @InjectRealm
    ManagedRealm realm;

    @Test
    public void testCreate() {
        UPConfig configuration = realm.admin().users().userProfile().getConfiguration();

        configuration.addOrReplaceAttribute(new UPAttribute("test", Map.of("scim.schema", "urn:ietf:params:scim:schemas:core:2.0:User",
                "scim.schema.attribute", "externalId")));
        configuration.addOrReplaceAttribute(new UPAttribute("kcnick", Map.of("scim.schema", "urn:ietf:params:scim:schemas:core:2.0:User",
                "scim.schema.attribute", "nickName")));

        realm.admin().users().userProfile().update(configuration);

        User expected = createUser();
        User actual = client.users().create(expected);
        actual = client.users().get(actual.getId());
        assertEquals(2, actual.getSchemas().size());
        assertAttributes(actual, expected);
        assertEquals(expected.getExternalId(), actual.getExternalId());
    }

    @Test
    public void testCreateEnterpriseUser() {
        UPConfig configuration = realm.admin().users().userProfile().getConfiguration();

        configuration.addOrReplaceAttribute(new UPAttribute("department", Map.of("scim.schema", "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                "scim.schema.attribute", "department")));
        configuration.addOrReplaceAttribute(new UPAttribute("division", Map.of("scim.schema", "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                "scim.schema.attribute", "division")));
        configuration.addOrReplaceAttribute(new UPAttribute("costCenter", Map.of("scim.schema", "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                "scim.schema.attribute", "costCenter")));
        configuration.addOrReplaceAttribute(new UPAttribute("employeeNumber", Map.of("scim.schema", "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                "scim.schema.attribute", "employeeNumber")));
        configuration.addOrReplaceAttribute(new UPAttribute("organization", Map.of("scim.schema", "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                "scim.schema.attribute", "organization")));
        configuration.addOrReplaceAttribute(new UPAttribute("manager", Map.of("scim.schema", "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                "scim.schema.attribute", "manager.value")));
        configuration.addOrReplaceAttribute(new UPAttribute("managerName", Map.of("scim.schema", "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                "scim.schema.attribute", "manager.dispayName")));
        realm.admin().users().userProfile().update(configuration);

        User expected = createUser();
        EnterpriseUser enterpriseUser = new EnterpriseUser();
        enterpriseUser.setCostCenter("c");
        enterpriseUser.setDepartment("dp");
        enterpriseUser.setDivision("dv");
        enterpriseUser.setOrganization("o");
        enterpriseUser.setEmployeeNumber("en");
        Manager manager = new Manager();
        manager.setValue("m");
        manager.setDisplayName("mdn");
        enterpriseUser.setManager(manager);
        expected.setEnterpriseUser(enterpriseUser);
        User actual = client.users().create(expected);
        actual = client.users().get(actual.getId());
        assertEquals(2, actual.getSchemas().size());
        assertAttributes(actual, expected);
        assertNotNull(actual.getEnterpriseUser());
        assertEquals(enterpriseUser.getDepartment(), actual.getEnterpriseUser().getDepartment());
        assertEquals(enterpriseUser.getDivision(), actual.getEnterpriseUser().getDivision());
        assertEquals(enterpriseUser.getCostCenter(), actual.getEnterpriseUser().getCostCenter());
        assertEquals(enterpriseUser.getOrganization(), actual.getEnterpriseUser().getOrganization());
        assertEquals(enterpriseUser.getEmployeeNumber(), actual.getEnterpriseUser().getEmployeeNumber());
    }

    @Test
    public void testGetById() {
        User expected = createUser();
        String id = client.users().create(expected).getId();
        User actual = client.users().get(id);
        assertAttributes(actual, expected);
    }

    @Test
    public void testUpdate() {
        User expected = client.users().create(createUser());
        expected.setActive(false);
        expected.setEmail(expected.getEmail().replace("keycloak.org", "updated.org"));
        User actual = client.users().update(expected);
        assertEquals(1, actual.getSchemas().size());
        assertAttributes(actual, expected);
    }

    @Test
    public void testDelete() {
        User expected = createUser();
        String id = client.users().create(expected).getId();
        User actual = client.users().get(id);
        client.users().delete(actual.getId());
        actual = client.users().get(id);
        assertNull(actual);
    }

    @Test
    public void testError() {
        User user = client.users().create(createUser());
        try {
            client.users().create(user);
            fail("should fail");
        } catch (ScimClientException sce) {
            ErrorResponse error = sce.getError();
            assertNotNull(error);
            assertEquals(400, error.getStatusInt());
            assertNotNull(error.getDetail());
        }
    }

    private void assertAttributes(User actual, User expected) {
        assertNotNull(actual);
        assertTrue(actual.hasSchema(User.SCHEMA));
        assertNotNull(actual.getMeta());
        assertEquals("User", actual.getMeta().getResourceType());
        assertNotNull(actual.getMeta().getCreated());
        assertNotNull(actual.getMeta().getLastModified());
        assertNotNull(actual.getMeta().getLocation());
        assertEquals(expected.getUserName(), actual.getUserName());
        assertEquals(expected.getActive(), actual.getActive());

        if (expected.getEmail() != null) {
            assertNotNull(actual.getEmails());
            assertEquals(expected.getEmails().size(), actual.getEmails().size());

            for (Email email : expected.getEmails()) {
                Email actualEmail = actual.getEmails().stream()
                        .filter((e) -> email.getValue().equals(e.getValue()))
                        .findFirst()
                        .orElse(null);
                assertNotNull(actualEmail);
                assertEquals(email.getType(), actualEmail.getType());
                assertEquals(email.getPrimary(), actualEmail.getPrimary());
            }
        }

//        assertEquals(expected.getExternalId(), actual.getExternalId());

        Name name = expected.getName();

        if (name != null) {
            assertEquals(name.getFamilyName(), actual.getName().getFamilyName());
            assertEquals(name.getGivenName(), actual.getName().getGivenName());
//            TODO: support for middleName, formatted, honorificPrefix, honorificSuffix
//            assertEquals(name.getMiddleName(), actual.getName().getMiddleName());
//            assertEquals(name.getFormatted(), actual.getName().getFormatted());
//            assertEquals(name.getHonorificPrefix(), actual.getName().getHonorificPrefix());
//            assertEquals(name.getHonorificSuffix(), actual.getName().getHonorificSuffix());
        }

//        assertEquals(expected.getNickName(), actual.getNickName());
    }

    private User createUser() {
        User user = new User();

        user.setUserName(KeycloakModelUtils.generateId());
        user.setEmail(user.getUserName() + "@keycloak.org");
        user.setExternalId(KeycloakModelUtils.generateId());
        user.setActive(true);

        Name name = new Name();
        name.setGivenName(user.getUserName() + "_Given");
        name.setMiddleName(user.getUserName() + "_Middle");
        name.setFamilyName(user.getUserName() + "_Family");
        name.setFormatted(name.getGivenName() + " " + name.getMiddleName() + " " + name.getFamilyName());
        name.setHonorificPrefix("Mr.");
        name.setHonorificSuffix("Jr.");
        user.setName(name);

        user.setNickName("mynickname");

        return user;
    }
}
