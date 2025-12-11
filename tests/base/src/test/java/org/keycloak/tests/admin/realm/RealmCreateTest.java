package org.keycloak.tests.admin.realm;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.tests.utils.Assert;
import org.keycloak.util.JsonSerialization;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class RealmCreateTest extends AbstractRealmTest {

    @Test
    public void getRealms() {
        List<RealmRepresentation> realms = adminClient.realms().findAll();
        Assert.assertNames(realms, "master", managedRealm.getName());
    }

    @Test
    public void getRealmRepresentation() {
        RealmRepresentation rep = managedRealm.admin().toRepresentation();
        Assertions.assertEquals(managedRealm.getName(), rep.getRealm());
        assertTrue(rep.isEnabled());
    }

    @Test
    public void createRealmEmpty() {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm("new-realm");

        adminClient.realms().create(rep);

        Assert.assertNames(adminClient.realms().findAll(), "master", managedRealm.getName(), "new-realm");

        List<String> clients = adminClient.realms().realm("new-realm").clients().findAll().stream().map(ClientRepresentation::getClientId).collect(Collectors.toList());
        assertThat(clients, containsInAnyOrder("account", "account-console", "admin-cli", "broker", "realm-management", "security-admin-console"));

        adminClient.realms().realm("new-realm").remove();

        Assert.assertNames(adminClient.realms().findAll(), "master", managedRealm.getName());
    }

    @Test
    public void createRealmWithValidConsoleUris() throws Exception {
        var realmNameWithSpaces = "new realm";

        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm(realmNameWithSpaces);
        rep.setEnabled(Boolean.TRUE);
        rep.setUsers(Collections.singletonList(UserConfigBuilder.create()
                .username("new-realm-admin")
                .name("new-realm-admin", "new-realm-admin")
                .email("new-realm-admin@keycloak.org")
                .emailVerified(true)
                .password("password")
                .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN)
                .build()));

        adminClient.realms().create(rep);

        Assert.assertNames(adminClient.realms().findAll(), "master", managedRealm.getName(), realmNameWithSpaces);

        final var urlPlaceHolders = ImmutableSet.of("${authBaseUrl}", "${authAdminUrl}");

        RealmResource newRealm = adminClient.realms().realm(realmNameWithSpaces);
        List<String> clientUris = newRealm.clients()
                .findAll()
                .stream()
                .flatMap(client -> Stream.concat(Stream.concat(Stream.concat(
                                        client.getRedirectUris().stream(),
                                        Stream.of(client.getBaseUrl())),
                                Stream.of(client.getRootUrl())),
                        Stream.of(client.getAdminUrl())))
                .filter(Objects::nonNull)
                .filter(uri -> !urlPlaceHolders.contains(uri))
                .collect(Collectors.toList());

        assertThat(clientUris, not(empty()));
        assertThat(clientUris, everyItem(containsString("/new%20realm/")));

        try (Keycloak client = adminClientFactory.create().realm(realmNameWithSpaces)
                .username("new-realm-admin").password("password").clientId(Constants.ADMIN_CLI_CLIENT_ID).build()) {
            Assertions.assertNotNull(client.serverInfo().getInfo());
        }

        adminClient.realms().realm(realmNameWithSpaces).remove();

        Assert.assertNames(adminClient.realms().findAll(), "master", managedRealm.getName());
    }

    @Test
    public void createRealmRejectReservedCharOrEmptyName() {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm("new-re;alm");
        assertThrows(BadRequestException.class, () -> adminClient.realms().create(rep));
        rep.setRealm("");
        assertThrows(BadRequestException.class, () -> adminClient.realms().create(rep));
        rep.setRealm("new-realm");
        rep.setId("invalid;id");
        assertThrows(BadRequestException.class, () -> adminClient.realms().create(rep));
    }

    @Test
    public void createRealmCheckDefaultPasswordPolicy() {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm("new-realm");

        adminClient.realms().create(rep);

        assertNull(adminClient.realm("new-realm").toRepresentation().getPasswordPolicy());

        adminClient.realms().realm("new-realm").remove();

        rep.setPasswordPolicy("length(8)");

        adminClient.realms().create(rep);

        assertEquals("length(8)", adminClient.realm("new-realm").toRepresentation().getPasswordPolicy());

        adminClient.realms().realm("new-realm").remove();
    }

    @Test
    public void createRealmFromJson() throws IOException {
        RealmRepresentation rep = JsonSerialization.readValue(getClass().getResourceAsStream("testrealm.json"), RealmRepresentation.class);
        adminClient.realms().create(rep);

        RealmRepresentation created = adminClient.realms().realm("admin-test-1").toRepresentation();
        assertRealm(rep, created);

        adminClient.realms().realm("admin-test-1").remove();
    }

    //KEYCLOAK-6146
    @Test
    public void createRealmWithPasswordPolicyFromJsonWithInvalidPasswords() throws IOException {
        RealmRepresentation rep = JsonSerialization.readValue(getClass().getResourceAsStream("testrealm-keycloak-6146-error.json"), RealmRepresentation.class);
        //try to create realm with password policies and users with plain-text passwords what doesn't met the policies
        try {
            adminClient.realms().create(rep);
        } catch (BadRequestException ex) {
            //ensure the realm was not created
            Assertions.assertThrows(NotFoundException.class, () -> {
                adminClient.realms().realm("secure-app").toRepresentation();
            });
        }
    }

    //KEYCLOAK-6146
    @Test
    public void createRealmWithPasswordPolicyFromJsonWithValidPasswords() throws IOException {
        RealmRepresentation rep = JsonSerialization.readValue(RealmCreateTest.class.getResourceAsStream("testrealm-keycloak-6146.json"), RealmRepresentation.class);
        adminClient.realms().create(rep);
        assertRealm(rep, adminClient.realm(rep.getRealm()).toRepresentation());
        adminClient.realms().realm(rep.getRealm()).remove();
    }

    private void assertRealm(RealmRepresentation realm, RealmRepresentation storedRealm) {
        if (realm.getRealm() != null) {
            assertEquals(realm.getRealm(), storedRealm.getRealm());
        }
        if (realm.isEnabled() != null) assertEquals(realm.isEnabled(), storedRealm.isEnabled());
        if (realm.isBruteForceProtected() != null) assertEquals(realm.isBruteForceProtected(), storedRealm.isBruteForceProtected());
        if (realm.getMaxFailureWaitSeconds() != null) assertEquals(realm.getMaxFailureWaitSeconds(), storedRealm.getMaxFailureWaitSeconds());
        if (realm.getMinimumQuickLoginWaitSeconds() != null) assertEquals(realm.getMinimumQuickLoginWaitSeconds(), storedRealm.getMinimumQuickLoginWaitSeconds());
        if (realm.getWaitIncrementSeconds() != null) assertEquals(realm.getWaitIncrementSeconds(), storedRealm.getWaitIncrementSeconds());
        if (realm.getQuickLoginCheckMilliSeconds() != null) assertEquals(realm.getQuickLoginCheckMilliSeconds(), storedRealm.getQuickLoginCheckMilliSeconds());
        if (realm.getMaxDeltaTimeSeconds() != null) assertEquals(realm.getMaxDeltaTimeSeconds(), storedRealm.getMaxDeltaTimeSeconds());
        if (realm.getFailureFactor() != null) assertEquals(realm.getFailureFactor(), storedRealm.getFailureFactor());
        if (realm.isRegistrationAllowed() != null) assertEquals(realm.isRegistrationAllowed(), storedRealm.isRegistrationAllowed());
        if (realm.isRegistrationEmailAsUsername() != null) assertEquals(realm.isRegistrationEmailAsUsername(), storedRealm.isRegistrationEmailAsUsername());
        if (realm.isRememberMe() != null) assertEquals(realm.isRememberMe(), storedRealm.isRememberMe());
        if (realm.isVerifyEmail() != null) assertEquals(realm.isVerifyEmail(), storedRealm.isVerifyEmail());
        if (realm.isLoginWithEmailAllowed() != null) assertEquals(realm.isLoginWithEmailAllowed(), storedRealm.isLoginWithEmailAllowed());
        if (realm.isDuplicateEmailsAllowed() != null) assertEquals(realm.isDuplicateEmailsAllowed(), storedRealm.isDuplicateEmailsAllowed());
        if (realm.isResetPasswordAllowed() != null) assertEquals(realm.isResetPasswordAllowed(), storedRealm.isResetPasswordAllowed());
        if (realm.isEditUsernameAllowed() != null) assertEquals(realm.isEditUsernameAllowed(), storedRealm.isEditUsernameAllowed());
        if (realm.getSslRequired() != null) assertEquals(realm.getSslRequired(), storedRealm.getSslRequired());
        if (realm.getAccessCodeLifespan() != null) assertEquals(realm.getAccessCodeLifespan(), storedRealm.getAccessCodeLifespan());
        if (realm.getAccessCodeLifespanUserAction() != null)
            assertEquals(realm.getAccessCodeLifespanUserAction(), storedRealm.getAccessCodeLifespanUserAction());
        if (realm.getActionTokenGeneratedByAdminLifespan() != null)
            assertEquals(realm.getActionTokenGeneratedByAdminLifespan(), storedRealm.getActionTokenGeneratedByAdminLifespan());
        if (realm.getActionTokenGeneratedByUserLifespan() != null)
            assertEquals(realm.getActionTokenGeneratedByUserLifespan(), storedRealm.getActionTokenGeneratedByUserLifespan());
        else
            assertEquals(realm.getAccessCodeLifespanUserAction(), storedRealm.getActionTokenGeneratedByUserLifespan());
        if (realm.getNotBefore() != null) assertEquals(realm.getNotBefore(), storedRealm.getNotBefore());
        if (realm.getAccessTokenLifespan() != null) assertEquals(realm.getAccessTokenLifespan(), storedRealm.getAccessTokenLifespan());
        if (realm.getAccessTokenLifespanForImplicitFlow() != null) assertEquals(realm.getAccessTokenLifespanForImplicitFlow(), storedRealm.getAccessTokenLifespanForImplicitFlow());
        if (realm.getSsoSessionIdleTimeout() != null) assertEquals(realm.getSsoSessionIdleTimeout(), storedRealm.getSsoSessionIdleTimeout());
        if (realm.getSsoSessionMaxLifespan() != null) assertEquals(realm.getSsoSessionMaxLifespan(), storedRealm.getSsoSessionMaxLifespan());
        if (realm.getSsoSessionIdleTimeoutRememberMe() != null) Assert.assertEquals(realm.getSsoSessionIdleTimeoutRememberMe(), storedRealm.getSsoSessionIdleTimeoutRememberMe());
        if (realm.getSsoSessionMaxLifespanRememberMe() != null) Assert.assertEquals(realm.getSsoSessionMaxLifespanRememberMe(), storedRealm.getSsoSessionMaxLifespanRememberMe());
        if (realm.getClientSessionIdleTimeout() != null)
            Assertions.assertEquals(realm.getClientSessionIdleTimeout(), storedRealm.getClientSessionIdleTimeout());
        if (realm.getClientSessionMaxLifespan() != null)
            Assertions.assertEquals(realm.getClientSessionMaxLifespan(), storedRealm.getClientSessionMaxLifespan());
        if (realm.getClientOfflineSessionIdleTimeout() != null)
            Assertions.assertEquals(realm.getClientOfflineSessionIdleTimeout(), storedRealm.getClientOfflineSessionIdleTimeout());
        if (realm.getClientOfflineSessionMaxLifespan() != null)
            Assertions.assertEquals(realm.getClientOfflineSessionMaxLifespan(), storedRealm.getClientOfflineSessionMaxLifespan());
        if (realm.getRequiredCredentials() != null) {
            assertNotNull(storedRealm.getRequiredCredentials());
            for (String cred : realm.getRequiredCredentials()) {
                assertTrue(storedRealm.getRequiredCredentials().contains(cred));
            }
        }
        if (realm.getLoginTheme() != null) assertEquals(realm.getLoginTheme(), storedRealm.getLoginTheme());
        if (realm.getAccountTheme() != null) assertEquals(realm.getAccountTheme(), storedRealm.getAccountTheme());
        if (realm.getAdminTheme() != null) assertEquals(realm.getAdminTheme(), storedRealm.getAdminTheme());
        if (realm.getEmailTheme() != null) assertEquals(realm.getEmailTheme(), storedRealm.getEmailTheme());

        if (realm.getPasswordPolicy() != null) assertEquals(realm.getPasswordPolicy(), storedRealm.getPasswordPolicy());

        if (realm.getSmtpServer() != null) {
            assertEquals(realm.getSmtpServer(), storedRealm.getSmtpServer());
        }

        if (realm.getBrowserSecurityHeaders() != null) {
            assertEquals(realm.getBrowserSecurityHeaders(), storedRealm.getBrowserSecurityHeaders());
        }

        if (realm.getAttributes() != null) {
            HashMap<String, String> attributes = new HashMap<>();
            attributes.putAll(storedRealm.getAttributes());
            attributes.entrySet().retainAll(realm.getAttributes().entrySet());
            assertEquals(realm.getAttributes(), attributes);
        }

        if (realm.isUserManagedAccessAllowed() != null) assertEquals(realm.isUserManagedAccessAllowed(), storedRealm.isUserManagedAccessAllowed());
    }
}
