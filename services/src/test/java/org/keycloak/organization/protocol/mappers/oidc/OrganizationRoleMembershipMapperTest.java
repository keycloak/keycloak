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
package org.keycloak.organization.protocol.mappers.oidc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.Profile;
import org.keycloak.common.profile.ProfileConfigResolver;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.protocol.mappers.OrganizationRoleMapperUtils;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.AccessToken;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
    @SuppressWarnings("unchecked")
    public void mapsOrganizationRolesByClientSessionOrganizationNote() {
        TestContext context = new TestContext();
        context.clientSessionOrganizationId = "org";
        context.token.getOtherClaims().put(OAuth2Constants.ORGANIZATION, List.of("acme"));

        new OrganizationRoleMembershipMapper().setClaim(context.token, context.roleMapper, context.userSession, context.session, context.clientSessionContext);

        Map<String, Object> organization = (Map<String, Object>) context.token.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        Map<String, Object> acme = (Map<String, Object>) organization.get("acme");
        assertEquals(List.of("admin", "member"), acme.get(OrganizationRoleMapperUtils.ORGANIZATION_ROLES));
        assertEquals(Map.of(OrganizationRoleMapperUtils.ROLES, List.of("realm-admin")), acme.get(OrganizationRoleMapperUtils.REALM_ACCESS));
        assertEquals(Map.of("service", Map.of(OrganizationRoleMapperUtils.ROLES, List.of("service-admin"))), acme.get(OrganizationRoleMapperUtils.RESOURCE_ACCESS));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mapsOrganizationRolesByCurrentOrganizationContext() {
        TestContext context = new TestContext();
        context.currentOrganization = context.organization;

        new OrganizationRoleMembershipMapper().setClaim(context.token, context.roleMapper, context.userSession, context.session, context.clientSessionContext);

        Map<String, Object> organization = (Map<String, Object>) context.token.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertTrue(organization.containsKey("acme"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mapsOrganizationRolesByRequestedOrganizationScope() {
        TestContext context = new TestContext();
        context.scope = "openid organization:*";

        new OrganizationRoleMembershipMapper().setClaim(context.token, context.roleMapper, context.userSession, context.session, context.clientSessionContext);

        Map<String, Object> organization = (Map<String, Object>) context.token.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertTrue(organization.containsKey("acme"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void separatesOrganizationRoleClaimsAcrossOrganizations() {
        TestContext context = new TestContext();
        context.scope = "openid organization:*";
        context.user.roleMappings.add(context.otherOrganizationRole.model);

        new OrganizationRoleMembershipMapper().setClaim(context.token, context.roleMapper, context.userSession, context.session, context.clientSessionContext);

        Map<String, Object> organization = (Map<String, Object>) context.token.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        Map<String, Object> acme = (Map<String, Object>) organization.get("acme");
        Map<String, Object> other = (Map<String, Object>) organization.get("other");
        assertEquals(List.of("admin", "member"), acme.get(OrganizationRoleMapperUtils.ORGANIZATION_ROLES));
        assertEquals(List.of("viewer"), other.get(OrganizationRoleMapperUtils.ORGANIZATION_ROLES));
        assertFalse(other.containsKey(OrganizationRoleMapperUtils.REALM_ACCESS));
        assertFalse(other.containsKey(OrganizationRoleMapperUtils.RESOURCE_ACCESS));
    }

    @Test
    public void skipsWhenOrganizationsDisabledOrMembershipMapperMissing() {
        TestContext disabled = new TestContext();
        disabled.organizationsEnabled = false;
        new OrganizationRoleMembershipMapper().setClaim(disabled.token, disabled.roleMapper, disabled.userSession, disabled.session, disabled.clientSessionContext);
        assertFalse(disabled.token.getOtherClaims().containsKey(OAuth2Constants.ORGANIZATION));

        TestContext missingMembershipMapper = new TestContext();
        missingMembershipMapper.includeOrganizationMapper = false;
        new OrganizationRoleMembershipMapper().setClaim(missingMembershipMapper.token, missingMembershipMapper.roleMapper,
                missingMembershipMapper.userSession, missingMembershipMapper.session, missingMembershipMapper.clientSessionContext);
        assertFalse(missingMembershipMapper.token.getOtherClaims().containsKey(OAuth2Constants.ORGANIZATION));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void ignoresOrganizationsWithoutRoleClaimsAndReplacesNonMapOrganizationData() {
        TestContext context = new TestContext();
        context.clientSessionOrganizationId = "org";
        context.user.roleMappings.clear();
        new OrganizationRoleMembershipMapper().setClaim(context.token, context.roleMapper, context.userSession, context.session, context.clientSessionContext);
        Map<String, Object> organization = (Map<String, Object>) context.token.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        assertTrue(organization.isEmpty());

        context.user.roleMappings.add(context.organizationRole.model);
        organization.put("acme", "not-a-map");
        new OrganizationRoleMembershipMapper().setClaim(context.token, context.roleMapper, context.userSession, context.session, context.clientSessionContext);
        organization = (Map<String, Object>) context.token.getOtherClaims().get(OAuth2Constants.ORGANIZATION);
        Map<String, Object> acme = (Map<String, Object>) organization.get("acme");
        assertEquals(List.of("admin", "member"), acme.get(OrganizationRoleMapperUtils.ORGANIZATION_ROLES));
    }

    @Test
    public void exposesProviderMetadataAndCreateDefaults() {
        OrganizationRoleMembershipMapper mapper = new OrganizationRoleMembershipMapper();

        assertEquals(OrganizationRoleMembershipMapper.PROVIDER_ID, mapper.getId());
        assertEquals("Organization Role Membership", mapper.getDisplayType());
        assertEquals("Token mapper", mapper.getDisplayCategory());
        assertEquals("Map user Organization role membership", mapper.getHelpText());
        assertEquals(20, mapper.getPriority());
        assertTrue(mapper.isSupported(null));
        assertFalse(mapper.getConfigProperties().isEmpty());

        ProtocolMapperModel created = OrganizationRoleMembershipMapper.create("organization roles", true, true, true);
        assertEquals(OrganizationRoleMembershipMapper.PROVIDER_ID, created.getProtocolMapper());
        assertEquals("true", created.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN));
        assertEquals("true", created.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN));
        assertEquals("true", created.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION));

        ProtocolMapperModel excluded = OrganizationRoleMembershipMapper.create("organization roles", false, false, false);
        assertNull(excluded.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN));
        assertNull(excluded.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN));
        assertNull(excluded.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION));
    }

    private static class TestContext {
        private final Map<String, Object> sessionAttributes = new HashMap<>();
        private boolean organizationsEnabled = true;
        private boolean includeOrganizationMapper = true;
        private String clientSessionOrganizationId;
        private String scope = "openid";
        private OrganizationModel currentOrganization;
        private final AccessToken token = new AccessToken();
        private final RealmModel realm = proxy(RealmModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getId" -> "realm";
            case "getName" -> "realm";
            case "isOrganizationsEnabled" -> true;
            default -> defaultValue(method.getReturnType());
        });
        private final ClientModel serviceClient = proxy(ClientModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getId" -> "service-id";
            case "getClientId" -> "service";
            case "getRealm" -> realm;
            case "isSurrogateAuthRequired" -> false;
            default -> defaultValue(method.getReturnType());
        });
        private final OrganizationModel organization = organization("org", "acme");
        private final OrganizationModel otherOrganization = organization("other-org", "other");
        private final TestRole organizationRole = new TestRole("role-admin", "admin", RoleModel.Type.ORGANIZATION, organization);
        private final TestRole organizationComposite = new TestRole("role-member", "member", RoleModel.Type.ORGANIZATION, organization);
        private final TestRole realmComposite = new TestRole("realm-role", "realm-admin", RoleModel.Type.REALM, realm);
        private final TestRole clientComposite = new TestRole("client-role", "service-admin", RoleModel.Type.CLIENT, serviceClient);
        private final TestRole otherOrganizationRole = new TestRole("other-role", "viewer", RoleModel.Type.ORGANIZATION, otherOrganization);
        private final TestUser user = new TestUser();
        private final ProtocolMapperModel organizationMapper = OrganizationMembershipMapper.create(OAuth2Constants.ORGANIZATION, true, true, true);
        private final ProtocolMapperModel roleMapper = OrganizationRoleMembershipMapper.create("organization roles", true, true, true);
        private final ClientScopeModel organizationScope = proxy(ClientScopeModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getName" -> OAuth2Constants.ORGANIZATION;
            case "getProtocolMappersStream" -> Stream.of(organizationMapper);
            default -> defaultValue(method.getReturnType());
        });
        private final ClientModel client = proxy(ClientModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getId" -> "client";
            case "getClientId" -> "client";
            case "getClientScopes" -> Map.of(OAuth2Constants.ORGANIZATION, organizationScope);
            case "getAttribute" -> null;
            default -> defaultValue(method.getReturnType());
        });
        private final AuthenticatedClientSessionModel clientSession = proxy(AuthenticatedClientSessionModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getNote" -> OrganizationModel.ORGANIZATION_ATTRIBUTE.equals(args[0]) ? clientSessionOrganizationId : null;
            case "getClient" -> client;
            default -> defaultValue(method.getReturnType());
        });
        private final ClientSessionContext clientSessionContext = proxy(ClientSessionContext.class, (proxy, method, args) -> switch (method.getName()) {
            case "getClientSession" -> clientSession;
            case "getScopeString" -> scope;
            case "getProtocolMappersStream" -> includeOrganizationMapper ? Stream.of(organizationMapper, roleMapper) : Stream.of(roleMapper);
            default -> defaultValue(method.getReturnType());
        });
        private final UserSessionModel userSession = proxy(UserSessionModel.class, (proxy, method, args) -> switch (method.getName()) {
            case "getId" -> "user-session";
            case "getUser" -> user.model;
            case "getRealm" -> realm;
            default -> defaultValue(method.getReturnType());
        });
        private final OrganizationProvider organizationProvider = proxy(OrganizationProvider.class, (proxy, method, args) -> switch (method.getName()) {
            case "isEnabled" -> organizationsEnabled;
            case "getById", "getByAlias" -> organization;
            case "getByMember" -> Stream.of(organization, otherOrganization);
            default -> defaultValue(method.getReturnType());
        });
        private final KeycloakContext keycloakContext = proxy(KeycloakContext.class, (proxy, method, args) -> switch (method.getName()) {
            case "getRealm" -> realm;
            case "getOrganization" -> currentOrganization;
            case "getClient" -> client;
            default -> defaultValue(method.getReturnType());
        });
        private final KeycloakSession session = proxy(KeycloakSession.class, (proxy, method, args) -> switch (method.getName()) {
            case "getContext" -> keycloakContext;
            case "getProvider" -> args[0].equals(OrganizationProvider.class) ? organizationProvider : null;
            case "getAttributeOrDefault" -> sessionAttributes.getOrDefault(args[0], args[1]);
            case "setAttribute" -> {
                sessionAttributes.put((String) args[0], args[1]);
                yield null;
            }
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
            case "getId" -> "user";
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
