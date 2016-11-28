/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.admin;

import org.junit.Ignore;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Ignore
public class UserStorageMapperTest extends AbstractAdminTest {

    private String ldapProviderId;
    private String dummyProviderId;
    /*

    @Before
    public void initFederationProviders() {
        UserFederationProviderRepresentation ldapRep = UserFederationProviderBuilder.create()
                .displayName("ldap-1")
                .providerName("ldap")
                .priority(1)
                .build();
        Response resp = realm.userFederation().create(ldapRep);
        this.ldapProviderId = ApiUtil.getCreatedId(resp);
        resp.close();
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.userFederationResourcePath(this.ldapProviderId), ldapRep, ResourceType.USER_FEDERATION_PROVIDER);

        UserFederationProviderRepresentation dummyRep = UserFederationProviderBuilder.create()
                .displayName("dummy-1")
                .providerName("dummy")
                .priority(2)
                .build();
        resp = realm.userFederation().create(dummyRep);
        this.dummyProviderId = ApiUtil.getCreatedId(resp);
        resp.close();
        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.userFederationResourcePath(this.dummyProviderId), dummyRep, ResourceType.USER_FEDERATION_PROVIDER);
    }

    @After
    public void cleanFederationProviders() {
        realm.userFederation().get(ldapProviderId).remove();
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.userFederationResourcePath(ldapProviderId), ResourceType.USER_FEDERATION_PROVIDER);

        realm.userFederation().get(dummyProviderId).remove();
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.userFederationResourcePath(dummyProviderId), ResourceType.USER_FEDERATION_PROVIDER);
    }


    @Test
    public void testProviderFactories() {
        // Test dummy mapper
        Map<String, UserFederationMapperTypeRepresentation> mapperTypes = realm.userFederation().get(dummyProviderId).getMapperTypes();
        Assert.assertEquals(1, mapperTypes.size());
        Assert.assertEquals("Dummy", mapperTypes.get("dummy-mapper").getName());


        // Test LDAP mappers
        mapperTypes = ldapProviderResource().getMapperTypes();
        Assert.assertTrue(mapperTypes.keySet().containsAll(Arrays.asList("user-attribute-ldap-mapper", "full-name-ldap-mapper", "role-ldap-mapper")));

        UserFederationMapperTypeRepresentation attrMapper = mapperTypes.get("user-attribute-ldap-mapper");
        Assert.assertEquals("User Attribute", attrMapper.getName());
        Assert.assertFalse(attrMapper.getSyncConfig().isFedToKeycloakSyncSupported());
        Assert.assertFalse(attrMapper.getSyncConfig().isKeycloakToFedSyncSupported());
        Set<String> propNames = getConfigPropertyNames(attrMapper);
        Assert.assertTrue(propNames.containsAll(Arrays.asList("user.model.attribute", "ldap.attribute", "read.only")));
        Assert.assertEquals("false", attrMapper.getDefaultConfig().get("always.read.value.from.ldap"));

        UserFederationMapperTypeRepresentation roleMapper = mapperTypes.get("role-ldap-mapper");
        Assert.assertEquals("Role mappings", roleMapper.getName());
        Assert.assertTrue(roleMapper.getSyncConfig().isFedToKeycloakSyncSupported());
        Assert.assertTrue(roleMapper.getSyncConfig().isKeycloakToFedSyncSupported());
        Assert.assertEquals("sync-ldap-roles-to-keycloak", roleMapper.getSyncConfig().getFedToKeycloakSyncMessage());
        Assert.assertEquals("sync-keycloak-roles-to-ldap", roleMapper.getSyncConfig().getKeycloakToFedSyncMessage());
        propNames = getConfigPropertyNames(roleMapper);
        Assert.assertTrue(propNames.containsAll(Arrays.asList("roles.dn", "role.name.ldap.attribute", "role.object.classes")));
        Assert.assertEquals("cn", roleMapper.getDefaultConfig().get("role.name.ldap.attribute"));
    }

    private Set<String> getConfigPropertyNames(UserFederationMapperTypeRepresentation mapper) {
        List<ConfigPropertyRepresentation> cfg = mapper.getProperties();
        Set<String> result = new HashSet<>();
        for (ConfigPropertyRepresentation rep : cfg) {
            result.add(rep.getName());
        }
        return result;

    }


    @Test
    public void testUserAttributeMapperCRUD() {
        // Test create fails with invalid config
        UserFederationMapperRepresentation attrMapper = createMapperRep("email-mapper", UserAttributeLDAPFederationMapperFactory.PROVIDER_ID);
        Response response = ldapProviderResource().addMapper(attrMapper);
        Assert.assertEquals(400, response.getStatus());
        response.close();

        attrMapper.getConfig().put(UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, "email");
        response = ldapProviderResource().addMapper(attrMapper);
        Assert.assertEquals(400, response.getStatus());
        response.close();

        // Test create success when all mandatory attributes available
        attrMapper.getConfig().put(UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, "mail");
        String mapperId = createMapper(ldapProviderId, attrMapper);

        // Test get
        UserFederationMapperRepresentation mapperRep = ldapProviderResource().getMapperById(mapperId);
        assertMapper(mapperRep, mapperId, "email-mapper", UserAttributeLDAPFederationMapperFactory.PROVIDER_ID, UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, "email", UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, "mail");

        // Test update fails with invalid config
        mapperRep.getConfig().put(UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, "mail-updated");
        mapperRep.getConfig().remove(UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE);
        try {
            ldapProviderResource().updateMapper(mapperId, mapperRep);
            Assert.fail("Not expected update to success");
        } catch (BadRequestException bre) {
            // Expected
        }

        // Test not updated
        mapperRep = ldapProviderResource().getMapperById(mapperId);
        assertMapper(mapperRep, mapperId, "email-mapper", UserAttributeLDAPFederationMapperFactory.PROVIDER_ID, UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, "email", UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, "mail");

        // Test update success
        mapperRep.getConfig().put(UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, "email-updated");
        mapperRep.getConfig().put(UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, "mail-updated");
        ldapProviderResource().updateMapper(mapperId, mapperRep);
        assertAdminEvents.assertEvent(realmId, OperationType.UPDATE, AdminEventPaths.userFederationMapperResourcePath(ldapProviderId, mapperId), mapperRep, ResourceType.USER_FEDERATION_MAPPER);

        mapperRep = ldapProviderResource().getMapperById(mapperId);
        assertMapper(mapperRep, mapperId, "email-mapper", UserAttributeLDAPFederationMapperFactory.PROVIDER_ID, UserAttributeLDAPFederationMapper.USER_MODEL_ATTRIBUTE, "email-updated", UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE, "mail-updated");

        // Test removed successfully
        ldapProviderResource().removeMapper(mapperId);
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.userFederationMapperResourcePath(ldapProviderId, mapperId), ResourceType.USER_FEDERATION_MAPPER);

        try {
            ldapProviderResource().getMapperById(mapperId);
            Assert.fail("Not expected find to success as mapper was removed");
        } catch (NotFoundException nfe) {
            // Expected
        }
    }

    private String createMapper(String userFederationProviderId, UserFederationMapperRepresentation mapper) {
        Response response = realm.userFederation().get(userFederationProviderId).addMapper(mapper);
        Assert.assertEquals(201, response.getStatus());
        response.close();
        String mapperId = ApiUtil.getCreatedId(response);

        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.userFederationMapperResourcePath(userFederationProviderId , mapperId), mapper, ResourceType.USER_FEDERATION_MAPPER);

        return mapperId;
    }


    @Test
    public void testRoleMapper() {
        // Create role mapper will fail
        UserFederationMapperRepresentation roleMapper = createMapperRep("role-mapper", RoleLDAPFederationMapperFactory.PROVIDER_ID,
                RoleMapperConfig.ROLES_DN, "ou=roles,dc=keycloak,dc=org",
                RoleMapperConfig.MODE, "READ_ONLY");
        Response response = ldapProviderResource().addMapper(roleMapper);
        Assert.assertEquals(400, response.getStatus());
        response.close();

        // Fix config and create successfully
        roleMapper.getConfig().put(RoleMapperConfig.USE_REALM_ROLES_MAPPING, "true");
        String roleMapperId = createMapper(ldapProviderId, roleMapper);

        // Assert builtin mappers
        List<UserFederationMapperRepresentation> mappers = ldapProviderResource().getMappers();
        Assert.assertNotNull(findMapperByName(mappers, "email"));
        Assert.assertNotNull(findMapperByName(mappers, "first name"));
        Assert.assertNull(findMapperByName(mappers, "non-existent"));

        roleMapper = findMapperByName(mappers, "role-mapper");
        assertMapper(roleMapper, roleMapperId, "role-mapper", RoleLDAPFederationMapperFactory.PROVIDER_ID,
                RoleMapperConfig.ROLES_DN, "ou=roles,dc=keycloak,dc=org",
                RoleMapperConfig.MODE, "READ_ONLY",
                RoleMapperConfig.USE_REALM_ROLES_MAPPING, "true");


        // Remove role mapper and assert not found anymore
        ldapProviderResource().removeMapper(roleMapperId);
        assertAdminEvents.assertEvent(realmId, OperationType.DELETE, AdminEventPaths.userFederationMapperResourcePath(ldapProviderId, roleMapperId), ResourceType.USER_FEDERATION_MAPPER);

        mappers = ldapProviderResource().getMappers();
        Assert.assertNull(findMapperByName(mappers, "role-mapper"));
    }


    @Test
    public void testSyncMapper() {
        // Create dummy mapper
        UserFederationMapperRepresentation dummyMapperRep = new UserFederationMapperRepresentation();
        dummyMapperRep.setName("some-dummy");
        dummyMapperRep.setFederationMapperType(DummyUserFederationMapper.PROVIDER_NAME);
        dummyMapperRep.setFederationProviderDisplayName("dummy-1");
        String mapperId = createMapper(dummyProviderId, dummyMapperRep);

        // Try to sync with unknown action - fail
        try {
            ldapProviderResource().syncMapperData(mapperId, "unknown");
            Assert.fail("Not expected to pass");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // Try fed To Keycloak sync
        UserFederationSyncResultRepresentation result = ldapProviderResource().syncMapperData(mapperId, "fedToKeycloak");
        Assert.assertEquals("dummyFedToKeycloakSuccess mapper=some-dummy", result.getStatus());

        Map<String, Object> eventRep = new HashMap<>();
        eventRep.put("action", "fedToKeycloak");
        assertAdminEvents.assertEvent(realmId, OperationType.ACTION, AdminEventPaths.userFederationMapperResourcePath(ldapProviderId, mapperId) + "/sync", eventRep, ResourceType.USER_FEDERATION_PROVIDER);

        // Try keycloak to fed
        result = ldapProviderResource().syncMapperData(mapperId, "keycloakToFed");
        Assert.assertEquals("dummyKeycloakToFedSuccess mapper=some-dummy", result.getStatus());

        eventRep.put("action", "keycloakToFed");
        assertAdminEvents.assertEvent(realmId, OperationType.ACTION, AdminEventPaths.userFederationMapperResourcePath(ldapProviderId, mapperId) + "/sync", ResourceType.USER_FEDERATION_PROVIDER);

    }


    private UserFederationProviderResource ldapProviderResource() {
        return realm.userFederation().get(ldapProviderId);
    }

    private UserFederationMapperRepresentation createMapperRep(String name, String type, String... config) {
        UserFederationMapperRepresentation rep = new UserFederationMapperRepresentation();
        rep.setName(name);
        rep.setFederationMapperType(type);
        rep.setFederationProviderDisplayName("ldap-1");

        Map<String, String> cfg = new HashMap<>();
        for (int i=0 ; i<config.length ; i+=2) {
            cfg.put(config[i], config[i+1]);
        }
        rep.setConfig(cfg);
        return rep;
    }

    private void assertMapper(UserFederationMapperRepresentation rep, String id, String name, String federationMapperType, String... config) {
        Assert.assertEquals(id, rep.getId());
        Assert.assertEquals(name, rep.getName());
        Assert.assertEquals("ldap-1", rep.getFederationProviderDisplayName());
        Assert.assertEquals(federationMapperType, rep.getFederationMapperType());

        Assert.assertMap(rep.getConfig(), config);
    }

    private UserFederationMapperRepresentation findMapperByName(List<UserFederationMapperRepresentation> mappers, String name) {
        for (UserFederationMapperRepresentation rep : mappers) {
            if (rep.getName().equals(name)) {
                return rep;
            }
        }
        return null;
    }
    */
}
