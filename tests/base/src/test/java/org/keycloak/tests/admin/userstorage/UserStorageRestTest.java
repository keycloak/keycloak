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

package org.keycloak.tests.admin.userstorage;

import java.util.List;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ComponentResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.LDAPConstants;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ComponentTypeRepresentation;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.CommonLDAPGroupMapperConfig;
import org.keycloak.storage.ldap.mappers.membership.group.GroupLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.membership.group.GroupMapperConfig;
import org.keycloak.storage.ldap.mappers.membership.role.RoleLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.membership.role.RoleMapperConfig;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.utils.Assert;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@KeycloakIntegrationTest
public class UserStorageRestTest extends AbstractUserStorageRestTest {

    @Test
    public void testValidateAndCreateLdapProviderCustomSearchFilter() {
        // Invalid filter

        ComponentRepresentation ldapRep = createBasicLDAPProviderRep();
        ldapRep.getConfig().putSingle(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "dc=something");

        Response resp = managedRealm.admin().components().add(ldapRep);
        Assertions.assertEquals(400, resp.getStatus());
        resp.close();

        // Invalid filter
        ldapRep.getConfig().putSingle(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(dc=something");
        resp = managedRealm.admin().components().add(ldapRep);
        Assertions.assertEquals(400, resp.getStatus());
        resp.close();

        // Invalid filter
        ldapRep.getConfig().putSingle(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "dc=something)");
        resp = managedRealm.admin().components().add(ldapRep);
        Assertions.assertEquals(400, resp.getStatus());
        resp.close();

        // Assert nothing created so far
        Assertions.assertTrue(managedRealm.admin().components().query(managedRealm.getId(), UserStorageProvider.class.getName()).isEmpty());
        Assertions.assertNull(adminEvents.poll());


        // Valid filter. Creation success
        ldapRep.getConfig().putSingle(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(dc=something)");
        String id1 = createComponent(ldapRep);

        // Missing filter is ok too. Creation success
        ComponentRepresentation ldapRep2 = new ComponentRepresentation();
        ldapRep2.setName("ldap3");
        ldapRep2.setProviderId("ldap");
        ldapRep2.setProviderType(UserStorageProvider.class.getName());
        ldapRep2.setConfig(new MultivaluedHashMap<>());
        ldapRep2.getConfig().putSingle("priority", Integer.toString(2));
        ldapRep2.getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.UNSYNCED.name());
        ldapRep2.getConfig().putSingle(LDAPConstants.BIND_DN, "cn=manager");
        ldapRep2.getConfig().putSingle(LDAPConstants.BIND_CREDENTIAL, "password");
        String id2 = createComponent(ldapRep2);

        // Assert both providers created
        List<ComponentRepresentation> providerInstances = managedRealm.admin().components().query(managedRealm.getId(), UserStorageProvider.class.getName());
        Assertions.assertEquals(providerInstances.size(), 2);

        // Cleanup
        removeComponent(id1);
        removeComponent(id2);
    }

    @Test
    public void testValidateAndCreateLdapProviderEditMode() {
        // Test provider without editMode should fail
        ComponentRepresentation ldapRep = createBasicLDAPProviderRep();
        ldapRep.getConfig().remove(LDAPConstants.EDIT_MODE);

        Response resp = managedRealm.admin().components().add(ldapRep);
        Assertions.assertEquals(400, resp.getStatus());
        resp.close();

        // Test provider with READ_ONLY edit mode and validatePasswordPolicy will fail
        ldapRep = createBasicLDAPProviderRep();
        ldapRep.getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.READ_ONLY.name());
        ldapRep.getConfig().putSingle(LDAPConstants.VALIDATE_PASSWORD_POLICY, "true");
        resp = managedRealm.admin().components().add(ldapRep);
        Assertions.assertEquals(400, resp.getStatus());
        resp.close();

        // Test provider with UNSYNCED edit mode and validatePasswordPolicy will fail
        ldapRep.getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.UNSYNCED.name());
        ldapRep.getConfig().putSingle(LDAPConstants.VALIDATE_PASSWORD_POLICY, "true");
        resp = managedRealm.admin().components().add(ldapRep);
        Assertions.assertEquals(400, resp.getStatus());
        resp.close();

        // Test provider with WRITABLE edit mode and validatePasswordPolicy will fail
        ldapRep.getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.WRITABLE.name());
        ldapRep.getConfig().putSingle(LDAPConstants.SYNC_REGISTRATIONS, "true");
        String id1 = createComponent(ldapRep);

