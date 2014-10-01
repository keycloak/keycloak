package org.keycloak.testsuite.model;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.ClientConnection;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.PasswordToken;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationManager.AuthenticationStatus;
import org.keycloak.services.managers.BruteForceProtector;

import javax.ws.rs.core.MultivaluedMap;
import java.util.UUID;

public class AuthenticationManagerTest extends AbstractModelTest {

    private AuthenticationManager am;
    private MultivaluedMap<String, String> formData;
    private TimeBasedOTP otp;
    private RealmModel realm;
    private UserModel user;
    private BruteForceProtector protector;
    private ClientConnection dummyConnection = new ClientConnection() {
        @Override
        public String getRemoteAddr() {
            return "127.0.0.1";

        }

        @Override
        public String getRemoteHost() {
            return "localhost";

        }

        @Override
        public int getReportPort() {
            return 8080;
        }
    };

    @Test
    public void authForm() {
        AuthenticationStatus status = am.authenticateForm(session, dummyConnection, realm, formData);
        Assert.assertEquals(AuthenticationStatus.SUCCESS, status);
    }

    @Test
    public void authFormInvalidPassword() {
        formData.remove(CredentialRepresentation.PASSWORD);
        formData.add(CredentialRepresentation.PASSWORD, "invalid");

        AuthenticationStatus status = am.authenticateForm(session, dummyConnection, realm, formData);
        Assert.assertEquals(AuthenticationStatus.INVALID_CREDENTIALS, status);
    }

    @Test
    public void authFormMissingUsername() {
        formData.remove("username");

        AuthenticationStatus status = am.authenticateForm(session, dummyConnection, realm, formData);
        Assert.assertEquals(AuthenticationStatus.INVALID_USER, status);
    }

    @Test
    public void authFormMissingPassword() {
        formData.remove(CredentialRepresentation.PASSWORD);

        AuthenticationStatus status = am.authenticateForm(session, dummyConnection, realm, formData);
        Assert.assertEquals(AuthenticationStatus.MISSING_PASSWORD, status);
    }

    @Test
    public void authFormRequiredAction() {
        realm.addRequiredCredential(CredentialRepresentation.TOTP);
        user.addRequiredAction(RequiredAction.CONFIGURE_TOTP);

        AuthenticationStatus status = am.authenticateForm(session, dummyConnection, realm, formData);
        Assert.assertEquals(AuthenticationStatus.ACTIONS_REQUIRED, status);
    }

    @Test
    public void authFormUserDisabled() {
        user.setEnabled(false);

        AuthenticationStatus status = am.authenticateForm(session, dummyConnection, realm, formData);
        Assert.assertEquals(AuthenticationStatus.ACCOUNT_DISABLED, status);
    }

    @Test
    public void authFormWithTotp() {
        realm.addRequiredCredential(CredentialRepresentation.TOTP);

        String totpSecret = UUID.randomUUID().toString();

        UserCredentialModel credential = new UserCredentialModel();
        credential.setType(CredentialRepresentation.TOTP);
        credential.setValue(totpSecret);

        user.updateCredential(credential);

        user.setTotp(true);

        String token = otp.generate(totpSecret);

        formData.add(CredentialRepresentation.TOTP, token);

        AuthenticationStatus status = am.authenticateForm(session, dummyConnection, realm, formData);
        Assert.assertEquals(AuthenticationStatus.SUCCESS, status);
    }

    @Test
    public void authFormWithTotpInvalidPassword() {
        authFormWithTotp();

        formData.remove(CredentialRepresentation.PASSWORD);
        formData.add(CredentialRepresentation.PASSWORD, "invalid");

        AuthenticationStatus status = am.authenticateForm(session, dummyConnection, realm, formData);
        Assert.assertEquals(AuthenticationStatus.INVALID_CREDENTIALS, status);
    }

    @Test
    public void authFormWithTotpMissingPassword() {
        authFormWithTotp();

        formData.remove(CredentialRepresentation.PASSWORD);

        AuthenticationStatus status = am.authenticateForm(session, dummyConnection, realm, formData);
        Assert.assertEquals(AuthenticationStatus.MISSING_PASSWORD, status);
    }

    @Test
    public void authFormWithTotpInvalidTotp() {
        authFormWithTotp();

        formData.remove(CredentialRepresentation.TOTP);
        formData.add(CredentialRepresentation.TOTP, "invalid");

        AuthenticationStatus status = am.authenticateForm(session, dummyConnection, realm, formData);
        Assert.assertEquals(AuthenticationStatus.INVALID_CREDENTIALS, status);
    }

    @Test
    public void authFormWithTotpMissingTotp() {
        authFormWithTotp();

        formData.remove(CredentialRepresentation.TOTP);

        AuthenticationStatus status = am.authenticateForm(session, dummyConnection, realm, formData);
        Assert.assertEquals(AuthenticationStatus.MISSING_TOTP, status);
    }

