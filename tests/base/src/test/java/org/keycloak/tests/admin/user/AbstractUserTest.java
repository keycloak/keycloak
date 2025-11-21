package org.keycloak.tests.admin.user;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.credential.CredentialModel;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.utils.StripSecretsUtils;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.admin.AdminEventPaths;
import org.keycloak.tests.utils.runonserver.RunHelpers;
import org.keycloak.testsuite.util.userprofile.UserProfileUtil;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AbstractUserTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectAdminEvents
    AdminEvents adminEvents;

    @InjectWebDriver
    WebDriver driver;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectPage
    LoginPage loginPage;

    protected Set<String> managedAttributes = new HashSet<>();

    {
        managedAttributes.add("test");
        managedAttributes.add("attr");
        managedAttributes.add("attr1");
        managedAttributes.add("attr2");
        managedAttributes.add("attr3");
        managedAttributes.add("foo");
        managedAttributes.add("bar");
        managedAttributes.add("phoneNumber");
        managedAttributes.add("usercertificate");
        managedAttributes.add("saml.persistent.name.id.for.foo");
        managedAttributes.add(LDAPConstants.LDAP_ID);
        managedAttributes.add("LDap_Id");
        managedAttributes.add("deniedSomeAdmin");

        for (int i = 1; i < 10; i++) {
            managedAttributes.add("test" + i);
        }
    }

    @BeforeEach
    public void beforeUserTest() throws IOException {

        UserProfileUtil.setUserProfileConfiguration(managedRealm.admin(), null);
        UPConfig upConfig = managedRealm.admin().users().userProfile().getConfiguration();

        for (String name : managedAttributes) {
            upConfig.addOrReplaceAttribute(createAttributeMetadata(name));
        }

        UserProfileUtil.setUserProfileConfiguration(managedRealm.admin(), JsonSerialization.writeValueAsString(upConfig));

        adminEvents.clear();
    }

    @AfterEach
    public void after() {
        managedRealm.admin().identityProviders().findAll()
                .forEach(ip -> managedRealm.admin().identityProviders().get(ip.getAlias()).remove());

        managedRealm.admin().groups().groups()
                .forEach(g -> managedRealm.admin().groups().group(g.getId()).remove());
    }

    protected String createUser() {
        return createUser("user1", "user1@localhost");
    }

    protected String createUser(String username, String email) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setRequiredActions(Collections.emptyList());
        user.setEnabled(true);

        return createUser(user);
    }

    protected String createUser(UserRepresentation userRep) {
        return createUser(userRep, true);
    }

    protected String createUser(UserRepresentation userRep, boolean assertAdminEvent) {
        final String createdId;
        try (Response response = managedRealm.admin().users().create(userRep)) {
            createdId = ApiUtil.getCreatedId(response);
        }
        managedRealm.cleanup().add(r -> r.users().get(createdId).remove());

        StripSecretsUtils.stripSecrets(null, userRep);

        if (assertAdminEvent) {
            AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.userResourcePath(createdId), userRep,
                    ResourceType.USER);
        }

        return createdId;
    }

    protected void updateUser(UserResource user, UserRepresentation userRep) {
        user.update(userRep);
        List<CredentialRepresentation> credentials = userRep.getCredentials();
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.userResourcePath(userRep.getId()), StripSecretsUtils.stripSecrets(null, userRep), ResourceType.USER);
        userRep.setCredentials(credentials);
    }

    protected UPAttribute createAttributeMetadata(String name) {
        UPAttribute attribute = new UPAttribute();
        attribute.setName(name);
        attribute.setMultivalued(true);
        UPAttributePermissions permissions = new UPAttributePermissions();
        permissions.setEdit(Set.of("user", "admin"));
        attribute.setPermissions(permissions);
        this.managedAttributes.add(name);
        return attribute;
    }

    protected CredentialModel fetchCredentials(String username) {
        return runOnServer.fetch(RunHelpers.fetchCredentials(username));
    }

    protected List<String> createUsers() {
        List<String> ids = new ArrayList<>();

        for (int i = 1; i < 10; i++) {
            UserRepresentation user = new UserRepresentation();
            user.setUsername("username" + i);
            user.setEmail("user" + i + "@localhost");
            user.setFirstName("First" + i);
            user.setLastName("Last" + i);

            addAttribute(user, "test", Collections.singletonList("test" + i));
            addAttribute(user, "test" + i, Collections.singletonList("test" + i));
            addAttribute(user, "attr", Arrays.asList("common", "common2"));

            ids.add(createUser(user));
        }

        return ids;
    }

    private void addAttribute(UserRepresentation user, String name, List<String> values) {
        Map<String, List<String>> attributes = Optional.ofNullable(user.getAttributes()).orElse(new HashMap<>());

        attributes.put(name, values);
        managedAttributes.add(name);

        user.setAttributes(attributes);
    }

    protected void deleteUser(String id) {
        try (Response response = managedRealm.admin().users().delete(id)) {
            assertEquals(204, response.getStatus());
        }
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.userResourcePath(id), ResourceType.USER);
    }

    protected void addFederatedIdentity(String keycloakUserId, String identityProviderAlias1,
                                      FederatedIdentityRepresentation link) {
        Response response1 = managedRealm.admin().users().get(keycloakUserId).addFederatedIdentity(identityProviderAlias1, link);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE,
                AdminEventPaths.userFederatedIdentityLink(keycloakUserId, identityProviderAlias1), link,
                ResourceType.USER);
        assertEquals(204, response1.getStatus());
    }

    protected void addSampleIdentityProvider() {
        addSampleIdentityProvider("social-provider-id", 0);
    }

    protected void addSampleIdentityProvider(final String alias, final int expectedInitialIdpCount) {
        List<IdentityProviderRepresentation> providers = managedRealm.admin().identityProviders().findAll();
        Assertions.assertEquals(expectedInitialIdpCount, providers.size());

        IdentityProviderRepresentation rep = new IdentityProviderRepresentation();
        rep.setAlias(alias);
        rep.setProviderId("oidc");

        managedRealm.admin().identityProviders().create(rep);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.identityProviderPath(rep.getAlias()), rep, ResourceType.IDENTITY_PROVIDER);
    }

    protected String mapToSearchQuery(Map<String, String> search) {
        return search.entrySet()
                .stream()
                .map(e -> String.format("%s:%s", e.getKey(), e.getValue()))
                .collect(Collectors.joining(" "));
    }

    protected void switchEditUsernameAllowedOn(boolean enable) {
        RealmRepresentation rep = managedRealm.admin().toRepresentation();
        managedRealm.cleanup().add(r -> r.update(rep));
        rep.setEditUsernameAllowed(enable);
        managedRealm.admin().update(rep);
        AdminEventAssertion.assertSuccess(adminEvents.poll()).operationType(OperationType.UPDATE).representation(rep).resourceType(ResourceType.REALM);
    }

    protected void switchRegistrationEmailAsUsername(boolean enable) {
        RealmRepresentation rep = managedRealm.admin().toRepresentation();
        managedRealm.cleanup().add(r -> r.update(rep));
        rep.setRegistrationEmailAsUsername(enable);
        managedRealm.admin().update(rep);
        AdminEventAssertion.assertSuccess(adminEvents.poll()).operationType(OperationType.UPDATE).representation(rep).resourceType(ResourceType.REALM);
    }

    protected static <T> T loadJson(InputStream is, Class<T> type) {
        try {
            return JsonSerialization.readValue(is, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse json", e);
        }
    }
}
