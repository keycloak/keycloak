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
package org.keycloak.organization.protocol.mappers.saml;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.common.Profile;
import org.keycloak.common.profile.ProfileConfigResolver;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.protocol.saml.SamlProtocol;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OrganizationRoleMembershipMapperTest {

    @Before
    public void configureProfile() {
        Profile.reset();
        Profile.configure(new OrganizationFeatureResolver());
    }

    @After
    public void resetProfile() {
        Profile.reset();
    }

    @Test
    public void mapsOrganizationRoleClaimsAsAttributes() {
        TestContext context = new TestContext();
        AttributeStatementType statement = new AttributeStatementType();

        new OrganizationRoleMembershipMapper().transformAttributeStatement(statement, OrganizationRoleMembershipMapper.create(),
                context.session, context.userSession, null);

        Map<String, List<Object>> attributes = attributes(statement);
        assertEquals(List.of("admin", "member"), attributes.get("organization.acme.organization_roles"));
        assertEquals(List.of("realm-admin"), attributes.get("organization.acme.realm_access.roles"));
        assertEquals(List.of("service-admin"), attributes.get("organization.acme.resource_access.service.roles"));
    }

    @Test
    public void omitsAttributesForEmptyRoleClaimGroups() {
        TestContext context = new TestContext();
        context.organizationRole.composites.clear();
        AttributeStatementType statement = new AttributeStatementType();

        new OrganizationRoleMembershipMapper().transformAttributeStatement(statement, OrganizationRoleMembershipMapper.create(),
                context.session, context.userSession, null);

        Map<String, List<Object>> attributes = attributes(statement);
        assertEquals(List.of("admin"), attributes.get("organization.acme.organization_roles"));
        assertFalse(attributes.containsKey("organization.acme.realm_access.roles"));
        assertFalse(attributes.containsKey("organization.acme.resource_access.service.roles"));
    }

    @Test
    public void skipsWhenOrganizationsAreDisabledOrUserHasNoRoleClaims() {
        TestContext disabled = new TestContext();
        disabled.organizationsEnabled = false;
        AttributeStatementType statement = new AttributeStatementType();

        new OrganizationRoleMembershipMapper().transformAttributeStatement(statement, OrganizationRoleMembershipMapper.create(),
                disabled.session, disabled.userSession, null);

        assertTrue(statement.getAttributes().isEmpty());

        TestContext noRoles = new TestContext();
        noRoles.user.roleMappings.clear();
        statement = new AttributeStatementType();
        new OrganizationRoleMembershipMapper().transformAttributeStatement(statement, OrganizationRoleMembershipMapper.create(),
                noRoles.session, noRoles.userSession, null);

        assertTrue(statement.getAttributes().isEmpty());
    }

    @Test
    public void exposesProviderMetadataAndCreateDefaults() {
        OrganizationRoleMembershipMapper mapper = new OrganizationRoleMembershipMapper();
        ProtocolMapperModel created = OrganizationRoleMembershipMapper.create();

        assertEquals(OrganizationRoleMembershipMapper.ID, mapper.getId());
        assertEquals("Organization Role Membership", mapper.getDisplayType());
        assertEquals("AttributeStatement Mapper", mapper.getDisplayCategory());
        assertEquals("Add attributes to the assertion with information about the organization role membership.", mapper.getHelpText());
        assertEquals(20, mapper.getPriority());
        assertTrue(mapper.getConfigProperties().isEmpty());
        assertTrue(mapper.isSupported(null));
        assertEquals("organization-roles", created.getName());
        assertEquals(OrganizationRoleMembershipMapper.ID, created.getProtocolMapper());
        assertEquals(SamlProtocol.LOGIN_PROTOCOL, created.getProtocol());
    }

    private Map<String, List<Object>> attributes(AttributeStatementType statement) {
        Map<String, List<Object>> result = new LinkedHashMap<>();
        statement.getAttributes().stream()
                .map(AttributeStatementType.ASTChoiceType::getAttribute)
                .forEach(attribute -> result.put(attribute.getName(), attribute.getAttributeValue()));
        statement.getAttributes().stream()
                .map(AttributeStatementType.ASTChoiceType::getAttribute)
                .map(AttributeType::getFriendlyName)
                .forEach(name -> assertTrue(name.startsWith("Organization")));
        return result;
    }

    private static class TestContext {
        private boolean organizationsEnabled = true;
        private final RealmModel realm = proxy(RealmModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getId" -> "realm";
            default -> defaultValue(method.getReturnType());
        });
        private final ClientModel serviceClient = proxy(ClientModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getId" -> "service-id";
            case "getClientId" -> "service";
            case "getRealm" -> realm;
            default -> defaultValue(method.getReturnType());
        });
        private final OrganizationModel organization = organization("org", "acme");
        private final TestRole organizationRole = new TestRole("role-admin", "admin", RoleModel.Type.ORGANIZATION, organization);
        private final TestRole organizationComposite = new TestRole("role-member", "member", RoleModel.Type.ORGANIZATION, organization);
        private final TestRole realmComposite = new TestRole("realm-role", "realm-admin", RoleModel.Type.REALM, realm);
        private final TestRole clientComposite = new TestRole("client-role", "service-admin", RoleModel.Type.CLIENT, serviceClient);
        private final TestUser user = new TestUser();
        private final UserSessionModel userSession = proxy(UserSessionModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getUser" -> user.model;
            default -> defaultValue(method.getReturnType());
        });
        private final OrganizationProvider organizationProvider = proxy(OrganizationProvider.class, (proxy, method, args) -> switch (method.getName()) {
            case "isEnabled", "hasOrganizations" -> organizationsEnabled;
            case "getByMember" -> Stream.of(organization);
            default -> defaultValue(method.getReturnType());
        });
        private final KeycloakSession session = proxy(KeycloakSession.class, (proxy, method, args) -> switch (method.getName()) {
            case "getProvider" -> args[0].equals(OrganizationProvider.class) ? organizationProvider : null;
            default -> defaultValue(method.getReturnType());
        });

        TestContext() {
            organizationRole.composites.addAll(List.of(organizationComposite.model, realmComposite.model, clientComposite.model));
            user.roleMappings.add(organizationRole.model);
        }

        private OrganizationModel organization(String id, String alias) {
            return proxy(OrganizationModel.class, (proxy, method, args) -> switch (method.getName()) {
                case "getId" -> id;
                case "getAlias" -> alias;
                case "getRealm" -> realm;
                case "isEnabled" -> true;
                case "isMember" -> args[0] == user.model;
                default -> defaultValue(method.getReturnType());
            });
        }
    }

    private static class TestUser {
        private final Set<RoleModel> roleMappings = new LinkedHashSet<>();
        private final UserModel model = proxy(UserModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getRoleMappingsStream" -> roleMappings.stream();
            case "getGroupsStream" -> Stream.empty();
            default -> defaultValue(method.getReturnType());
        });
    }

    private static class TestRole {
        private final List<RoleModel> composites = new java.util.ArrayList<>();
        private final RoleModel model;

        TestRole(String id, String name, RoleModel.Type type, RoleContainerModel container) {
            model = proxy(RoleModel.class, (proxy, method, args) -> switch (method.getName()) {
                case "getId" -> id;
                case "getName" -> name;
                case "getType" -> type;
                case "isClientRole" -> type == RoleModel.Type.CLIENT;
                case "isRealmRole" -> type == RoleModel.Type.REALM;
                case "isOrganizationRole" -> type == RoleModel.Type.ORGANIZATION;
                case "getContainer" -> container;
                case "getContainerId" -> container.getId();
                case "getCompositesStream" -> composites.stream();
                case "hasRole" -> Objects.equals(proxy, args[0]) || composites.contains(args[0]);
                default -> defaultValue(method.getReturnType());
            });
        }
    }

    private static class OrganizationFeatureResolver implements ProfileConfigResolver {
        @Override
        public Profile.ProfileName getProfileName() {
            return null;
        }

        @Override
        public FeatureConfig getFeatureConfig(String featureName) {
            return Profile.Feature.ORGANIZATION.getVersionedKey().equals(featureName) ? FeatureConfig.ENABLED : FeatureConfig.UNCONFIGURED;
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
