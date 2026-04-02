package org.keycloak.tests.scim.tck;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributeRequired;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.scim.client.ScimClient;
import org.keycloak.scim.client.ScimClientException;
import org.keycloak.scim.protocol.request.PatchRequest;
import org.keycloak.scim.protocol.response.ErrorResponse;
import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.Scim;
import org.keycloak.scim.resource.user.Email;
import org.keycloak.scim.resource.user.EnterpriseUser;
import org.keycloak.scim.resource.user.EnterpriseUser.Manager;
import org.keycloak.scim.resource.user.GroupMembership;
import org.keycloak.scim.resource.user.Name;
import org.keycloak.scim.resource.user.User;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.GroupConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.scim.client.annotations.InjectScimClient;
import org.keycloak.testframework.util.ApiUtil;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.keycloak.scim.model.user.AbstractUserModelSchema.ANNOTATION_SCIM_SCHEMA_ATTRIBUTE;
import static org.keycloak.scim.resource.Scim.ENTERPRISE_USER_SCHEMA;
import static org.keycloak.scim.resource.Scim.USER_RESOURCE_TYPE;
import static org.keycloak.scim.resource.Scim.getCoreSchema;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
        adminEvents.clear();
    }

    @Test
    public void testCreateWithMinimalRepresentation() {
        User expected = new User();
        expected.setUserName(KeycloakModelUtils.generateId());
        User actual = client.users().create(expected);

        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.CREATE)
                .resourceType(ResourceType.USER)
                .representation(Map.of("userName", expected.getUserName()));

        actual = client.users().get(actual.getId());
        assertEquals(1, actual.getSchemas().size());
        assertRootAttributes(actual, expected);
    }

    @Test
    public void testCreateWithSingleEmail() {
        User expected = new User();
        expected.setUserName(KeycloakModelUtils.generateId());
        expected.setEmail(expected.getUserName() + "@keycloak.org");
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
        addEnterpriseUserUserProfileAttributes();

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
        assertEquals(enterpriseUser.getDivision(), actual.getEnterpriseUser().getDivision());
        assertEquals(enterpriseUser.getDepartment(), actual.getEnterpriseUser().getDepartment());
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
        adminEvents.clear();
        expected.setEmail(expected.getEmail().replace("keycloak.org", "updated.org"));
        User actual = client.users().update(expected);

        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.UPDATE)
                .resourceType(ResourceType.USER)
                .representation(Map.of("userName", expected.getUserName()));

        assertEquals(1, actual.getSchemas().size());
        assertRootAttributes(actual, expected);

        User another = client.users().create(createUser());
        assertNotNull(another.getId());

        try {
            client.users().update(another.getId(), expected);
            fail("should fail because of invalid identifier");
        } catch (ScimClientException sce) {
            ErrorResponse error = sce.getError();
            assertNotNull(error);
            assertEquals(400, error.getStatusInt());
            assertEquals("Invalid reference to resource", error.getDetail());
        }

        try {
            client.users().update(null, expected);
            fail("should fail because identifier not provided");
        } catch (ScimClientException sce) {
            ErrorResponse error = sce.getError();
            assertNotNull(error);
            assertEquals(404, error.getStatusInt());
        }
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
        adminEvents.clear();
        User actual = client.users().get(id);
        client.users().delete(actual.getId());

        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.DELETE)
                .resourceType(ResourceType.USER)
                .representation(Map.of("id", id, "userName", expected.getUserName()));

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

    @Test
    public void testPatchAdd() {
        User expected = client.users().create(createUser());
        UPConfig configuration = realm.admin().users().userProfile().getConfiguration();
        configuration.addOrReplaceAttribute(new UPAttribute("middleName", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, "name.middleName")));
        configuration.addOrReplaceAttribute(new UPAttribute("honorificPrefix", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, "name.honorificPrefix")));
        configuration.addOrReplaceAttribute(new UPAttribute("honorificSuffix", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, "name.honorificSuffix")));
        realm.admin().users().userProfile().update(configuration);
        adminEvents.clear();

        // patch multiple attributes in a single request
        client.users().patch(expected.getId(), PatchRequest.create()
                .add("name", "{\"givenName\": \"PatchedGivenName\"}")
                .add("name.middleName", "MiddleName")
                .add("name.honorificPrefix", "HonorificPrefix")
                .add("name.honorificSuffix", "HonorificSuffix")
                .add("active", "false")
                .build());

        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.UPDATE)
                .resourceType(ResourceType.USER)
                .representation(Map.of("userName", expected.getUserName()));
        adminEvents.clear();

        User actual = client.users().get(expected.getId());
        expected.setFirstName("PatchedGivenName");
        expected.getName().setMiddleName("MiddleName");
        expected.getName().setHonorificPrefix("HonorificPrefix");
        expected.getName().setHonorificSuffix("HonorificSuffix");
        expected.setActive(false);
        assertRootAttributes(actual, expected);

        // patch a specific attribute by providing its path and the value as a JSON object
        // this is needed to patch complex attributes like "emails" which is a multi-valued attribute with sub-attributes
        // for now, we're only mapping the "value" from a complex attribute as the value to be patched
        client.users().patch(expected.getId(), PatchRequest.create()
                .add("emails", "{\"value\": \"" + expected.getEmail().replace("keycloak.org", "patched.org") + "\", \"type\": \"work\", \"primary\": true}")
                .build());
        actual = client.users().get(expected.getId());
        expected.setEmail(expected.getEmail().replace("keycloak.org", "patched.org"));
        assertRootAttributes(actual, expected);

        // patch a specific attribute by providing its path and the value as a JSON array
        client.users().patch(expected.getId(), PatchRequest.create()
                .add("emails", "[{\"value\": \"" + expected.getEmail().replace("patched.org", "patched2.org") + "\", \"type\": \"work\", \"primary\": true}]")
                .build());
        actual = client.users().get(expected.getId());
        expected.setEmail(expected.getEmail().replace("patched.org", "patched2.org"));
        assertRootAttributes(actual, expected);

        // patch a complex attribute by providing only the value as a JSON object without a path.
        // in this case, the path is derived from the structure of the JSON object.
        client.users().patch(expected.getId(), PatchRequest.create()
                .add("{\"emails\": [{\"value\": \"" + expected.getEmail().replace("patched2.org", "patched3.org") + "\", \"type\": \"work\", \"primary\": true}]}")
                .add("{\"name\": {\"givenName\": \"PatchedGivenName2\"}}")
                .build());
        actual = client.users().get(expected.getId());
        expected.setEmail(expected.getEmail().replace("patched2.org", "patched3.org"));
        expected.setFirstName("PatchedGivenName2");
        assertRootAttributes(actual, expected);

        // patch multiple attributes by providing only the value as a JSON object without a path.
        client.users().patch(expected.getId(), PatchRequest.create()
                .add("{\"emails\": [{\"value\": \"" + expected.getEmail().replace("patched3.org", "patched4.org") + "\", \"type\": \"work\", \"primary\": true}], \"name\": {\"givenName\": \"PatchedGivenName3\"}, \"active\": false}")
                .build());
        actual = client.users().get(expected.getId());
        expected.setEmail(expected.getEmail().replace("patched3.org", "patched4.org"));
        expected.setFirstName("PatchedGivenName3");
        expected.setActive(false);
        assertRootAttributes(actual, expected);

        // patch a multivalued attribute by using the value subattribute in the path
        expected.setEmail(expected.getEmail().replace("patched4.org", "patched5.org"));
        client.users().patch(expected.getId(), PatchRequest.create()
                .add("emails.value", expected.getEmail())
                .build());
        actual = client.users().get(expected.getId());
        assertRootAttributes(actual, expected);

        // patch a multivalued attribute using a filter in the path, path filtering is not yet supported
        expected.setEmail(expected.getEmail().replace("patched5.org", "patched6.org"));
        client.users().patch(expected.getId(), PatchRequest.create()
                .add("emails[type eq \"work\"].value", expected.getEmail())
                .build());
        actual = client.users().get(expected.getId());
        assertRootAttributes(actual, expected);

        // patch a multivalued attribute using a filter in the path and the primary subattribute, which is not supported yet
        client.users().patch(expected.getId(), PatchRequest.create()
                .add("emails[type eq \"work\"].primary", "false")
                .build());
        actual = client.users().get(expected.getId());
        assertTrue(actual.getEmails().get(0).getPrimary());
        assertRootAttributes(actual, expected);

        // patch a simple attribute by providing only the value as a JSON object without a path.
        client.users().patch(expected.getId(), PatchRequest.create()
                .add("{\"active\": true}")
                .build());
        actual = client.users().get(expected.getId());
        expected.setActive(true);
        assertRootAttributes(actual, expected);

        // patch an attribute from an extension schema
        configuration.addOrReplaceAttribute(new UPAttribute("employeeNumber", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ".employeeNumber")));
        realm.admin().users().userProfile().update(configuration);
        assertNull(actual.getEnterpriseUser());
        client.users().patch(expected.getId(), PatchRequest.create()
                .add(ENTERPRISE_USER_SCHEMA + ":" + "employeeNumber", "1234")
                .build());
        actual = client.users().get(expected.getId());
        assertNotNull(actual.getEnterpriseUser());
        expected.setEnterpriseUser(new EnterpriseUser());
        expected.getEnterpriseUser().setEmployeeNumber("1234");
        assertEquals("1234", actual.getEnterpriseUser().getEmployeeNumber());

        client.users().patch(expected.getId(), PatchRequest.create()
                .add(ENTERPRISE_USER_SCHEMA, "{\"employeeNumber\": \"4321\"}")
                .build());
        actual = client.users().get(expected.getId());
        assertNotNull(actual.getEnterpriseUser());
        expected.setEnterpriseUser(new EnterpriseUser());
        expected.getEnterpriseUser().setEmployeeNumber("4321");
        assertEquals("4321", actual.getEnterpriseUser().getEmployeeNumber());

        client.users().patch(expected.getId(), PatchRequest.create()
                .add("{\"" + ENTERPRISE_USER_SCHEMA + "\":{\"employeeNumber\": \"1234\"}}")
                .build());
        actual = client.users().get(expected.getId());
        assertNotNull(actual.getEnterpriseUser());
        assertEquals("1234", actual.getEnterpriseUser().getEmployeeNumber());

        // patch attributes from the core schema and an extension schema in a single request by providing the values as a JSON object without a path.
        client.users().patch(expected.getId(), PatchRequest.create()
                .add("{\"name.givenName\": \"Amanda\", \"" + ENTERPRISE_USER_SCHEMA + ":employeeNumber\": \"321\"}}")
                .build());
        actual = client.users().get(expected.getId());
        assertNotNull(actual.getEnterpriseUser());
        assertEquals("321", actual.getEnterpriseUser().getEmployeeNumber());
        assertEquals("Amanda", actual.getFirstName());

        configuration.addOrReplaceAttribute(new UPAttribute("managerId", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ".manager.value")));
        realm.admin().users().userProfile().update(configuration);
        // patch a sub attribute of a complex attribute using a direct path
        client.users().patch(expected.getId(), PatchRequest.create()
                .add("{\"name.givenName\": \"Alice\", \"" + ENTERPRISE_USER_SCHEMA + ":manager\": \"321\"}}")
                .build());
        actual = client.users().get(expected.getId());
        assertNotNull(actual.getEnterpriseUser());
        assertNotNull(actual.getEnterpriseUser().getManager());
        assertEquals("321", actual.getEnterpriseUser().getManager().getValue());
        assertEquals("Alice", actual.getFirstName());

        client.users().patch(expected.getId(), PatchRequest.create()
                .add("{\"name.givenName\": \"Amanda\", \"" + ENTERPRISE_USER_SCHEMA + "\": {\"manager\": {\"value\": \"567\"}}}")
                .build());
        actual = client.users().get(expected.getId());
        assertNotNull(actual.getEnterpriseUser());
        assertNotNull(actual.getEnterpriseUser().getManager());
        assertEquals("567", actual.getEnterpriseUser().getManager().getValue());
        assertEquals("Amanda", actual.getFirstName());
    }

    @Test
    public void testPatchReplace() {
        User expected = client.users().create(createUser());
        UPConfig configuration = realm.admin().users().userProfile().getConfiguration();
        configuration.addOrReplaceAttribute(new UPAttribute("middleName", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, "name.middleName")));
        configuration.addOrReplaceAttribute(new UPAttribute("honorificPrefix", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, "name.honorificPrefix")));
        configuration.addOrReplaceAttribute(new UPAttribute("honorificSuffix", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, "name.honorificSuffix")));
        realm.admin().users().userProfile().update(configuration);

        // patch multiple attributes in a single request
        client.users().patch(expected.getId(), PatchRequest.create()
                .replace("name", "{\"givenName\": \"PatchedGivenName\"}")
                .replace("name.middleName", "MiddleName")
                .replace("name.honorificPrefix", "HonorificPrefix")
                .replace("name.honorificSuffix", "HonorificSuffix")
                .replace("active", "false")
                .build());
        User actual = client.users().get(expected.getId());
        expected.setFirstName("PatchedGivenName");
        expected.getName().setMiddleName("MiddleName");
        expected.getName().setHonorificPrefix("HonorificPrefix");
        expected.getName().setHonorificSuffix("HonorificSuffix");
        expected.setActive(false);
        assertRootAttributes(actual, expected);

        // patch a specific attribute by providing its path and the value as a JSON object
        // this is needed to patch complex attributes like "emails" which is a multi-valued attribute with sub-attributes
        // for now, we're only mapping the "value" from a complex attribute as the value to be patched
        client.users().patch(expected.getId(), PatchRequest.create()
                .replace("emails", "{\"value\": \"" + expected.getEmail().replace("keycloak.org", "patched.org") + "\", \"type\": \"work\", \"primary\": true}")
                .build());
        actual = client.users().get(expected.getId());
        expected.setEmail(expected.getEmail().replace("keycloak.org", "patched.org"));
        assertRootAttributes(actual, expected);

        // patch a specific attribute by providing its path and the value as a JSON array
        client.users().patch(expected.getId(), PatchRequest.create()
                .replace("emails", "[{\"value\": \"" + expected.getEmail().replace("patched.org", "patched2.org") + "\", \"type\": \"work\", \"primary\": true}]")
                .build());
        actual = client.users().get(expected.getId());
        expected.setEmail(expected.getEmail().replace("patched.org", "patched2.org"));
        assertRootAttributes(actual, expected);

        // patch a complex attribute by providing only the value as a JSON object without a path.
        // in this case, the path is derived from the structure of the JSON object.
        client.users().patch(expected.getId(), PatchRequest.create()
                .replace("{\"emails\": [{\"value\": \"" + expected.getEmail().replace("patched2.org", "patched3.org") + "\", \"type\": \"work\", \"primary\": true}]}")
                .replace("{\"name\": {\"givenName\": \"PatchedGivenName2\"}}")
                .build());
        actual = client.users().get(expected.getId());
        expected.setEmail(expected.getEmail().replace("patched2.org", "patched3.org"));
        expected.setFirstName("PatchedGivenName2");
        assertRootAttributes(actual, expected);

        // patch a simple attribute by providing only the value as a JSON object without a path.
        client.users().patch(expected.getId(), PatchRequest.create()
                .replace("{\"active\": true}")
                .build());
        actual = client.users().get(expected.getId());
        expected.setActive(true);
        assertRootAttributes(actual, expected);

        // patch multiple attributes by providing only the value as a JSON object without a path.
        client.users().patch(expected.getId(), PatchRequest.create()
                .replace("{\"emails\": [{\"value\": \"" + expected.getEmail().replace("patched3.org", "patched4.org") + "\", \"type\": \"work\", \"primary\": true}], \"name\": {\"givenName\": \"PatchedGivenName3\"}, \"active\": false}")
                .build());
        actual = client.users().get(expected.getId());
        expected.setEmail(expected.getEmail().replace("patched3.org", "patched4.org"));
        expected.setFirstName("PatchedGivenName3");
        expected.setActive(false);
        assertRootAttributes(actual, expected);

        // patch a multivalued attribute using a filter in the path that matches an existing value
        client.users().patch(expected.getId(), PatchRequest.create()
                .replace("emails[value ew \"patched4.org\"].value", expected.getEmail().replace("patched4.org", "filtered.org"))
                .build());
        actual = client.users().get(expected.getId());
        expected.setEmail(expected.getEmail().replace("patched4.org", "filtered.org"));
        assertRootAttributes(actual, expected);

        // patch a multivalued attribute using a filter in the path that does not resolve to any value, no update should be performed
        String expectedEmail = expected.getEmail();
        expected.setEmail(expected.getEmail().replace("patched4.org", "patched5.org"));
        client.users().patch(expected.getId(), PatchRequest.create()
                .replace("emails[type eq \"work\"].primary", expected.getEmail())
                .build());
        actual = client.users().get(expected.getId());
        expected.setEmail(expectedEmail);
        assertRootAttributes(actual, expected);

        // patch an attribute from an extension schema
        configuration.addOrReplaceAttribute(new UPAttribute("employeeNumber", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ".employeeNumber")));
        realm.admin().users().userProfile().update(configuration);
        assertNull(actual.getEnterpriseUser());
        client.users().patch(expected.getId(), PatchRequest.create()
                .replace(ENTERPRISE_USER_SCHEMA + ":" + "employeeNumber", "1234")
                .build());
        actual = client.users().get(expected.getId());
        assertNotNull(actual.getEnterpriseUser());
        expected.setEnterpriseUser(new EnterpriseUser());
        expected.getEnterpriseUser().setEmployeeNumber("1234");
        assertEquals("1234", actual.getEnterpriseUser().getEmployeeNumber());

        client.users().patch(expected.getId(), PatchRequest.create()
                .replace(ENTERPRISE_USER_SCHEMA, "{\"employeeNumber\": \"4321\"}")
                .build());
        actual = client.users().get(expected.getId());
        assertNotNull(actual.getEnterpriseUser());
        expected.setEnterpriseUser(new EnterpriseUser());
        expected.getEnterpriseUser().setEmployeeNumber("4321");
        assertEquals("4321", actual.getEnterpriseUser().getEmployeeNumber());

        client.users().patch(expected.getId(), PatchRequest.create()
                .replace("{\"" + ENTERPRISE_USER_SCHEMA + "\":{\"employeeNumber\": \"1234\"}}")
                .build());
        actual = client.users().get(expected.getId());
        assertNotNull(actual.getEnterpriseUser());
        expected.setEnterpriseUser(new EnterpriseUser());
        expected.getEnterpriseUser().setEmployeeNumber("1234");
        assertEquals("1234", actual.getEnterpriseUser().getEmployeeNumber());
    }

    @Test
    public void testPatchRemove() {
        User expected = client.users().create(createUser());
        UPConfig configuration = realm.admin().users().userProfile().getConfiguration();
        configuration.addOrReplaceAttribute(new UPAttribute("middleName", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, "name.middleName")));
        configuration.addOrReplaceAttribute(new UPAttribute("honorificPrefix", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, "name.honorificPrefix")));
        configuration.addOrReplaceAttribute(new UPAttribute("honorificSuffix", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, "name.honorificSuffix")));
        realm.admin().users().userProfile().update(configuration);

        // patch multiple attributes in a single request
        client.users().patch(expected.getId(), PatchRequest.create()
                .add("name", "{\"givenName\": \"PatchedGivenName\"}")
                .add("name.middleName", "MiddleName")
                .add("name.honorificPrefix", "HonorificPrefix")
                .add("name.honorificSuffix", "HonorificSuffix")
                .build());
        User actual = client.users().get(expected.getId());
        expected.setFirstName("PatchedGivenName");
        expected.getName().setMiddleName("MiddleName");
        expected.getName().setHonorificPrefix("HonorificPrefix");
        expected.getName().setHonorificSuffix("HonorificSuffix");
        expected.setActive(true);
        assertRootAttributes(actual, expected);

        client.users().patch(expected.getId(), PatchRequest.create()
                .remove("name.honorificPrefix")
                .remove("name.honorificSuffix")
                .build());
        actual = client.users().get(expected.getId());
        expected.getName().setHonorificPrefix(null);
        expected.getName().setHonorificSuffix(null);
        assertRootAttributes(actual, expected);

        assertNull(actual.getEnterpriseUser());
        configuration.addOrReplaceAttribute(new UPAttribute("employeeNumber", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ".employeeNumber")));
        configuration.addOrReplaceAttribute(new UPAttribute("costCenter", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ".costCenter")));
        realm.admin().users().userProfile().update(configuration);
        client.users().patch(expected.getId(), PatchRequest.create()
                .add(ENTERPRISE_USER_SCHEMA + ":" + "employeeNumber", "1234")
                .add(ENTERPRISE_USER_SCHEMA + ":" + "costCenter", "5678")
                .build());
        actual = client.users().get(expected.getId());
        assertNotNull(actual.getEnterpriseUser());
        assertEquals("1234", actual.getEnterpriseUser().getEmployeeNumber());
        assertEquals("5678", actual.getEnterpriseUser().getCostCenter());
        client.users().patch(expected.getId(), PatchRequest.create()
                .remove(ENTERPRISE_USER_SCHEMA + ":" + "employeeNumber")
                .build());
        actual = client.users().get(expected.getId());
        assertNotNull(actual.getEnterpriseUser());
        assertNull(actual.getEnterpriseUser().getEmployeeNumber());
        assertEquals("5678", actual.getEnterpriseUser().getCostCenter());
    }

    @Test
    public void testUserMembership() {
        GroupRepresentation groupA = createGroup("Group A");
        GroupRepresentation groupA1 = createSubGroup(groupA, "Group A1");
        GroupRepresentation groupA2 = createSubGroup(groupA, "Group A2");
        GroupRepresentation groupA21 = createSubGroup(groupA2, "Group A21");
        GroupRepresentation groupB = createGroup("Group B");
        GroupRepresentation groupC = createGroup("Group C");
        GroupRepresentation groupC1 = createSubGroup(groupC, "Group C1");

        User user = createUser();

        user.addGroup(groupA.getId());
        user.addGroup(groupA1.getId());
        user.addGroup(groupA2.getId());
        user.addGroup(groupA21.getId());
        user.addGroup(groupB.getId());
        user.addGroup(groupC1.getId());

        User expected = client.users().create(user);
        User actual = client.users().get(expected.getId());

        List<GroupMembership> groups = actual.getGroups();

        assertNotNull(groups);
        assertEquals(7, groups.size());
        assertGroup(groups, groupA, "direct");
        assertGroup(groups, groupC, "indirect");

        client.users().patch(expected.getId(), PatchRequest.create()
                .remove("groups[value eq \"" + groupC1.getId() + "\"]")
                .build());
        actual = client.users().get(expected.getId());
        groups = actual.getGroups();
        assertNotNull(groups);
        assertEquals(5, groups.size());

        client.users().patch(expected.getId(), PatchRequest.create()
                .remove("groups[value eq \"" + groupA1.getId() + "\" or value eq \"" + groupB.getId() + "\"]")
                .build());
        actual = client.users().get(expected.getId());
        groups = actual.getGroups();
        assertNotNull(groups);
        assertEquals(3, groups.size());

        client.users().patch(expected.getId(), PatchRequest.create()
                .add("groups", groupC1.getId())
                .build());
        actual = client.users().get(expected.getId());
        groups = actual.getGroups();
        assertNotNull(groups);
        assertEquals(5, groups.size());

        client.users().patch(expected.getId(), PatchRequest.create()
                .add("groups", groupA1.getId())
                .add("groups", groupB.getId())
                .build());
        actual = client.users().get(expected.getId());
        groups = actual.getGroups();
        assertNotNull(groups);
        assertEquals(7, groups.size());

        expected = actual;
        expected.getGroups().clear();
        expected.addGroup(groupA.getId());
        client.users().update(expected);
        actual = client.users().get(expected.getId());
        groups = actual.getGroups();
        assertNotNull(groups);
        assertEquals(1, groups.size());

        User finalExpected = expected;
        assertNotNull(client.users().search("groups.value eq \"" + groupA.getId() + "\"").getResources().stream()
                .filter(u -> u.getId().equals(finalExpected.getId()))
                .findFirst().orElse(null));
        assertNull(client.users().search("groups.value eq \"" + groupC.getId() + "\"").getResources().stream()
                .filter(u -> u.getId().equals(finalExpected.getId()))
                .findFirst().orElse(null));

        client.users().patch(expected.getId(), PatchRequest.create()
                .add("groups", groupC.getId())
                .build());
        assertNotNull(client.users().search("groups.value eq \"" + groupC.getId() + "\"").getResources().stream()
                .filter(u -> u.getId().equals(finalExpected.getId()))
                .findFirst().orElse(null));
        assertNull(client.users().search("(groups.value eq \"" + groupC.getId() + "\") and (groups.value eq \"" + groupB.getId() + "\")").getResources().stream()
                .filter(u -> u.getId().equals(finalExpected.getId()))
                .findFirst().orElse(null));
        assertNotNull(client.users().search("(groups.value eq \"" + groupC.getId() + "\") or (groups.value eq \"" + groupB.getId() + "\")").getResources().stream()
                .filter(u -> u.getId().equals(finalExpected.getId()))
                .findFirst().orElse(null));

        client.users().patch(expected.getId(), PatchRequest.create()
                .remove("groups[value eq \"" + groupA.getId() + "\"]")
                .remove("groups[value eq \"" + groupC.getId() + "\"]")
                .build());
        User expected1 = client.users().create(createUser());
        client.users().patch(expected1.getId(), PatchRequest.create()
                .add("groups", groupC.getId())
                .add("groups", groupA21.getId())
                .build());
        ListResponse<User> resources = client.users().search("groups.value ne \"" + groupC.getId() + "\"");
        assertEquals(1, resources.getResources().size());
        assertNotNull(resources.getResources().stream()
                .filter(u -> u.getId().equals(expected1.getId()))
                .findFirst().orElse(null));

        client.users().patch(expected.getId(), PatchRequest.create()
                .add("groups", groupC.getId())
                .build());
        resources = client.users().search("groups.value eq \"" + groupC.getId() + "\"");
        assertEquals(2, resources.getResources().size());
        assertNotNull(resources.getResources().stream()
                .filter(u -> u.getId().equals(finalExpected.getId()))
                .findFirst().orElse(null));
        assertNotNull(resources.getResources().stream()
                .filter(u -> u.getId().equals(expected1.getId()))
                .findFirst().orElse(null));
    }

    @Test
    public void testGetWithAttributes() {
        User expected = client.users().create(createUser());

        // Request only userName
        User actual = client.users().get(expected.getId(), List.of("userName"), null);
        assertNotNull(actual);
        assertNotNull(actual.getId());
        assertNotNull(actual.getSchemas());
        assertEquals(expected.getUserName(), actual.getUserName());
        assertNull(actual.getName());
        assertNull(actual.getEmails());
        assertNull(actual.getActive());
        assertNull(actual.getDisplayName());
    }

    @Test
    public void testGetWithParentAttribute() {
        User expected = client.users().create(createUser());

        // Requesting "name" should return all its sub-attributes (givenName, familyName, etc.)
        User actual = client.users().get(expected.getId(), List.of("name"), null);
        assertNotNull(actual);
        assertNotNull(actual.getId());
        assertNotNull(actual.getName());
        assertEquals(expected.getName().getGivenName(), actual.getName().getGivenName());
        assertEquals(expected.getName().getFamilyName(), actual.getName().getFamilyName());
        assertNull(actual.getUserName());
        assertNull(actual.getEmails());
    }

    @Test
    public void testGetWithSubAttribute() {
        User expected = client.users().create(createUser());

        // Request only name.familyName
        User actual = client.users().get(expected.getId(), List.of("name.familyName"), null);
        assertNotNull(actual);
        assertNotNull(actual.getId());
        assertNotNull(actual.getName());
        assertEquals(expected.getName().getFamilyName(), actual.getName().getFamilyName());
        assertNull(actual.getName().getGivenName());
        assertNull(actual.getUserName());
    }

    @Test
    public void testGetWithExcludedAttributes() {
        User expected = client.users().create(createUser());

        // Exclude emails
        User actual = client.users().get(expected.getId(), null, List.of("emails"));
        assertNotNull(actual);
        assertNotNull(actual.getId());
        assertEquals(expected.getUserName(), actual.getUserName());
        assertNull(actual.getEmails());
        assertNotNull(actual.getName());
    }

    @Test
    public void testGetWithExcludedAttributesCannotExcludeId() {
        User expected = client.users().create(createUser());

        // Attempting to exclude id should have no effect (returned: always)
        User actual = client.users().get(expected.getId(), null, List.of("id"));
        assertNotNull(actual);
        assertNotNull(actual.getId());
        assertEquals(expected.getId(), actual.getId());
    }

    @Test
    public void testGetAllWithAttributes() {
        User expected = client.users().create(createUser());

        // Request only userName for list
        ListResponse<User> response = client.users().getAll(List.of("userName"), null);
        assertNotNull(response);
        assertThat(response.getTotalResults(), greaterThanOrEqualTo(1));

        User actual = response.getResources().stream()
                .filter(u -> u.getId().equals(expected.getId()))
                .findFirst().orElse(null);
        assertNotNull(actual);
        assertNotNull(actual.getId());
        assertEquals(expected.getUserName(), actual.getUserName());
        assertNull(actual.getName());
        assertNull(actual.getEmails());
    }

    @Test
    public void testGetAllWithExcludedAttributes() {
        User expected = client.users().create(createUser());

        // Exclude emails from list
        ListResponse<User> response = client.users().getAll(null, List.of("emails"));
        assertNotNull(response);
        assertThat(response.getTotalResults(), greaterThanOrEqualTo(1));

        User actual = response.getResources().stream()
                .filter(u -> u.getId().equals(expected.getId()))
                .findFirst().orElse(null);
        assertNotNull(actual);
        assertEquals(expected.getUserName(), actual.getUserName());
        assertNull(actual.getEmails());
    }

    @Test
    public void testGetWithMultipleAttributes() {
        User expected = client.users().create(createUser());

        // Request userName and emails
        User actual = client.users().get(expected.getId(), List.of("userName", "emails"), null);
        assertNotNull(actual);
        assertNotNull(actual.getId());
        assertEquals(expected.getUserName(), actual.getUserName());
        assertNotNull(actual.getEmails());
        assertNull(actual.getName());
        assertNull(actual.getActive());
    }

    @Test
    public void testGetWithBothAttributesAndExcludedAttributes() {
        User expected = client.users().create(createUser());

        // Include userName and emails, then exclude emails
        // Per issue spec: apply inclusion first, then exclusion
        User actual = client.users().get(expected.getId(), List.of("userName", "emails"), List.of("emails"));
        assertNotNull(actual);
        assertNotNull(actual.getId());
        assertEquals(expected.getUserName(), actual.getUserName());
        assertNull(actual.getEmails());
        assertNull(actual.getName());
    }

    @Test
    public void testGetWithAttributeInBothIncludeAndExclude() {
        User expected = client.users().create(createUser());

        // Same attribute in both lists — inclusion first, then exclusion removes it
        User actual = client.users().get(expected.getId(), List.of("userName"), List.of("userName"));
        assertNotNull(actual);
        assertNotNull(actual.getId());
        assertNull(actual.getUserName());
    }

    @Test
    public void testGetWithNonExistingAttribute() {
        User expected = client.users().create(createUser());

        // Non-existing attribute should be silently ignored (best-effort)
        User actual = client.users().get(expected.getId(), List.of("userName", "nonExistingAttr"), null);
        assertNotNull(actual);
        assertNotNull(actual.getId());
        assertEquals(expected.getUserName(), actual.getUserName());
        assertNull(actual.getName());
    }

    @Test
    public void testGetWithAttributesCaseInsensitive() {
        User expected = client.users().create(createUser());

        // Attribute names are case insensitive per RFC 7644, Section 3.10
        User actual = client.users().get(expected.getId(), List.of("USERNAME"), null);
        assertNotNull(actual);
        assertNotNull(actual.getId());
        assertEquals(expected.getUserName(), actual.getUserName());
        assertNull(actual.getName());
        assertNull(actual.getEmails());
    }

    @Test
    public void testGetWithExcludedSubAttribute() {
        User expected = client.users().create(createUser());

        // Exclude a sub-attribute: name.givenName should be excluded but name.familyName kept
        User actual = client.users().get(expected.getId(), null, List.of("name.givenName"));
        assertNotNull(actual);
        assertNotNull(actual.getName());
        assertNull(actual.getName().getGivenName());
        assertEquals(expected.getName().getFamilyName(), actual.getName().getFamilyName());
        assertEquals(expected.getUserName(), actual.getUserName());
    }

    @Test
    public void testGetWithExtensionUrnAttribute() {
        addEnterpriseUserUserProfileAttributes();
        User expected = createUser();
        EnterpriseUser enterpriseUser = new EnterpriseUser();
        enterpriseUser.setEmployeeNumber("12345");
        enterpriseUser.setDepartment("Engineering");
        expected.setEnterpriseUser(enterpriseUser);
        expected = client.users().create(expected);

        // Requesting the extension URN should return all extension attributes
        User actual = client.users().get(expected.getId(),
                List.of("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"), null);
        assertNotNull(actual);
        assertNotNull(actual.getId());
        assertNotNull(actual.getEnterpriseUser());
        assertEquals("12345", actual.getEnterpriseUser().getEmployeeNumber());
        assertEquals("Engineering", actual.getEnterpriseUser().getDepartment());
        assertNull(actual.getUserName());
        assertNull(actual.getName());
    }

    @Test
    public void testGetWithExcludedExtensionUrnAttribute() {
        addEnterpriseUserUserProfileAttributes();
        User expected = createUser();
        EnterpriseUser enterpriseUser = new EnterpriseUser();
        enterpriseUser.setEmployeeNumber("12345");
        enterpriseUser.setDepartment("Engineering");
        expected.setEnterpriseUser(enterpriseUser);
        expected = client.users().create(expected);

        // Requesting the extension URN should return all extension attributes
        User actual = client.users().get(expected.getId(),
                null, List.of("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"));
        assertNotNull(actual);
        assertNotNull(actual.getId());
        assertNull(actual.getEnterpriseUser());
    }

    private static void assertGroup(List<GroupMembership> groups, GroupRepresentation group, String type) {
        assertTrue(groups.stream().anyMatch(membership -> {
            boolean found = group.getId().equals(membership.getValue()) && group.getName().equals(membership.getDisplay());

            if (found) {
                return type.equals(membership.getType());
            }

            return false;
        }));
    }

    private GroupRepresentation createGroup(String name) {
        GroupRepresentation group = GroupConfigBuilder.create().name(name).build();
        try (Response response = realm.admin().groups().add(group)) {
            group.setId(ApiUtil.getCreatedId(response));
        }
        return group;
    }

    private GroupRepresentation createSubGroup(GroupRepresentation parent, String name) {
        GroupResource parentApi = realm.admin().groups().group(parent.getId());
        GroupRepresentation subGroup = GroupConfigBuilder.create().name(name).build();

        try (Response response = parentApi.subGroup(subGroup)) {
            subGroup.setId(ApiUtil.getCreatedId(response));
        }

        return subGroup;
    }

    @Test
    public void testMetaTimestamps() {
        User user = client.users().create(createUser());
        User afterCreate = client.users().get(user.getId());
        assertNotNull(afterCreate.getMeta().getCreated());
        assertNotNull(afterCreate.getMeta().getLastModified());

        Instant createdTimestamp = Instant.parse(afterCreate.getMeta().getCreated());
        Instant lastModifiedAfterCreate = Instant.parse(afterCreate.getMeta().getLastModified());

        // after create, lastModified should be >= created
        assertThat(lastModifiedAfterCreate, greaterThanOrEqualTo(createdTimestamp));

        // update the user
        afterCreate.setEmail(afterCreate.getUserName() + "@updated.org");
        client.users().update(afterCreate);
        User afterUpdate = client.users().get(afterCreate.getId());

        Instant createdAfterUpdate = Instant.parse(afterUpdate.getMeta().getCreated());
        Instant lastModifiedAfterUpdate = Instant.parse(afterUpdate.getMeta().getLastModified());

        // created should not change after update
        assertEquals(createdTimestamp, createdAfterUpdate);
        // lastModified should be >= created after update
        assertThat(lastModifiedAfterUpdate, greaterThanOrEqualTo(createdAfterUpdate));
        // lastModified should have advanced after update
        assertThat(lastModifiedAfterUpdate, greaterThanOrEqualTo(lastModifiedAfterCreate));
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

        if (expected.getEmails() != null) {
            for (Email email : expected.getEmails()) {
                Email actualEmail = actual.getEmails().stream()
                        .filter((e) -> email.getValue() != null && email.getValue().equals(e.getValue()))
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
            assertEquals(name.getMiddleName(), actual.getName().getMiddleName());
//            assertEquals(name.getFormatted(), actual.getName().getFormatted());
            assertEquals(name.getHonorificPrefix(), actual.getName().getHonorificPrefix());
            assertEquals(name.getHonorificSuffix(), actual.getName().getHonorificSuffix());
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
        name.setFamilyName(user.getUserName() + "_Family");
        user.setName(name);

        user.setNickName("mynickname");

        return user;
    }

    @Test
    public void testCreateWithInvalidAttribute() {
        User user = new User() {
            @Override
            public Set<String> getSchemas() {
                return Set.of(Scim.USER_CORE_SCHEMA);
            }

            @JsonProperty("invalidAttribute")
            public String getInvalidAttribute() {
                return "invalidValue";
            }
        };

        try {
            client.users().create(user);
            fail("should fail because of invalid attribute");
        } catch (ScimClientException sce) {
            ErrorResponse error = sce.getError();
            assertNotNull(error);
            assertEquals(400, error.getStatusInt());
            assertNotNull(error.getDetail());
            assertTrue(error.getDetail().contains("invalidAttribute"));
        }
    }

    @Test
    public void testCreateDuplicate() {
        User user = new User();
        user.setUserName(KeycloakModelUtils.generateId());
        client.users().create(user);

        try {
            client.users().create(user);
            fail("should fail because of duplicate user");
        } catch (ScimClientException sce) {
            ErrorResponse error = sce.getError();
            assertNotNull(error);
            assertEquals(409, error.getStatusInt());
            assertEquals("uniqueness", error.getScimType());
            assertNotNull(error.getDetail());
        }
    }

    private void addEnterpriseUserUserProfileAttributes() {
        UPConfig configuration = realm.admin().users().userProfile().getConfiguration();

        configuration.addOrReplaceAttribute(new UPAttribute("department", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ".department")));
        configuration.addOrReplaceAttribute(new UPAttribute("division", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ".division")));
        configuration.addOrReplaceAttribute(new UPAttribute("costCenter", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ".costCenter")));
        configuration.addOrReplaceAttribute(new UPAttribute("employeeNumber", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ".employeeNumber")));
        configuration.addOrReplaceAttribute(new UPAttribute("organization", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ".organization")));
        configuration.addOrReplaceAttribute(new UPAttribute("manager", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ".manager.value")));
        configuration.addOrReplaceAttribute(new UPAttribute("managerName", Map.of(
                ANNOTATION_SCIM_SCHEMA_ATTRIBUTE, ENTERPRISE_USER_SCHEMA + ".manager.displayName")));
        realm.admin().users().userProfile().update(configuration);
    }
}
