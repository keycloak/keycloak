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

package org.keycloak.tests.federation.ldap;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.HardcodedAttributeMapper;
import org.keycloak.storage.ldap.mappers.HardcodedAttributeMapperFactory;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapper;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.util.ldap.LDAPEmbeddedServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class LDAPReadOnlyAttributesTest {

    private static final String USERNAME = "johnkeycloak";
    private static final String EMAIL = "john@email.org";
    private static final String PASSWORD = "Password1";
    private static final String LOCAL_ATTRIBUTE = "localAttribute";
    private static final String DUPLICATE_ATTRIBUTE = "duplicateMappedAttribute";

    private static LDAPEmbeddedServer ldap;

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectOAuthClient(config = DirectGrantClient.class, lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectSimpleHttp
    SimpleHttp http;

    @InjectRunOnServer(permittedPackages = {"org.keycloak.tests.federation.ldap", "org.keycloak.testsuite.util"}, lifecycle = LifeCycle.METHOD)
    RunOnServerClient runOnServer;

    @BeforeAll
    static void startLdap() throws Exception {
        Properties properties = new Properties();
        properties.setProperty(LDAPEmbeddedServer.PROPERTY_DSF, LDAPEmbeddedServer.DSF_INMEMORY);
        try (ServerSocket socket = new ServerSocket(0)) {
            properties.setProperty(LDAPEmbeddedServer.PROPERTY_BIND_PORT, Integer.toString(socket.getLocalPort()));
        }
        ldap = new LDAPEmbeddedServer(properties);
        ldap.init();
        ldap.start();
    }

    @AfterAll
    static void stopLdap() throws Exception {
        if (ldap != null) {
            ldap.stop();
        }
    }

    @BeforeEach
    void configureLdap() {
        String ldapUrl = UriBuilder.newInstance().scheme("ldap").host(ldap.getBindHost()).port(ldap.getBindPort()).build().toString();
        ComponentRepresentation provider = new ComponentRepresentation();
        provider.setName("test-ldap");
        provider.setProviderId(LDAPStorageProviderFactory.PROVIDER_NAME);
        provider.setProviderType(UserStorageProvider.class.getName());
        provider.setConfig(new MultivaluedHashMap<>(Map.of(
                LDAPConstants.CONNECTION_URL, List.of(ldapUrl),
                LDAPConstants.USERS_DN, List.of("ou=People,dc=keycloak,dc=org"),
                LDAPConstants.BIND_DN, List.of("uid=admin,ou=system"),
                LDAPConstants.BIND_CREDENTIAL, List.of("secret"),
                LDAPConstants.EDIT_MODE, List.of(UserStorageProvider.EditMode.READ_ONLY.name()),
                UserStorageProviderModel.IMPORT_ENABLED, List.of("true"),
                UserStorageProviderModel.CACHE_POLICY, List.of(UserStorageProviderModel.CachePolicy.NO_CACHE.name()))));
        ApiUtil.getCreatedId(realm.admin().components().add(provider));
        runOnServer.run(ServerCode.seedLdapUser());
    }

    @Test
    void testAccountCanUpdateLocalAttributes() throws IOException {
        configureEditableAttribute(LOCAL_ATTRIBUTE, Set.of("admin", "user"));
        configureEditableAttribute("adminOnlyAttribute", Set.of("admin"));
        configureEditableAttribute("protectedAttribute", Set.of());

        AccessTokenResponse response = oauth.scope("openid").doPasswordGrantRequest(USERNAME, PASSWORD);
        assertEquals(200, response.getStatusCode(), response.getErrorDescription());
        String token = response.getAccessToken();
        UserRepresentation user = accountProfile(token);
        user.singleAttribute(LOCAL_ATTRIBUTE, "first");
        assertEquals(204, updateAccountProfile(token, user));
        user = accountProfile(token);
        assertEquals(List.of("first"), attribute(user, LOCAL_ATTRIBUTE));

        user.singleAttribute(LOCAL_ATTRIBUTE, "second");
        assertEquals(204, updateAccountProfile(token, user));
        user = accountProfile(token);
        assertEquals(List.of("second"), attribute(user, LOCAL_ATTRIBUTE));

        user.getAttributes().remove(LOCAL_ATTRIBUTE);
        assertEquals(204, updateAccountProfile(token, user));
        user = accountProfile(token);
        assertNull(attribute(user, LOCAL_ATTRIBUTE));
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals(EMAIL, user.getEmail());

        for (String name : List.of("adminOnlyAttribute", "protectedAttribute")) {
            user = accountProfile(token);
            user.singleAttribute(name, "forbidden");
            assertEquals(400, updateAccountProfile(token, user), name);
            assertNull(attribute(accountProfile(token), name));
        }

        user = accountProfile(token);
        user.setFirstName("Changed");
        assertEquals(400, updateAccountProfile(token, user));
        assertEquals("John", accountProfile(token).getFirstName());
    }

    @Test
    void testLocalAttributeWritesDoNotUnlockLdapAttributes() {
        runOnServer.run(ServerCode.checkLocalAndLdapWrites());
    }

    @Test
    void testMappedLocaleRemainsReadOnly() {
        RealmRepresentation config = realm.admin().toRepresentation();
        config.setInternationalizationEnabled(true);
        realm.admin().update(config);
        runOnServer.run(ServerCode.addHardcodedMapper(UserModel.LOCALE, "en"));
        runOnServer.run(ServerCode.checkMappedLocale());
    }

    @Test
    void testMappedAttributeNamesAreCaseInsensitive() {
        configureEditableAttribute("CITY", Set.of("admin", "user"));
        runOnServer.run(ServerCode.configureCaseInsensitiveMappers());
        runOnServer.run(ServerCode.checkCaseInsensitiveMappers());
    }

    @Test
    void testSearchByHardcodedEnabledAttribute() {
        runOnServer.run(ServerCode.addHardcodedMapper(UserModel.ENABLED, "true"));
        assertTrue(realm.admin().users().searchByAttributes("enabled:true").stream()
                .anyMatch(user -> USERNAME.equals(user.getUsername())));
    }

    @Test
    void testDuplicateMappedAttributes() {
        UserResource user = ldapUser();
        runOnServer.run(ServerCode.addHardcodedMapper(DUPLICATE_ATTRIBUTE, "value"));
        runOnServer.run(ServerCode.addDuplicateUserAttributeMapper());

        assertEquals(1, user.toRepresentation(true).getUserProfileMetadata().getAttributes().stream()
                .filter(attribute -> DUPLICATE_ATTRIBUTE.equals(attribute.getName())).count());
    }

    @Test
    void testEmailAsUsernameIsReadOnlyWithoutEmailMapper() {
        UserResource user = ldapUser();
        runOnServer.run(ServerCode.renameEmailMapper("unmappedEmail"));

        RealmRepresentation config = realm.admin().toRepresentation();
        config.setEditUsernameAllowed(false);
        config.setRegistrationEmailAsUsername(true);
        realm.admin().update(config);

        var profile = user.toRepresentation(true);
        assertTrue(profile.getUserProfileMetadata().getAttributeMetadata(UserModel.EMAIL).isReadOnly());
        profile.setEmail("john@newemail.org");
        assertThrows(BadRequestException.class, () -> user.update(profile));
        assertEquals(EMAIL, user.toRepresentation().getEmail());
        runOnServer.run(ServerCode.checkEmailWrite());
    }

    private void configureEditableAttribute(String name, Set<String> editRoles) {
        var resource = realm.admin().users().userProfile();
        UPConfig config = resource.getConfiguration();
        config.addOrReplaceAttribute(new UPAttribute(name, new UPAttributePermissions(Set.of("admin", "user"), editRoles)));
        resource.update(config);
    }

    private UserResource ldapUser() {
        String id = realm.admin().users().searchByUsername(USERNAME, true).get(0).getId();
        return realm.admin().users().get(id);
    }

    private UserRepresentation accountProfile(String token) throws IOException {
        return http.doGet(realm.getBaseUrl() + "/account").auth(token).asJson(UserRepresentation.class);
    }

    private int updateAccountProfile(String token, UserRepresentation user) throws IOException {
        return http.doPost(realm.getBaseUrl() + "/account").auth(token).json(user).asStatus();
    }

    private static List<String> attribute(UserRepresentation user, String name) {
        return user.getAttributes() == null ? null : user.getAttributes().get(name);
    }

    public static class DirectGrantClient implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("test-app").secret("password").directAccessGrantsEnabled(true);
        }
    }

    public static class ServerCode {

        static RunOnServer seedLdapUser() {
            return session -> {
                RealmModel realm = session.getContext().getRealm();
                ComponentModel model = LDAPTestUtils.getLdapProviderModel(realm);
                LDAPStorageProvider provider = LDAPTestUtils.getLdapProvider(session, model);
                LDAPTestUtils.addZipCodeLDAPMapper(realm, model);
                LDAPTestUtils.removeAllLDAPUsers(provider, realm);
                LDAPObject user = LDAPTestUtils.addLDAPUser(provider, realm, USERNAME, "John", "Doe", EMAIL, null, "1234");
                LDAPTestUtils.updateLDAPPassword(provider, user, PASSWORD);
            };
        }

        static RunOnServer checkLocalAndLdapWrites() {
            return session -> {
                RealmModel realm = session.getContext().getRealm();
                UserModel user = session.users().getUserByUsername(realm, USERNAME);
                user.setSingleAttribute(LOCAL_ATTRIBUTE, "first");
                assertEquals("first", user.getFirstAttribute(LOCAL_ATTRIBUTE));
                user.setAttribute(LOCAL_ATTRIBUTE, List.of("second", "third"));
                assertEquals(List.of("second", "third"), user.getAttributeStream(LOCAL_ATTRIBUTE).toList());
                user.removeAttribute(LOCAL_ATTRIBUTE);
                assertNull(user.getFirstAttribute(LOCAL_ATTRIBUTE));

                for (String name : List.of(UserModel.USERNAME, UserModel.EMAIL, UserModel.FIRST_NAME, UserModel.LAST_NAME,
                        "postal_code", LDAPConstants.LDAP_ID, LDAPConstants.LDAP_ENTRY_DN)) {
                    List<String> original = user.getAttributeStream(name).toList();
                    user.setAttribute(name, original);
                    assertThrows(ReadOnlyException.class, () -> user.setSingleAttribute(name, "changed"), name);
                    assertThrows(ReadOnlyException.class, () -> user.setAttribute(name, List.of("changed")), name);
                    assertThrows(ReadOnlyException.class, () -> user.removeAttribute(name), name);
                    assertEquals(original, user.getAttributeStream(name).toList());
                }

                assertThrows(ReadOnlyException.class, () -> user.setUsername("changed"));
                assertThrows(ReadOnlyException.class, () -> user.setFirstName("changed"));
                assertThrows(ReadOnlyException.class, () -> user.setLastName("changed"));
                assertThrows(ReadOnlyException.class, () -> user.setEmail("changed@example.org"));
                LDAPStorageProvider provider = LDAPTestUtils.getLdapProvider(session, LDAPTestUtils.getLdapProviderModel(realm));
                assertThrows(ReadOnlyException.class, () -> provider.updateCredential(realm, user, UserCredentialModel.password("ChangedPassword1")));
            };
        }

        static RunOnServer addHardcodedMapper(String name, String value) {
            return session -> {
                RealmModel realm = session.getContext().getRealm();
                ComponentModel model = LDAPTestUtils.getLdapProviderModel(realm);
                realm.addComponentModel(KeycloakModelUtils.createComponentModel("hardcoded-" + name, model.getId(),
                        HardcodedAttributeMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                        HardcodedAttributeMapper.USER_MODEL_ATTRIBUTE, name, HardcodedAttributeMapper.ATTRIBUTE_VALUE, value));
            };
        }

        static RunOnServer checkMappedLocale() {
            return session -> {
                UserModel user = session.users().getUserByUsername(session.getContext().getRealm(), USERNAME);
                assertTrue(session.getProvider(UserProfileProvider.class).create(UserProfileContext.USER_API, user)
                        .getAttributes().isReadOnly(UserModel.LOCALE));
                assertThrows(ReadOnlyException.class, () -> user.setSingleAttribute(UserModel.LOCALE, "fr"));
            };
        }

        static RunOnServer configureCaseInsensitiveMappers() {
            return session -> {
                RealmModel realm = session.getContext().getRealm();
                ComponentModel model = LDAPTestUtils.getLdapProviderModel(realm);
                ComponentModel email = LDAPTestUtils.getSubcomponentByName(realm, model, "email");
                email.put(UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE, "EMAIL");
                realm.updateComponent(email);

                ComponentModel city = LDAPTestUtils.addUserAttributeMapper(realm, model, "city", "city", "l");
                city.put(UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, "true");
                realm.updateComponent(city);
                LDAPStorageProvider provider = LDAPTestUtils.getLdapProvider(session, model);
                LDAPObject user = provider.loadLDAPUserByUsername(realm, USERNAME);
                user.setSingleAttribute("l", "Brno");
                provider.getLdapIdentityStore().update(user);
            };
        }

        static RunOnServer checkCaseInsensitiveMappers() {
            return session -> {
                UserModel user = session.users().getUserByUsername(session.getContext().getRealm(), USERNAME);
                assertEquals(List.of("Brno"), user.getAttributeStream("CITY").toList());
                assertTrue(session.getProvider(UserProfileProvider.class).create(UserProfileContext.USER_API, user)
                        .getAttributes().isReadOnly("CITY"));
                assertThrows(ReadOnlyException.class, () -> user.setSingleAttribute("CITY", "Paris"));
                assertThrows(ReadOnlyException.class, () -> user.setEmail("changed@example.org"));
                assertThrows(ReadOnlyException.class, () -> user.setSingleAttribute("EMAIL", "changed@example.org"));
            };
        }

        static RunOnServer addDuplicateUserAttributeMapper() {
            return session -> {
                RealmModel realm = session.getContext().getRealm();
                ComponentModel model = LDAPTestUtils.getLdapProviderModel(realm);
                LDAPTestUtils.addUserAttributeMapper(realm, model, "duplicate-user-attribute", DUPLICATE_ATTRIBUTE, "description");
            };
        }

        static RunOnServer renameEmailMapper(String name) {
            return session -> {
                RealmModel realm = session.getContext().getRealm();
                ComponentModel email = LDAPTestUtils.getSubcomponentByName(realm, LDAPTestUtils.getLdapProviderModel(realm), "email");
                email.put(UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE, name);
                realm.updateComponent(email);
            };
        }

        static RunOnServer checkEmailWrite() {
            return session -> {
                UserModel user = session.users().getUserByUsername(session.getContext().getRealm(), USERNAME);
                assertThrows(ReadOnlyException.class, () -> user.setEmail("john@newemail.org"));
            };
        }
    }
}