    @Test
    public void authFormWithTotpPasswordToken() {
        realm.addRequiredCredential(CredentialRepresentation.TOTP);

        String totpSecret = UUID.randomUUID().toString();

        UserCredentialModel credential = new UserCredentialModel();
        credential.setType(CredentialRepresentation.TOTP);
        credential.setValue(totpSecret);

        user.updateCredential(credential);

        user.setTotp(true);

        String token = otp.generate(totpSecret);

        formData.add(CredentialRepresentation.TOTP, token);
        formData.remove(CredentialRepresentation.PASSWORD);

        String passwordToken = new JWSBuilder().jsonContent(new PasswordToken(realm.getName(), user.getId())).rsa256(realm.getPrivateKey());
        formData.add(CredentialRepresentation.PASSWORD_TOKEN, passwordToken);

        AuthenticationStatus status = am.authenticateForm(session, dummyConnection, realm, formData);
        Assert.assertEquals(AuthenticationStatus.SUCCESS, status);
    }

    @Test
    public void authFormWithTotpPasswordTokenInvalidKey() {
        authFormWithTotpPasswordToken();

        formData.remove(CredentialRepresentation.PASSWORD_TOKEN);
        String passwordToken = new JWSBuilder().jsonContent(new PasswordToken(realm.getName(), user.getId())).rsa256(realm.getPrivateKey());
        formData.add(CredentialRepresentation.PASSWORD_TOKEN, passwordToken);

        KeycloakModelUtils.generateRealmKeys(realm);

        AuthenticationStatus status = am.authenticateForm(session, dummyConnection, realm, formData);
        Assert.assertEquals(AuthenticationStatus.INVALID_CREDENTIALS, status);
    }

    @Test
    public void authFormWithTotpPasswordTokenInvalidRealm() {
        authFormWithTotpPasswordToken();

        formData.remove(CredentialRepresentation.PASSWORD_TOKEN);
        String passwordToken = new JWSBuilder().jsonContent(new PasswordToken("invalid", user.getId())).rsa256(realm.getPrivateKey());
        formData.add(CredentialRepresentation.PASSWORD_TOKEN, passwordToken);

        AuthenticationStatus status = am.authenticateForm(session, dummyConnection, realm, formData);
        Assert.assertEquals(AuthenticationStatus.INVALID_CREDENTIALS, status);
    }

    @Test
    public void authFormWithTotpPasswordTokenInvalidUser() {
        authFormWithTotpPasswordToken();

        formData.remove(CredentialRepresentation.PASSWORD_TOKEN);
        String passwordToken = new JWSBuilder().jsonContent(new PasswordToken(realm.getName(), "invalid")).rsa256(realm.getPrivateKey());
        formData.add(CredentialRepresentation.PASSWORD_TOKEN, passwordToken);

        AuthenticationStatus status = am.authenticateForm(session, dummyConnection, realm, formData);
        Assert.assertEquals(AuthenticationStatus.INVALID_CREDENTIALS, status);
    }

    @Test
    public void authFormWithTotpPasswordTokenExpired() throws InterruptedException {
        int lifespan = realm.getAccessCodeLifespanUserAction();

        try {
            authFormWithTotpPasswordToken();

            realm.setAccessCodeLifespanUserAction(1);

            formData.remove(CredentialRepresentation.PASSWORD_TOKEN);
            String passwordToken = new JWSBuilder().jsonContent(new PasswordToken(realm.getName(), "invalid")).rsa256(realm.getPrivateKey());
            formData.add(CredentialRepresentation.PASSWORD_TOKEN, passwordToken);

            Thread.sleep(2000);

            AuthenticationStatus status = am.authenticateForm(session, dummyConnection, realm, formData);
            Assert.assertEquals(AuthenticationStatus.INVALID_CREDENTIALS, status);
        } finally {
            realm.setAccessCodeLifespanUserAction(lifespan);
        }
    }

    @Before
    @Override
    public void before() throws Exception {
        super.before();

        realm = realmManager.createRealm("TestAuth");
        realm.setAccessCodeLifespan(100);
        realm.setAccessCodeLifespanUserAction(100);
        realm.setEnabled(true);
        realm.setName("TestAuth");

        KeycloakModelUtils.generateRealmKeys(realm);

        realm.setAccessTokenLifespan(1000);
        realm.addRequiredCredential(CredentialRepresentation.PASSWORD);

        protector = ResteasyProviderFactory.getContextData(BruteForceProtector.class);
        am = new AuthenticationManager(protector);

        user = realmManager.getSession().users().addUser(realm, "test");
        user.setEnabled(true);

        UserCredentialModel credential = new UserCredentialModel();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue("password");

        user.updateCredential(credential);

        formData = new MultivaluedMapImpl<String, String>();
        formData.add("username", "test");
        formData.add(CredentialRepresentation.PASSWORD, "password");

        otp = new TimeBasedOTP();
    }

}
