package org.keycloak.tests.actions;

import java.util.List;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.PasswordCredentialProvider;
import org.keycloak.credential.PasswordCredentialProviderFactory;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServer;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordUpdatePage;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.tests.providers.federation.UserMapStorageFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class RequiredActionUpdatePasswordFederatedTest {

    private static final String USERNAME = "federated-update-password";
    private static final String FEDERATED_PASSWORD = "Federated-password1";
    private static final String LOCAL_PASSWORD = "Local-password1";
    private static final String UPDATED_PASSWORD = "Federated-password2";

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectEvents
    Events events;

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    LoginPasswordUpdatePage updatePasswordPage;

    // The user storage provider handles the password update, so the password stored in the Keycloak DB is left
    // untouched and its ID must not be reported on the UPDATE_CREDENTIAL event
    @Test
    public void updatePasswordHandledByUserStorageDoesNotReportCredentialId() {
        addUserStorageProvider();

        UserResource user = addFederatedUser();
        String userId = user.toRepresentation().getId();

        // updateCredential is always answered by the provider, so the password of the user is stored there only
        resetPassword(user, FEDERATED_PASSWORD);
        Assertions.assertTrue(user.credentials().isEmpty(), "Password should not be stored in the Keycloak DB");

        // Store a password in the Keycloak DB too. It has to be written directly, as going through the credential
        // manager would hand the write to the provider again
        storeLocalPassword();
        String localPassword = localPasswordFingerprint();

        UserRepresentation userRep = user.toRepresentation();
        userRep.setRequiredActions(List.of(UserModel.RequiredAction.UPDATE_PASSWORD.name()));
        user.update(userRep);

        events.clear();

        oauth.openLoginForm();
        // The provider validates passwords itself and is asked before the local store, so the federated password is
        // the one the user authenticates with
        loginPage.fillLogin(USERNAME, FEDERATED_PASSWORD);
        loginPage.submit();

        updatePasswordPage.assertCurrent();
        updatePasswordPage.changePassword(UPDATED_PASSWORD, UPDATED_PASSWORD);

        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        EventAssertion.assertSuccess(events.poll()).type(EventType.UPDATE_PASSWORD)
                .details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).userId(userId);
        EventAssertion.assertSuccess(events.poll()).type(EventType.UPDATE_CREDENTIAL)
                .details(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).userId(userId)
                .withoutDetails(Details.CREDENTIAL_ID);

        // Assert the locally stored password is still the very same one as before the update
        assertEquals(localPassword, localPasswordFingerprint());
    }

    private void addUserStorageProvider() {
        ComponentRepresentation provider = new ComponentRepresentation();
        provider.setName("memory");
        provider.setProviderId(UserMapStorageFactory.PROVIDER_ID);
        provider.setProviderType(UserStorageProvider.class.getName());
        provider.setConfig(new MultivaluedHashMap<>());
        provider.getConfig().putSingle("priority", "0");
        provider.getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.WRITABLE.name());

        String providerId = ApiUtil.getCreatedId(managedRealm.admin().components().add(provider));
        managedRealm.cleanup().add(realm -> realm.components().component(providerId).remove());
    }

    private UserResource addFederatedUser() {
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername(USERNAME);
        userRep.setEmail(USERNAME + "@email.cz");
        userRep.setFirstName("firstName");
        userRep.setLastName("lastName");
        userRep.setEnabled(true);

        // No cleanup for the user, it is removed along with the provider it is federated from
        String userId = ApiUtil.getCreatedId(managedRealm.admin().users().create(userRep));

        UserResource user = managedRealm.admin().users().get(userId);
        Assertions.assertNotNull(user.toRepresentation().getFederationLink(), "User should be federated");
        return user;
    }

    private void resetPassword(UserResource user, String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        user.resetPassword(credential);
    }

    private void storeLocalPassword() {
        runOnServer.run(ServerCode.storeLocalPassword());
    }

    // The ID, the creation date and the secret of the locally stored password, used to assert the local row was
    // left untouched by the update
    private String localPasswordFingerprint() {
        return runOnServer.fetchString(ServerCode.localPasswordFingerprint());
    }

    // The class capturing a lambda is loaded on the server to run it, so the code running there is kept out of the
    // test class, which references admin client classes that are not available on the server
    public static class ServerCode {

        public static RunOnServer storeLocalPassword() {
            return session -> {
                RealmModel realm = session.getContext().getRealm();
                UserModel user = session.users().getUserByUsername(realm, USERNAME);
                PasswordCredentialProvider passwordProvider = (PasswordCredentialProvider) session
                        .getProvider(CredentialProvider.class, PasswordCredentialProviderFactory.PROVIDER_ID);
                passwordProvider.createCredential(realm, user, LOCAL_PASSWORD);
            };
        }

        public static FetchOnServer localPasswordFingerprint() {
            return session -> {
                RealmModel realm = session.getContext().getRealm();
                UserModel user = session.users().getUserByUsername(realm, USERNAME);
                CredentialModel password = user.credentialManager()
                        .getStoredCredentialsByTypeStream(PasswordCredentialModel.TYPE)
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("Password credential is not stored in the Keycloak DB"));
                return password.getId() + "|" + password.getCreatedDate() + "|" + password.getSecretData();
            };
        }
    }
}