        // Cleanup
        removeComponent(id1);
    }

    @Test
    public void testUpdateProvider() {
        ComponentRepresentation ldapRep = createBasicLDAPProviderRep();
        ldapRep.getConfig().putSingle(LDAPConstants.BIND_DN, "cn=manager");
        ldapRep.getConfig().putSingle(LDAPConstants.BIND_CREDENTIAL, "password");
        String id = createComponent(ldapRep);

        // Assert update with invalid filter should fail
        ldapRep = managedRealm.admin().components().component(id).toRepresentation();
        ldapRep.getConfig().putSingle(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(dc=something2");
        ldapRep.getConfig().putSingle(LDAPConstants.BIND_DN, "cn=manager-updated");
        try {
            managedRealm.admin().components().component(id).update(ldapRep);
            Assertions.fail("Not expected to successfull update");
        } catch (BadRequestException bre) {
            // Expected
        }

        // Assert nothing was updated
        assertFederationProvider(managedRealm.admin().components().component(id).toRepresentation(), id, "ldap2", "ldap", LDAPConstants.BIND_DN, "cn=manager", LDAPConstants.BIND_CREDENTIAL, "**********");

        // Change filter to be valid
        ldapRep.getConfig().putSingle(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(dc=something2)");
        managedRealm.admin().components().component(id).update(ldapRep);
        adminEvents.clear();

        // Assert updated successfully
        ldapRep = managedRealm.admin().components().component(id).toRepresentation();
        assertFederationProvider(ldapRep, id, "ldap2", "ldap", LDAPConstants.BIND_DN, "cn=manager-updated", LDAPConstants.BIND_CREDENTIAL, "**********",
                LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(dc=something2)");

        // Assert update displayName
        ldapRep.setName("ldap2");
        managedRealm.admin().components().component(id).update(ldapRep);

        assertFederationProvider(managedRealm.admin().components().component(id).toRepresentation(), id, "ldap2", "ldap",LDAPConstants.BIND_DN, "cn=manager-updated", LDAPConstants.BIND_CREDENTIAL, "**********",
                LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(dc=something2)");

        // Cleanup
        removeComponent(id);
    }

    // KEYCLOAK-12934
    @Test
    public void testLDAPMapperProviderConfigurationForVendorOther() {
        ComponentRepresentation ldapRep = createBasicLDAPProviderRep();
        ldapRep.getConfig().putSingle(LDAPConstants.VENDOR, LDAPConstants.VENDOR_OTHER);
        String ldapModelId = createComponent(ldapRep);

        ComponentTypeRepresentation groupLDAPMapperType = findMapperTypeConfiguration(ldapModelId, GroupLDAPStorageMapperFactory.PROVIDER_ID);
        ConfigPropertyRepresentation groupRetrieverConfigProperty = getUserRolesRetrieveStrategyConfigProperty(groupLDAPMapperType, CommonLDAPGroupMapperConfig.USER_ROLES_RETRIEVE_STRATEGY);

        // LOAD_GROUPS_BY_MEMBER_ATTRIBUTE_RECURSIVELY is expected to be present just for the active directory
        List<String> options = groupRetrieverConfigProperty.getOptions();
        Assert.assertNames(options, GroupMapperConfig.LOAD_GROUPS_BY_MEMBER_ATTRIBUTE, GroupMapperConfig.GET_GROUPS_FROM_USER_MEMBEROF_ATTRIBUTE);
        Assertions.assertFalse(groupRetrieverConfigProperty.getHelpText().contains("LOAD_GROUPS_BY_MEMBER_ATTRIBUTE_RECURSIVELY"));

        ComponentTypeRepresentation roleLDAPMapperType = findMapperTypeConfiguration(ldapModelId, RoleLDAPStorageMapperFactory.PROVIDER_ID);
        ConfigPropertyRepresentation roleRetrieverConfigProperty = getUserRolesRetrieveStrategyConfigProperty(roleLDAPMapperType, CommonLDAPGroupMapperConfig.USER_ROLES_RETRIEVE_STRATEGY);

        // LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY is expected to be present just for the active directory
        options = roleRetrieverConfigProperty.getOptions();
        Assert.assertNames(options, RoleMapperConfig.LOAD_ROLES_BY_MEMBER_ATTRIBUTE, RoleMapperConfig.GET_ROLES_FROM_USER_MEMBEROF_ATTRIBUTE);
        Assertions.assertFalse(roleRetrieverConfigProperty.getHelpText().contains("LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY"));

        // Cleanup including mappers
        removeComponent(ldapModelId);
    }

    // KEYCLOAK-12934
    @Test
    public void testLDAPMapperProviderConfigurationForVendorMSAD() {
        ComponentRepresentation ldapRep = createBasicLDAPProviderRep();
        ldapRep.getConfig().putSingle(LDAPConstants.VENDOR, LDAPConstants.VENDOR_ACTIVE_DIRECTORY);
        String ldapModelId = createComponent(ldapRep);

        ComponentTypeRepresentation groupLDAPMapperType = findMapperTypeConfiguration(ldapModelId, GroupLDAPStorageMapperFactory.PROVIDER_ID);
        ConfigPropertyRepresentation groupRetrieverConfigProperty = getUserRolesRetrieveStrategyConfigProperty(groupLDAPMapperType, CommonLDAPGroupMapperConfig.USER_ROLES_RETRIEVE_STRATEGY);

        // LOAD_GROUPS_BY_MEMBER_ATTRIBUTE_RECURSIVELY is expected to be present just for the active directory
        List<String> options = groupRetrieverConfigProperty.getOptions();
        Assert.assertNames(options, GroupMapperConfig.LOAD_GROUPS_BY_MEMBER_ATTRIBUTE, GroupMapperConfig.GET_GROUPS_FROM_USER_MEMBEROF_ATTRIBUTE,
                GroupMapperConfig.LOAD_GROUPS_BY_MEMBER_ATTRIBUTE_RECURSIVELY);
        Assertions.assertTrue(groupRetrieverConfigProperty.getHelpText().contains("LOAD_GROUPS_BY_MEMBER_ATTRIBUTE_RECURSIVELY"));

        ComponentTypeRepresentation roleLDAPMapperType = findMapperTypeConfiguration(ldapModelId, RoleLDAPStorageMapperFactory.PROVIDER_ID);
        ConfigPropertyRepresentation roleRetrieverConfigProperty = getUserRolesRetrieveStrategyConfigProperty(roleLDAPMapperType, CommonLDAPGroupMapperConfig.USER_ROLES_RETRIEVE_STRATEGY);

        // LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY is expected to be present just for the active directory
        options = roleRetrieverConfigProperty.getOptions();
        Assert.assertNames(options, RoleMapperConfig.LOAD_ROLES_BY_MEMBER_ATTRIBUTE, RoleMapperConfig.GET_ROLES_FROM_USER_MEMBEROF_ATTRIBUTE,
                RoleMapperConfig.LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY);
        Assertions.assertTrue(roleRetrieverConfigProperty.getHelpText().contains("LOAD_ROLES_BY_MEMBER_ATTRIBUTE_RECURSIVELY"));

        // Cleanup including mappers
        removeComponent(ldapModelId);
    }

    private void assertFederationProvider(ComponentRepresentation rep, String id, String displayName, String providerId,
                                          String... config) {
        Assertions.assertEquals(id, rep.getId());
        Assertions.assertEquals(displayName, rep.getName());
        Assertions.assertEquals(providerId, rep.getProviderId());

        Assert.assertMultivaluedMap(rep.getConfig(), config);
    }

    private ComponentTypeRepresentation findMapperTypeConfiguration(String ldapModelId, String mapperProviderId) {
        ComponentResource ldapProvider = managedRealm.admin().components().component(ldapModelId);
        List<ComponentTypeRepresentation> componentTypes = ldapProvider.getSubcomponentConfig(LDAPStorageMapper.class.getName());

        return componentTypes.stream()
                .filter(componentType -> mapperProviderId.equals(componentType.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Not able to find mapper with provider id: " + mapperProviderId));
    }

    private  ConfigPropertyRepresentation getUserRolesRetrieveStrategyConfigProperty(ComponentTypeRepresentation componentType, String propertyName) {
        return componentType.getProperties().stream()
                .filter(configPropertyRep -> propertyName.equals(configPropertyRep.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Not able to find config property with name: " + propertyName));
    }
}
