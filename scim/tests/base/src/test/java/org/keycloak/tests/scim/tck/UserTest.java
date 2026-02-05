package org.keycloak.tests.scim.tck;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributeRequired;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.scim.client.ScimClient;
import org.keycloak.scim.client.ScimClientException;
import org.keycloak.scim.protocol.response.ErrorResponse;
import org.keycloak.scim.resource.common.Email;
import org.keycloak.scim.resource.common.Name;
import org.keycloak.scim.resource.user.EnterpriseUser;
import org.keycloak.scim.resource.user.EnterpriseUser.Manager;
import org.keycloak.scim.resource.user.User;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.scim.client.annotations.InjectScimClient;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.keycloak.scim.model.user.AbstractUserModelSchema.ANNOTATION_SCIM_SCHEMA;
import static org.keycloak.scim.model.user.AbstractUserModelSchema.ANNOTATION_SCIM_SCHEMA_ATTRIBUTE;
import static org.keycloak.scim.resource.Scim.ENTERPRISE_USER_SCHEMA;
import static org.keycloak.scim.resource.Scim.USER_RESOURCE_TYPE;
import static org.keycloak.scim.resource.Scim.getCoreSchema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@KeycloakIntegrationTest(config = ScimServerConfig.class)
public class UserTest extends AbstractScimTest {

    @InjectScimClient(clientId = "noaccess-scim-client", clientSecret = "secret", attachTo = "noaccess-scim-client")
    ScimClient noAccessClient;

    @BeforeEach
    public void onBefore() {
        UPConfig upConfig = realm.admin().users().userProfile().getConfiguration();
        upConfig.getAttribute(UserModel.FIRST_NAME).setRequired(null);
        upConfig.getAttribute(UserModel.LAST_NAME).setRequired(null);
        upConfig.getAttribute(UserModel.EMAIL).setRequired(null);
        Iterator<UPAttribute> iterator = upConfig.getAttributes().iterator();
        while (iterator.hasNext()) {
            UPAttribute attribute = iterator.next();
            if (Set.of(UserModel.USERNAME, UserModel.FIRST_NAME, UserModel.LAST_NAME, UserModel.EMAIL).contains(attribute.getName())) {
                continue;
            }
            iterator.remove();
        }
        realm.admin().users().userProfile().update(upConfig);
    }

    @Test
    public void testCreateWithMinimalRepresentation() {
        User expected = new User();
        expected.setUserName(KeycloakModelUtils.generateId());
        User actual = client.users().create(expected);

        actual = client.users().get(actual.getId());
        assertEquals(1, actual.getSchemas().size());
        assertRootAttributes(actual, expected);
    }

    @Test
    public void testCreateWithSingleEmail() {
        User expected = new User();
        expected.setUserName(KeycloakModelUtils.generateId());
        expected.setEmail(expected.getEmail() + "@keycloak.org");
        User actual = client.users().create(expected);

        actual = client.users().get(actual.getId());
        assertEquals(1, actual.getSchemas().size());
        assertRootAttributes(actual, expected);
    }

    @Test
    public void testCreateWithExternalId() {
        UPConfig configuration = realm.admin().users().userProfile().getConfiguration();
        configuration.addOrReplaceAttribute(new UPAttribute("myExternalId", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, "externalId")));
        realm.admin().users().userProfile().update(configuration);

        User expected = new User();
        expected.setUserName(KeycloakModelUtils.generateId());
        expected.setExternalId(KeycloakModelUtils.generateId());
        User actual = client.users().create(expected);

