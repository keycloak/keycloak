package org.keycloak.tests.admin.identityprovider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperTypeRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.providers.broker.oidc.OverwrittenMappersTestIdentityProviderFactory;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest(config = IdentityProviderMapperTest.IdentityProviderMapperServerConf.class)
public class IdentityProviderMapperTest extends AbstractIdentityProviderTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @Test
    public void testMapperTypes() {

        IdentityProviderResource provider;
        Map<String, IdentityProviderMapperTypeRepresentation> mapperTypes;

        create(createRep("google", "google"));
        provider = managedRealm.admin().identityProviders().get("google");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "google-user-attribute-mapper", "oidc-username-idp-mapper");

        create(createRep("facebook", "facebook"));
        provider = managedRealm.admin().identityProviders().get("facebook");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "facebook-user-attribute-mapper", "oidc-username-idp-mapper");

        create(createRep("github", "github"));
        provider = managedRealm.admin().identityProviders().get("github");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "github-user-attribute-mapper", "oidc-username-idp-mapper");

        create(createRep("twitter", "twitter"));
        provider = managedRealm.admin().identityProviders().get("twitter");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "oidc-username-idp-mapper");

        /*
        // disabled to prevent 429 rate limiting on GitHub actions for LinkedIn's
        // https://www.linkedin.com/oauth/.well-known/openid-configuration discovery URL
        create(createRep("linkedin-openid-connect", "linkedin-openid-connect"));
        provider = managedRealm.admin().identityProviders().get("linkedin-openid-connect");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "linkedin-user-attribute-mapper", "oidc-username-idp-mapper");
        */

        create(createRep("microsoft", "microsoft"));
        provider = managedRealm.admin().identityProviders().get("microsoft");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "microsoft-user-attribute-mapper", "oidc-username-idp-mapper");

        create(createRep("stackoverflow", "stackoverflow"));
        provider = managedRealm.admin().identityProviders().get("stackoverflow");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "stackoverflow-user-attribute-mapper", "oidc-username-idp-mapper");

        create(createRep("keycloak-oidc", "keycloak-oidc"));
        provider = managedRealm.admin().identityProviders().get("keycloak-oidc");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "keycloak-oidc-role-to-role-idp-mapper", "oidc-user-attribute-idp-mapper", "oidc-role-idp-mapper", "oidc-username-idp-mapper", "oidc-advanced-group-idp-mapper", "oidc-advanced-role-idp-mapper", "oidc-user-session-note-idp-mapper");

        create(createRep("oidc", "oidc"));
        provider = managedRealm.admin().identityProviders().get("oidc");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "oidc-user-attribute-idp-mapper", "oidc-role-idp-mapper", "oidc-username-idp-mapper", "oidc-advanced-group-idp-mapper", "oidc-advanced-role-idp-mapper", "oidc-user-session-note-idp-mapper");

        create(createRep("saml", "saml"));
        provider = managedRealm.admin().identityProviders().get("saml");
        mapperTypes = provider.getMapperTypes();
        assertMapperTypes(mapperTypes, "saml-user-attribute-idp-mapper", "saml-role-idp-mapper", "saml-username-idp-mapper", "saml-advanced-role-idp-mapper", "saml-advanced-group-idp-mapper", "saml-xpath-attribute-idp-mapper");
    }

    @Test
    public void mapperTypesCanBeOverwritten() {
        String kcOidcProviderId = "keycloak-oidc";
        create(createRep(kcOidcProviderId, kcOidcProviderId));

        String testProviderId = OverwrittenMappersTestIdentityProviderFactory.PROVIDER_ID;
        create(createRep(testProviderId, testProviderId));

        /*
         * in the test provider, we have overwritten the mapper types to be the same as supported by "keycloak-oidc", so
         * the "keycloak-oidc" mappers are the expected mappers for the test provider
         */
        IdentityProviderResource kcOidcProvider = managedRealm.admin().identityProviders().get(kcOidcProviderId);
        Set<String> expectedMapperTypes = kcOidcProvider.getMapperTypes().keySet();

        IdentityProviderResource testProvider = managedRealm.admin().identityProviders().get(testProviderId);
        Set<String> actualMapperTypes = testProvider.getMapperTypes().keySet();

        assertThat(actualMapperTypes, equalTo(expectedMapperTypes));
    }

    @Test
    public void testMappers() {
        create(createRep("google", "google"));

        IdentityProviderResource provider = managedRealm.admin().identityProviders().get("google");

        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setIdentityProviderAlias("google");
        mapper.setName("my_mapper");
        mapper.setIdentityProviderMapper("oidc-hardcoded-role-idp-mapper");
        Map<String, String> config = new HashMap<>();
        config.put("role", "offline_access");
        config.put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.INHERIT.toString());
        mapper.setConfig(config);

        // createRep and add mapper
        Response response = provider.addMapper(mapper);
        String id = ApiUtil.getCreatedId(response);
        Assertions.assertNotNull(id);
        response.close();
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.identityProviderMapperPath("google", id), mapper, ResourceType.IDENTITY_PROVIDER_MAPPER);

        // list mappers
        List<IdentityProviderMapperRepresentation> mappers = provider.getMappers();
        Assertions.assertEquals(1, mappers.size(), "mappers count");
        Assertions.assertEquals(id, mappers.get(0).getId(), "newly created mapper id");

        // get mapper
        mapper = provider.getMapperById(id);
        Assertions.assertEquals("INHERIT", mappers.get(0).getConfig().get(IdentityProviderMapperModel.SYNC_MODE));
        Assertions.assertNotNull(mapper, "mapperById not null");
        Assertions.assertEquals(id, mapper.getId(), "mapper id");
        Assertions.assertNotNull(mapper.getConfig(), "mapper.config exists");
        Assertions.assertEquals("offline_access", mapper.getConfig().get("role"), "config retained");

        // add duplicate mapper
        Response error = provider.addMapper(mapper);
        Assertions.assertEquals(400, error.getStatus(), "mapper unique name");
        error.close();

        // update mapper
        mapper.getConfig().put("role", "master-realm.manage-realm");
        provider.update(id, mapper);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.identityProviderMapperPath("google", id), mapper, ResourceType.IDENTITY_PROVIDER_MAPPER);

        mapper = provider.getMapperById(id);
        Assertions.assertNotNull(mapper, "mapperById not null");
        Assertions.assertEquals("master-realm.manage-realm", mapper.getConfig().get("role"), "config changed");

        // delete mapper
        provider.delete(id);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.identityProviderMapperPath("google", id), ResourceType.IDENTITY_PROVIDER_MAPPER);
        try {
            provider.getMapperById(id);
            Assertions.fail("Should fail with NotFoundException");
        } catch (NotFoundException e) {
            // Expected
        }
    }

    // KEYCLOAK-4962
    @Test
    public void testUpdateProtocolMappers() {
        create(createRep("google2", "google"));

        IdentityProviderResource provider = managedRealm.admin().identityProviders().get("google2");

        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setIdentityProviderAlias("google2");
        mapper.setName("my_mapper");
        mapper.setIdentityProviderMapper("oidc-hardcoded-role-idp-mapper");
        Map<String, String> config = new HashMap<>();
        config.put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.INHERIT.toString());
        config.put("role", "");
        mapper.setConfig(config);

        Response response = provider.addMapper(mapper);
        String mapperId = ApiUtil.getCreatedId(response);


        List<IdentityProviderMapperRepresentation> mappers = provider.getMappers();
        assertEquals(1, mappers.size());
        assertEquals(1, mappers.get(0).getConfig().size());

        mapper = provider.getMapperById(mapperId);
        mapper.getConfig().put("role", "offline_access");

        provider.update(mapperId, mapper);

        mappers = provider.getMappers();
        assertEquals("INHERIT", mappers.get(0).getConfig().get(IdentityProviderMapperModel.SYNC_MODE));
        assertEquals(1, mappers.size());
        assertEquals(2, mappers.get(0).getConfig().size());
        assertEquals("offline_access", mappers.get(0).getConfig().get("role"));
    }

    // KEYCLOAK-7872
    @Test
    public void testDeleteProtocolMappersAfterDeleteIdentityProvider() {
        create(createRep("google3", "google"));

        IdentityProviderResource provider = managedRealm.admin().identityProviders().get("google3");

        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setIdentityProviderAlias("google3");
        mapper.setName("my_mapper");
        mapper.setIdentityProviderMapper("oidc-hardcoded-role-idp-mapper");
        Map<String, String> config = new HashMap<>();
        config.put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.INHERIT.toString());
        config.put("role", "offline_access");
        mapper.setConfig(config);

        provider.addMapper(mapper);

        List<IdentityProviderMapperRepresentation> mappers = provider.getMappers();
        assertThat(mappers, hasSize(1));

        adminEvents.clear();

        provider.remove();
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.identityProviderPath("google3"), ResourceType.IDENTITY_PROVIDER);

        create(createRep("google3", "google"));

        IdentityProviderResource newProvider = managedRealm.admin().identityProviders().get("google3");

        assertThat(newProvider.getMappers(), empty());
    }

    // Alias subversion: addMapper must reject a representation whose alias points at a different IDP than the path.
    @Test
    public void testAddMapperRejectsDifferentIdentityProviderAlias() {
        create(createRep("idp-a", "google"));
        create(createRep("idp-b", "google"));

        IdentityProviderResource providerA = managedRealm.admin().identityProviders().get("idp-a");
        IdentityProviderResource providerB = managedRealm.admin().identityProviders().get("idp-b");

        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        // Attempt to subvert the alias by pointing the representation at a different IDP than the path.
        mapper.setIdentityProviderAlias("idp-b");
        mapper.setName("subversion_mapper");
        mapper.setIdentityProviderMapper("oidc-hardcoded-role-idp-mapper");
        Map<String, String> config = new HashMap<>();
        config.put("role", "offline_access");
        config.put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.INHERIT.toString());
        mapper.setConfig(config);

        try (Response response = providerA.addMapper(mapper)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        // Neither identity provider must have received the mapper.
        assertThat(providerA.getMappers(), empty());
        assertThat(providerB.getMappers(), empty());
    }

    // Alias subversion: update must reject a representation whose id/alias retargets a different mapper or IDP than the path.
    @Test
    public void testUpdateMapperRejectsRetargetingViaRepresentation() {
        create(createRep("idp-a", "google"));
        create(createRep("idp-b", "google"));

        IdentityProviderResource providerA = managedRealm.admin().identityProviders().get("idp-a");
        IdentityProviderResource providerB = managedRealm.admin().identityProviders().get("idp-b");

        String idA = addHardcodedRoleMapper(providerA, "idp-a", "mapper_a", "offline_access");
        String idB = addHardcodedRoleMapper(providerB, "idp-b", "mapper_b", "offline_access");

        // Update idp-a's mapper through its own path, but carry idp-b's id and alias in the representation.
        IdentityProviderMapperRepresentation subverted = providerA.getMapperById(idA);
        subverted.getConfig().put("role", "master-realm.manage-realm");
        subverted.setId(idB);
        subverted.setIdentityProviderAlias("idp-b");

        Assertions.assertThrows(BadRequestException.class, () -> providerA.update(idA, subverted));

        // Both mappers must be untouched.
        assertEquals("offline_access", providerA.getMapperById(idA).getConfig().get("role"));
        assertEquals("offline_access", providerB.getMapperById(idB).getConfig().get("role"));
    }

    // Alias subversion: update by id must reject a mapper that belongs to a different identity provider than the path.
    @Test
    public void testUpdateMapperByIdRejectsForeignIdentityProvider() {
        create(createRep("idp-a", "google"));
        create(createRep("idp-b", "google"));

        IdentityProviderResource providerA = managedRealm.admin().identityProviders().get("idp-a");
        IdentityProviderResource providerB = managedRealm.admin().identityProviders().get("idp-b");

        String idB = addHardcodedRoleMapper(providerB, "idp-b", "mapper_b", "offline_access");

        IdentityProviderMapperRepresentation rep = providerB.getMapperById(idB);
        rep.getConfig().put("role", "master-realm.manage-realm");

        try {
            // idB belongs to idp-b, but we address it through idp-a's path.
            providerA.update(idB, rep);
            Assertions.fail("Should not allow updating a mapper that belongs to a different identity provider");
        } catch (NotFoundException e) {
            // expected
        }

        // The foreign mapper must remain unchanged.
        assertEquals("offline_access", providerB.getMapperById(idB).getConfig().get("role"));
    }

    private String addHardcodedRoleMapper(IdentityProviderResource provider, String alias, String name, String role) {
        IdentityProviderMapperRepresentation mapper = new IdentityProviderMapperRepresentation();
        mapper.setIdentityProviderAlias(alias);
        mapper.setName(name);
        mapper.setIdentityProviderMapper("oidc-hardcoded-role-idp-mapper");
        Map<String, String> config = new HashMap<>();
        config.put("role", role);
        config.put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.INHERIT.toString());
        mapper.setConfig(config);
        try (Response response = provider.addMapper(mapper)) {
            return ApiUtil.getCreatedId(response);
        }
    }

    private void assertMapperTypes(Map<String, IdentityProviderMapperTypeRepresentation> mapperTypes, String ... mapperIds) {
        Set<String> expected = new HashSet<>();
        expected.add("hardcoded-user-session-attribute-idp-mapper");
        expected.add("oidc-hardcoded-role-idp-mapper");
        expected.add("oidc-hardcoded-group-idp-mapper");
        expected.add("hardcoded-attribute-idp-mapper");
        expected.addAll(Arrays.asList(mapperIds));

        Assertions.assertEquals(expected, mapperTypes.keySet(), "mapperTypes");
    }

    public static class IdentityProviderMapperServerConf implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder builder) {
            return builder.dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
        }
    }
}
