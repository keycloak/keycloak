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
package org.keycloak.broker.provider;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleProvider;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class HardcodedOrganizationRoleMapperTest {

    @Test
    public void grantsConfiguredOrganizationRoleToMembers() {
        TestContext context = new TestContext();
        HardcodedOrganizationRoleMapper mapper = new HardcodedOrganizationRoleMapper();

        mapper.importNewUser(context.session, context.realm, context.user.model, context.mapperModel, context.brokeredIdentityContext);
        mapper.updateBrokeredUser(context.session, context.realm, context.user.model, context.mapperModel, context.brokeredIdentityContext);
        mapper.updateBrokeredUserLegacy(context.session, context.realm, context.user.model, context.mapperModel, context.brokeredIdentityContext);

        assertEquals(2, context.user.grants);
        assertTrue(context.user.roleMappings.contains(context.organizationRole.model));
    }

    @Test
    public void acceptsLinkedOrganizationIdAliasOrBlankConfiguration() {
        TestContext context = new TestContext();

        OrganizationRoleMapperHelper.grantUserRole(context.session, context.realm, context.user.model, context.mapperModel, context.brokeredIdentityContext);
        context.mapperModel.getConfig().put(OrganizationRoleMapperHelper.ORGANIZATION, "acme");
        OrganizationRoleMapperHelper.grantUserRole(context.session, context.realm, context.user.model, context.mapperModel, context.brokeredIdentityContext);
        context.mapperModel.getConfig().put(OrganizationRoleMapperHelper.ORGANIZATION, "");
        OrganizationRoleMapperHelper.grantUserRole(context.session, context.realm, context.user.model, context.mapperModel, context.brokeredIdentityContext);

        assertEquals(3, context.user.grants);
    }

    @Test
    public void failsForInvalidOrganizationRoleMapperConfiguration() {
        TestContext missingOrganization = new TestContext();
        missingOrganization.idp.setOrganizationId(null);
        assertThrows(IdentityBrokerException.class, () -> OrganizationRoleMapperHelper.grantUserRole(missingOrganization.session,
                missingOrganization.realm, missingOrganization.user.model, missingOrganization.mapperModel, missingOrganization.brokeredIdentityContext));

        TestContext wrongOrganization = new TestContext();
        wrongOrganization.mapperModel.getConfig().put(OrganizationRoleMapperHelper.ORGANIZATION, "other");
        assertThrows(IdentityBrokerException.class, () -> OrganizationRoleMapperHelper.grantUserRole(wrongOrganization.session,
                wrongOrganization.realm, wrongOrganization.user.model, wrongOrganization.mapperModel, wrongOrganization.brokeredIdentityContext));

        TestContext missingRoleId = new TestContext();
        missingRoleId.mapperModel.getConfig().remove(OrganizationRoleMapperHelper.ORGANIZATION_ROLE);
        assertThrows(IdentityBrokerException.class, () -> OrganizationRoleMapperHelper.grantUserRole(missingRoleId.session,
                missingRoleId.realm, missingRoleId.user.model, missingRoleId.mapperModel, missingRoleId.brokeredIdentityContext));

        TestContext missingRole = new TestContext();
        missingRole.mapperModel.getConfig().put(OrganizationRoleMapperHelper.ORGANIZATION_ROLE, "missing");
        assertThrows(IdentityBrokerException.class, () -> OrganizationRoleMapperHelper.grantUserRole(missingRole.session,
                missingRole.realm, missingRole.user.model, missingRole.mapperModel, missingRole.brokeredIdentityContext));

        TestContext foreignRole = new TestContext();
        foreignRole.mapperModel.getConfig().put(OrganizationRoleMapperHelper.ORGANIZATION_ROLE, foreignRole.otherOrganizationRole.model.getId());
        assertThrows(IdentityBrokerException.class, () -> OrganizationRoleMapperHelper.grantUserRole(foreignRole.session,
                foreignRole.realm, foreignRole.user.model, foreignRole.mapperModel, foreignRole.brokeredIdentityContext));
    }

    @Test
    public void failsForDisabledOrganizationOrNonMemberUser() {
        TestContext disabledOrganization = new TestContext();
        disabledOrganization.organizationEnabled = false;
        assertThrows(IdentityBrokerException.class, () -> OrganizationRoleMapperHelper.grantUserRole(disabledOrganization.session,
                disabledOrganization.realm, disabledOrganization.user.model, disabledOrganization.mapperModel, disabledOrganization.brokeredIdentityContext));

        TestContext nonMember = new TestContext();
        nonMember.member = false;
        assertThrows(IdentityBrokerException.class, () -> OrganizationRoleMapperHelper.grantUserRole(nonMember.session,
                nonMember.realm, nonMember.user.model, nonMember.mapperModel, nonMember.brokeredIdentityContext));
    }

    @Test
    public void exposesProviderMetadata() {
        HardcodedOrganizationRoleMapper mapper = new HardcodedOrganizationRoleMapper();

        assertEquals(HardcodedOrganizationRoleMapper.PROVIDER_ID, mapper.getId());
        assertEquals("Role Importer", mapper.getDisplayCategory());
        assertEquals("Hardcoded Organization Role", mapper.getDisplayType());
        assertEquals("When user is imported from provider, hardcode an organization role mapping for it.", mapper.getHelpText());
        assertArrayEquals(HardcodedOrganizationRoleMapper.COMPATIBLE_PROVIDERS, mapper.getCompatibleProviders());
        assertTrue(mapper.supportsSyncMode(IdentityProviderSyncMode.IMPORT));
        assertFalse(mapper.getConfigProperties().isEmpty());
    }

    private static class TestContext {
        private boolean organizationEnabled = true;
        private boolean member = true;
        private final Map<String, TestRole> roles = new LinkedHashMap<>();
        private final IdentityProviderModel idp = identityProvider();
        private final RealmModel realm = proxy(RealmModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getName" -> "realm";
            default -> defaultValue(method.getReturnType());
        });
        private final OrganizationModel organization = organization("org", "acme");
        private final OrganizationModel otherOrganization = organization("other-org", "other");
        private final TestRole organizationRole = addOrganizationRole("role", "member", organization);
        private final TestRole otherOrganizationRole = addOrganizationRole("other-role", "other", otherOrganization);
        private final TestUser user = new TestUser();
        private final RoleProvider roleProvider = proxy(RoleProvider.class, (proxy, method, args) -> switch (method.getName()) {
            case "getRoleById" -> {
                TestRole role = roles.get(args[1]);
                yield role != null && role.container == args[0] ? role.model : null;
            }
            default -> defaultValue(method.getReturnType());
        });
        private final OrganizationProvider organizationProvider = proxy(OrganizationProvider.class, (proxy, method, args) -> switch (method.getName()) {
            case "isEnabled" -> true;
            case "getById" -> Objects.equals(args[0], "org") ? organization : null;
            default -> defaultValue(method.getReturnType());
        });
        private final KeycloakSession session = proxy(KeycloakSession.class, (proxy, method, args) -> switch (method.getName()) {
            case "roles" -> roleProvider;
            case "getProvider" -> args[0].equals(OrganizationProvider.class) ? organizationProvider : null;
            default -> defaultValue(method.getReturnType());
        });
        private final IdentityProviderMapperModel mapperModel = mapperModel();
        private final BrokeredIdentityContext brokeredIdentityContext = new BrokeredIdentityContext(idp);

        private IdentityProviderModel identityProvider() {
            IdentityProviderModel model = new IdentityProviderModel();
            model.setAlias("idp");
            model.setEnabled(true);
            model.setOrganizationId("org");
            return model;
        }

        private OrganizationModel organization(String id, String alias) {
            return proxy(OrganizationModel.class, (proxy, method, args) -> switch (method.getName()) {
                case "getId" -> id;
                case "getAlias" -> alias;
                case "isEnabled" -> organizationEnabled;
                case "isMember" -> member && args[0] == user.model;
                case "getIdentityProviders" -> Stream.of(idp);
                default -> defaultValue(method.getReturnType());
            });
        }

        private IdentityProviderMapperModel mapperModel() {
            IdentityProviderMapperModel mapper = new IdentityProviderMapperModel();
            mapper.setName("organization-role-mapper");
            mapper.setConfig(new LinkedHashMap<>(Map.of(
                    OrganizationRoleMapperHelper.ORGANIZATION, "org",
                    OrganizationRoleMapperHelper.ORGANIZATION_ROLE, "role")));
            return mapper;
        }

        private TestRole addOrganizationRole(String id, String name, OrganizationModel organization) {
            TestRole role = new TestRole(id, name, organization);
            roles.put(id, role);
            return role;
        }
    }

    private static class TestUser {
        private final Set<RoleModel> roleMappings = new LinkedHashSet<>();
        private int grants;
        private final UserModel model = proxy(UserModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getId" -> "user";
            case "grantRole" -> {
                grants++;
                roleMappings.add((RoleModel) args[0]);
                yield null;
            }
            case "getRoleMappingsStream" -> roleMappings.stream();
            default -> defaultValue(method.getReturnType());
        });
    }

    private static class TestRole {
        private final RoleContainerModel container;
        private final RoleModel model;

        TestRole(String id, String name, RoleContainerModel container) {
            this.container = container;
            model = proxy(RoleModel.class, (proxy, method, args) -> switch (method.getName()) {
                case "getId" -> id;
                case "getName" -> name;
                case "isOrganizationRole" -> true;
                case "getContainer" -> container;
                case "getContainerId" -> container.getId();
                default -> defaultValue(method.getReturnType());
            });
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> {
            if (method.getDeclaringClass().equals(Object.class)) {
                return switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> type.getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(proxy));
                    default -> null;
                };
            }
            return handler.invoke(proxy, method, args);
        });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(type)) {
            return false;
        }
        if (char.class.equals(type)) {
            return '\0';
        }
        return 0;
    }
}