        actual = client.users().get(actual.getId());
        assertRootAttributes(actual, expected);
        assertEquals(expected.getExternalId(), actual.getExternalId());
    }

    @Test
    public void testCreateWithFullNameAttributes() {
        UPConfig configuration = realm.admin().users().userProfile().getConfiguration();
        configuration.addOrReplaceAttribute(new UPAttribute("middleName", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, "name.middleName")));
        configuration.addOrReplaceAttribute(new UPAttribute("honorificPrefix", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, "name.honorificPrefix")));
        configuration.addOrReplaceAttribute(new UPAttribute("honorificSuffix", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, "name.honorificSuffix")));
        realm.admin().users().userProfile().update(configuration);

        User expected = new User();
        expected.setUserName(KeycloakModelUtils.generateId());
        Name name = new Name();
        name.setGivenName("John");
        name.setFamilyName("Doe");
        name.setMiddleName("M");
        name.setHonorificPrefix("Mr.");
        name.setHonorificSuffix("Jr.");
        expected.setName(name);
        User actual = client.users().create(expected);

        actual = client.users().get(actual.getId());
        assertRootAttributes(actual, expected);
        Name actualName = actual.getName();
        assertNotNull(actualName);
        assertEquals(name.getMiddleName(), actualName.getMiddleName());
        assertEquals(name.getHonorificPrefix(), actualName.getHonorificPrefix());
        assertEquals(name.getHonorificSuffix(), actualName.getHonorificSuffix());
        assertEquals("Mr. John M Doe Jr.", actualName.getFormatted());
    }

    @Test
    public void testLocale() {
        RealmRepresentation realm = this.realm.admin().toRepresentation();
        realm.setInternationalizationEnabled(true);
        this.realm.admin().update(realm);

        User expected = new User();
        expected.setUserName(KeycloakModelUtils.generateId());
        expected.setLocale("en");
        User actual = client.users().create(expected);

        actual = client.users().get(actual.getId());
        assertRootAttributes(actual, expected);
        assertNotNull(actual.getLocale());
        assertEquals(expected.getLocale(), actual.getLocale());
    }

    @Test
    public void testLocaleInternationalizationDisabled() {
        User expected = new User();
        expected.setUserName(KeycloakModelUtils.generateId());
        expected.setLocale("en");
        User actual = client.users().create(expected);

        actual = client.users().get(actual.getId());
        assertRootAttributes(actual, expected);
        assertNull(actual.getLocale());
    }

    @Test
    @Disabled
    public void testDisplayName() {
        // TODO: The displayName attribute is currently not supported by the SCIM User resource. We need to decide how to map it to the Keycloak user model and implement support for it.
        //       Accordingly to the specs, the displayName can map to the username, the name of the user, or some other attribute that represents "the primary textual
        //       label by which this User is normally displayed by the service provider when presenting it to end-users".
        //       That means that the value of the displayName can be derived from other attributes, but it can also be a separate attribute that can be set independently of the others.
        //       We probably need to support both scenarios by allowing to configure how the value should be derived by providing "resource type" settings.
    }

    @Test
    public void testCreateEnterpriseUser() {
        UPConfig configuration = realm.admin().users().userProfile().getConfiguration();

        configuration.addOrReplaceAttribute(new UPAttribute("department", Map.of(
                ANNOTATION_SCIM_SCHEMA, ENTERPRISE_USER_SCHEMA,
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, "department")));
        configuration.addOrReplaceAttribute(new UPAttribute("division", Map.of(
                ANNOTATION_SCIM_SCHEMA, ENTERPRISE_USER_SCHEMA,
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, "division")));
        configuration.addOrReplaceAttribute(new UPAttribute("costCenter", Map.of(
                ANNOTATION_SCIM_SCHEMA, ENTERPRISE_USER_SCHEMA,
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, "costCenter")));
        configuration.addOrReplaceAttribute(new UPAttribute("employeeNumber", Map.of(
                ANNOTATION_SCIM_SCHEMA, ENTERPRISE_USER_SCHEMA,
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, "employeeNumber")));
        configuration.addOrReplaceAttribute(new UPAttribute("organization", Map.of(
                ANNOTATION_SCIM_SCHEMA, ENTERPRISE_USER_SCHEMA,
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, "organization")));
        configuration.addOrReplaceAttribute(new UPAttribute("manager", Map.of(
                ANNOTATION_SCIM_SCHEMA, ENTERPRISE_USER_SCHEMA,
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, "manager.value")));
        configuration.addOrReplaceAttribute(new UPAttribute("managerName", Map.of(
                ANNOTATION_SCIM_SCHEMA, ENTERPRISE_USER_SCHEMA,
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, "manager.dispayName")));
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
        assertRootAttributes(actual, expected);
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
        assertRootAttributes(actual, expected);
    }

    @Test
    public void testGetExisting() {
        UserRepresentation existing = UserConfigBuilder.create()
                .username(KeycloakModelUtils.generateId())
                .email(KeycloakModelUtils.generateId() + "@keycloak.org")
                .firstName("f")
                .lastName("l")
                .enabled(true)
                .build();
        try (Response response = realm.admin().users().create(existing)) {
            String id = ApiUtil.getCreatedId(response);
            existing.setId(id);
        }

        User actual = client.users().get(existing.getId());
        assertNotNull(actual);
        assertEquals(existing.getUsername(), actual.getUserName());
        assertEquals(existing.getEmail(), actual.getEmail());
        assertEquals(existing.getFirstName(), actual.getFirstName());
        assertEquals(existing.getLastName(), actual.getLastName());
        assertEquals(existing.isEnabled(), actual.getActive());
    }

    @Test
    public void testValidateUserProfileConfigOnCreate() {
        UPConfig upConfig = realm.admin().users().userProfile().getConfiguration();
        upConfig.getAttribute(UserModel.EMAIL).setRequired(new UPAttributeRequired());
        realm.admin().users().userProfile().update(upConfig);

        User expected = new User();
        expected.setUserName(KeycloakModelUtils.generateId());

        try {
            client.users().create(expected);
            fail("should fail because of required fields");
        } catch (ScimClientException sce) {
            ErrorResponse error = sce.getError();
            assertNotNull(error);
            assertEquals(400, error.getStatusInt());
            assertEquals("Please specify email.", error.getDetail());
        }

        expected.setEmail(expected.getUserName() + "@keycloak.org");
        client.users().create(expected);
    }

    @Test
    public void testUpdate() {
        User expected = client.users().create(createUser());
        expected.setEmail(expected.getEmail().replace("keycloak.org", "updated.org"));
        User actual = client.users().update(expected);
        assertEquals(1, actual.getSchemas().size());
        assertRootAttributes(actual, expected);
    }

    @Test
    public void testValidateUserProfileOnUpdate() {
        User expected = client.users().create(createUser());

        expected.setEmail("invalid");

        try {
            client.users().update(expected);
        } catch (ScimClientException sce) {
            assertNotNull(sce.getError());
            assertEquals(Status.BAD_REQUEST.getStatusCode(), sce.getError().getStatusInt());
            assertEquals("Invalid email address.", sce.getError().getDetail());
        }
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

    @Test
    public void testNoManagePermission() {
        realm.admin().clients().create(ClientConfigBuilder
                .create()
                .clientId("noaccess-scim-client")
                .secret("secret")
                .serviceAccountsEnabled(true)
                .enabled(true)
                .build()).close();

        try {
            noAccessClient.users().create(createUser());
        } catch (ScimClientException sce) {
            ErrorResponse error = sce.getError();
            assertNotNull(error);
            assertEquals(Status.FORBIDDEN.getStatusCode(), error.getStatusInt());
        }
    }

    private void assertRootAttributes(User actual, User expected) {
        assertNotNull(actual);
        assertTrue(actual.hasSchema(getCoreSchema(expected.getClass())));
        assertNotNull(actual.getMeta());
        assertEquals(USER_RESOURCE_TYPE, actual.getMeta().getResourceType());
        assertNotNull(actual.getMeta().getCreated());
        assertNotNull(actual.getMeta().getLastModified());
        assertNotNull(actual.getMeta().getLocation());
        assertEquals(expected.getUserName(), actual.getUserName());

        if (expected.getActive() != null) {
            assertEquals(expected.getActive(), actual.getActive());
        }

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
