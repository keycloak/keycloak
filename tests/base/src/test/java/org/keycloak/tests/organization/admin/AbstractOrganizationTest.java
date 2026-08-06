/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.organization.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.OrganizationModel.IdentityProviderRedirectMode;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.CredentialBuilder;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginUpdateProfilePage;
import org.keycloak.testframework.ui.page.LoginUsernamePage;
import org.keycloak.testframework.util.ApiUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base class for organization tests in the new test framework.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractOrganizationTest {

    protected String organizationName = "neworg";
    protected String memberEmail = "jdoe@neworg.org";
    protected String memberPassword = "password";

    @InjectRealm(config = OrganizationRealmConfig.class)
    protected ManagedRealm realm;

    protected OrganizationRepresentation createOrganization() {
        return createOrganization(organizationName);
    }

    protected OrganizationRepresentation createOrganization(String name) {
        return createOrganization(name, name + ".org");
    }

    protected OrganizationRepresentation createOrganization(String name, String... orgDomains) {
        return createOrganization(realm, name, orgDomains);
    }

    protected OrganizationRepresentation createOrganization(ManagedRealm managedRealm, String name, String... orgDomains) {
        return createOrganization(managedRealm, name, createOrgBroker(name), orgDomains);
    }

    protected OrganizationRepresentation createOrganization(String name, boolean isBrokerPublic) {
        IdentityProviderRepresentation broker = createOrgBroker(name);
        broker.setHideOnLogin(!isBrokerPublic);
        return createOrganization(realm, name, broker, name + ".org");
    }

    protected OrganizationRepresentation createOrganization(ManagedRealm managedRealm, String name,
                                                            IdentityProviderRepresentation broker, String... orgDomains) {
        OrganizationRepresentation org = createRepresentation(name, orgDomains);
        String id;

        try (Response response = managedRealm.admin().organizations().create(org)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            id = ApiUtil.getCreatedId(response);
        }

        if (orgDomains != null && orgDomains.length > 0) {
            broker.getConfig().put(OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, orgDomains[0]);
            broker.getConfig().put(IdentityProviderRedirectMode.EMAIL_MATCH.getKey(), Boolean.TRUE.toString());
        }
        managedRealm.admin().identityProviders().create(broker).close();

        managedRealm.admin().organizations().get(id).identityProviders().addIdentityProvider(broker.getAlias()).close();
        org = managedRealm.admin().organizations().get(id).toRepresentation();

        String orgId = id;
        // org deletion must run before IdP removal — removing an IdP linked to an org may fail
        managedRealm.cleanup().add(r -> {
            try {
                r.organizations().get(orgId).delete().close();
            } catch (NotFoundException ignored) {}
        });
        String brokerAlias = broker.getAlias();
        managedRealm.cleanup().add(r -> {
            try {
                r.identityProviders().get(brokerAlias).remove();
            } catch (NotFoundException ignored) {}
        });

        return org;
    }

    protected OrganizationRepresentation createRepresentation(String name, String... orgDomains) {
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setName(name);
        org.setAlias(name);
        org.setDescription(name + " is a test organization!");

        if (orgDomains != null) {
            for (String orgDomain : orgDomains) {
                OrganizationDomainRepresentation domainRep = new OrganizationDomainRepresentation();
                domainRep.setName(orgDomain);
                org.addDomain(domainRep);
            }
        }

        org.setAttributes(Map.of("key", List.of("value1", "value2")));

        return org;
    }

    protected MemberRepresentation addMember(OrganizationResource organization) {
        return addMember(organization, memberEmail);
    }

    protected MemberRepresentation addMember(OrganizationResource organization, String email) {
        return addMember(organization, null, email, "FirstName", "LastName", true);
    }

    protected MemberRepresentation addMember(OrganizationResource organization, String email, String firstName, String lastName) {
        return addMember(organization, null, email, firstName, lastName, true);
    }

    protected MemberRepresentation addMember(OrganizationResource organization, String username, String email,
                                             String firstName, String lastName, boolean isSetCredentials) {
        UserRepresentation expected = new UserRepresentation();

        expected.setEmail(email);
        expected.setUsername(username == null ? expected.getEmail() : username);
        expected.setEnabled(true);
        expected.setEmailVerified(true);
        expected.setFirstName(firstName);
        expected.setLastName(lastName);
        try (Response response = realm.admin().users().create(expected)) {
            expected.setId(ApiUtil.getCreatedId(response));
        }

        if (isSetCredentials) {
            realm.admin().users().get(expected.getId()).resetPassword(
                    CredentialBuilder.password(memberPassword).build());
        }

        String userId = expected.getId();
        realm.cleanup().add(r -> {
            try {
                r.users().get(userId).remove();
            } catch (NotFoundException ignored) {}
        });

        try (Response response = organization.members().addMember(userId)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            MemberRepresentation actual = organization.members().member(userId).toRepresentation();

            assertNotNull(expected);
            assertEquals(userId, actual.getId());
            assertEquals(expected.getUsername(), actual.getUsername());
            assertEquals(expected.getEmail(), actual.getEmail());

            return actual;
        }
    }

    protected UserRepresentation getUserRepresentation(String userEmail) {
        UsersResource users = realm.admin().users();
        List<UserRepresentation> reps = users.searchByEmail(userEmail, true);
        assertFalse(reps.isEmpty());
        assertEquals(1, reps.size());
        return reps.get(0);
    }

    protected GroupRepresentation createGroup(RealmResource realmResource, String name) {
        GroupRepresentation group = new GroupRepresentation();
        group.setName(name);
        try (Response response = realmResource.groups().add(group)) {
            String groupId = ApiUtil.getCreatedId(response);
            group.setId(groupId);
            return group;
        }
    }

    protected void setMapperConfig(String key, String value) {
        ClientScopeRepresentation orgScope = realm.admin().clientScopes().findAll().stream()
                .filter(s -> OIDCLoginProtocolFactory.ORGANIZATION.equals(s.getName()))
                .findAny()
                .orElseThrow();
        var orgScopeResource = realm.admin().clientScopes().get(orgScope.getId());

        ProtocolMapperRepresentation orgMapper = orgScopeResource.getProtocolMappers().getMappers().stream()
                .filter(m -> OIDCLoginProtocolFactory.ORGANIZATION.equals(m.getName()))
                .findAny()
                .orElseThrow();

        Map<String, String> config = orgMapper.getConfig();

        if (value == null) {
            config.remove(key);
        } else {
            config.put(key, value);
        }

        orgScopeResource.getProtocolMappers().update(orgMapper.getId(), orgMapper);
    }

    /**
     * Builds an IdP representation pointing to a real provider realm's OIDC endpoints.
     */
    public static IdentityProviderRepresentation createRealOrgBroker(String alias, ManagedRealm providerRealm) {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias(alias);
        idp.setProviderId("keycloak-oidc");
        idp.setEnabled(true);
        idp.setTrustEmail(true);
        String providerBaseUrl = providerRealm.getBaseUrl();
        idp.setConfig(new HashMap<>(Map.of(
                "clientId", CLIENT_ID,
                "clientSecret", CLIENT_SECRET,
                "authorizationUrl", providerBaseUrl + "/protocol/openid-connect/auth",
                "tokenUrl", providerBaseUrl + "/protocol/openid-connect/token",
                "userInfoUrl", providerBaseUrl + "/protocol/openid-connect/userinfo",
                "defaultScope", "email profile",
                "syncMode", "IMPORT"
        )));
        return idp;
    }

    /**
     * Performs identity-first login through a broker: enters email on the consumer realm login page,
     * follows the redirect to the provider realm, fills credentials, and optionally handles first-time
     * login profile update. Registers cleanup for the federated user created in the consumer realm.
     */
    protected void loginViaBroker(String email, String username, String password,
            OAuthClient oauth, LoginUsernamePage loginUsernamePage, LoginPage loginPage,
            ManagedRealm providerRealm) {
        loginViaBroker(email, username, password, null, oauth, loginUsernamePage, loginPage, null, providerRealm);
    }

    protected void loginViaBroker(String email, String username, String password,
            String updateEmail, OAuthClient oauth, LoginUsernamePage loginUsernamePage,
            LoginPage loginPage, LoginUpdateProfilePage loginUpdateProfilePage,
            ManagedRealm providerRealm) {
        oauth.openLoginForm();
        loginUsernamePage.fillLoginWithUsernameOnly(email);
        loginUsernamePage.submit();

        assertTrue(Objects.requireNonNull(oauth.getDriver().getCurrentUrl()).contains("/realms/" + providerRealm.getName() + "/"),
                "Should be on provider realm login page");

        loginPage.fillLogin(username, password);
        loginPage.submit();

        if (updateEmail != null) {
            String currentUrl = Objects.requireNonNull(oauth.getDriver().getCurrentUrl());
            if (currentUrl.contains("/login-actions/") || currentUrl.contains("/broker/")) {
                loginUpdateProfilePage.update("Firstname", "Lastname", updateEmail);
            }
        }

        String searchEmail = updateEmail != null ? updateEmail : email;
        List<UserRepresentation> users = realm.admin().users().searchByEmail(searchEmail, true);
        assertEquals(1, users.size(), "Federated user should be created in consumer realm");

        String userId = users.get(0).getId();
        realm.cleanup().add(r -> {
            try {
                r.users().get(userId).remove();
            } catch (NotFoundException ignored) {}
        });
    }

    /**
     * Performs broker registration and verifies the user becomes an organization member.
     * Equivalent to the old AbstractOrganizationTest.assertBrokerRegistration().
     */
    protected void assertBrokerRegistration(OrganizationResource organization,
            String username, String email,
            OAuthClient oauth, LoginUsernamePage loginUsernamePage, LoginPage loginPage,
            LoginUpdateProfilePage loginUpdateProfilePage, ManagedRealm providerRealm) {
        loginViaBroker(email, username, "password", email,
                oauth, loginUsernamePage, loginPage, loginUpdateProfilePage, providerRealm);

        assertIsMember(email, organization);
    }

    protected void assertIsMember(String userEmail, OrganizationResource organization) {
        UserRepresentation account = getUserRepresentation(userEmail);
        UserRepresentation member = organization.members().member(account.getId()).toRepresentation();
        assertEquals(account.getId(), member.getId());
    }

    /**
     * Creates an OIDC identity provider representation for the given organization name.
     */
    protected IdentityProviderRepresentation createOrgBroker(String orgName) {
        return IdentityProviderBuilder.create()
                .providerId(OIDCIdentityProviderFactory.PROVIDER_ID)
                .alias(orgName + "-identity-provider")
                .attribute("clientId", "broker-app")
                .attribute("clientSecret", "broker-secret")
                .attribute(IdentityProviderModel.SYNC_MODE, "IMPORT")
                .hideOnLoginPage()
                .build();
    }

    public static void setUpOrgBroker(ManagedRealm providerRealm, ManagedRealm consumerRealm, String orgDomain) {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias(IDP_ALIAS);
        idp.setProviderId("keycloak-oidc");
        idp.setEnabled(true);
        idp.setStoreToken(false);
        idp.setTrustEmail(true);

        String providerBaseUrl = providerRealm.getBaseUrl();
        idp.setConfig(Map.of(
                "clientId", CLIENT_ID,
                "clientSecret", CLIENT_SECRET,
                "authorizationUrl", providerBaseUrl + "/protocol/openid-connect/auth",
                "tokenUrl", providerBaseUrl + "/protocol/openid-connect/token",
                "userInfoUrl", providerBaseUrl + "/protocol/openid-connect/userinfo",
                "defaultScope", "email profile",
                "syncMode", "IMPORT",
                OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE, orgDomain,
                IdentityProviderRedirectMode.EMAIL_MATCH.getKey(), Boolean.TRUE.toString()
        ));

        consumerRealm.admin().identityProviders().create(idp).close();
        consumerRealm.cleanup().add(r -> {
            try {
                r.identityProviders().get(IDP_ALIAS).remove();
            } catch (Exception ignored) {}
        });
    }

    /**
     * Realm configuration with organizations enabled.
     */
    public static class OrganizationRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.organizationsEnabled(true);
        }
    }

    protected void createTestClients() {
        if (realm.admin().clients().findByClientId("direct-grant").isEmpty()) {
            realm.admin().clients().create(
                    ClientBuilder.create("direct-grant")
                            .secret("password")
                            .directAccessGrantsEnabled()
                            .redirectUris("*")
                            .build()
            ).close();
        }
        if (realm.admin().clients().findByClientId("broker-app").isEmpty()) {
            ProtocolMapperRepresentation audienceMapper = new ProtocolMapperRepresentation();
            audienceMapper.setName("audience-broker-app");
            audienceMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            audienceMapper.setProtocolMapper(AudienceProtocolMapper.PROVIDER_ID);
            audienceMapper.setConfig(Map.of(
                    AudienceProtocolMapper.INCLUDED_CUSTOM_AUDIENCE, "broker-app",
                    OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true"
            ));
            realm.admin().clients().create(
                    ClientBuilder.create("broker-app")
                            .secret("broker-app-secret")
                            .directAccessGrantsEnabled()
                            .redirectUris("*")
                            .protocolMappers(audienceMapper)
                            .build()
            ).close();
        }
        try {
            RequiredActionProviderRepresentation verifyProfile = realm.admin().flows()
                    .getRequiredAction("VERIFY_PROFILE");
            if (verifyProfile.isEnabled()) {
                verifyProfile.setEnabled(false);
                verifyProfile.setDefaultAction(false);
                realm.admin().flows().updateRequiredAction("VERIFY_PROFILE", verifyProfile);
            }
        } catch (NotFoundException ignored) {
        }
    }

    public static final String IDP_ALIAS = "org-identity-provider";
    public static final String CLIENT_ID = "broker-app";
    public static final String CLIENT_SECRET = "broker-secret";

    public static class ProviderRealmConf implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.clients(
                    ClientBuilder.create(CLIENT_ID)
                            .name(CLIENT_ID)
                            .secret(CLIENT_SECRET)
                            .redirectUris("*")
                            .directAccessGrantsEnabled()
            );
        }
    }

    public static class AliceUserConf implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder builder) {
            return builder.username("alice")
                    .password("password")
                    .email("alice@neworg.org")
                    .emailVerified(true)
                    .name("Alice", "Org");
        }
    }
}
