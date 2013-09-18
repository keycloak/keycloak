package org.keycloak.services.managers;

import java.util.UUID;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.AuthenticationManager.AuthenticationStatus;
import org.keycloak.services.models.KeycloakSession;
import org.keycloak.services.models.KeycloakSessionFactory;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.UserCredentialModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.models.UserModel.RequiredAction;
import org.keycloak.services.models.UserModel.Status;
import org.keycloak.services.resources.KeycloakApplication;
import org.picketlink.idm.credential.util.TimeBasedOTP;

public class AuthenticationManagerTest {

    private RealmManager adapter;
    private AuthenticationManager am;
    private KeycloakSessionFactory factory;
    private MultivaluedMap<String, String> formData;
    private KeycloakSession identitySession;
    private TimeBasedOTP otp;
    private RealmModel realm;
    private UserModel user;

    @After
    public void after() throws Exception {
        identitySession.getTransaction().commit();
        identitySession.close();
        factory.close();
    }

    @Test
    public void authForm() {
        AuthenticationStatus status = am.authenticateForm(realm, user, formData);
        Assert.assertEquals(AuthenticationStatus.SUCCESS, status);
    }

    @Test
    public void authFormInvalidPassword() {
        formData.remove(CredentialRepresentation.PASSWORD);
        formData.add(CredentialRepresentation.PASSWORD, "invalid");

        AuthenticationStatus status = am.authenticateForm(realm, user, formData);
        Assert.assertEquals(AuthenticationStatus.INVALID_CREDENTIALS, status);
    }

    @Test
    public void authFormMissingPassword() {
        formData.remove(CredentialRepresentation.PASSWORD);

        AuthenticationStatus status = am.authenticateForm(realm, user, formData);
        Assert.assertEquals(AuthenticationStatus.MISSING_PASSWORD, status);
    }

    @Test
    public void authFormRequiredAction() {
        realm.addRequiredCredential(CredentialRepresentation.TOTP);
        user.addRequiredAction(RequiredAction.CONFIGURE_TOTP);
        user.setStatus(Status.ACTIONS_REQUIRED);
        
        AuthenticationStatus status = am.authenticateForm(realm, user, formData);
        Assert.assertEquals(AuthenticationStatus.ACTIONS_REQUIRED, status);
    }

    @Test
    public void authFormUserDisabled() {
        user.setStatus(Status.DISABLED);

        AuthenticationStatus status = am.authenticateForm(realm, user, formData);
        Assert.assertEquals(AuthenticationStatus.ACCOUNT_DISABLED, status);
    }

    @Test
    public void authFormUserRequiredActions() {
        user.setStatus(Status.ACTIONS_REQUIRED);

        AuthenticationStatus status = am.authenticateForm(realm, user, formData);
        Assert.assertEquals(AuthenticationStatus.ACTIONS_REQUIRED, status);
    }

    @Test
    public void authFormWithTotp() {
        realm.addRequiredCredential(CredentialRepresentation.TOTP);
        
        String totpSecret = UUID.randomUUID().toString();

        UserCredentialModel credential = new UserCredentialModel();
        credential.setType(CredentialRepresentation.TOTP);
        credential.setValue(totpSecret);

        realm.updateCredential(user, credential);

        user.setTotp(true);

        String token = otp.generate(totpSecret);

        formData.add(CredentialRepresentation.TOTP, token);

        AuthenticationStatus status = am.authenticateForm(realm, user, formData);
        Assert.assertEquals(AuthenticationStatus.SUCCESS, status);
    }

    @Test
    public void authFormWithTotpInvalidPassword() {
        authFormWithTotp();

        formData.remove(CredentialRepresentation.PASSWORD);
        formData.add(CredentialRepresentation.PASSWORD, "invalid");

        AuthenticationStatus status = am.authenticateForm(realm, user, formData);
        Assert.assertEquals(AuthenticationStatus.INVALID_CREDENTIALS, status);
    }

    @Test
    public void authFormWithTotpInvalidTotp() {
        authFormWithTotp();

        formData.remove(CredentialRepresentation.TOTP);
        formData.add(CredentialRepresentation.TOTP, "invalid");

        AuthenticationStatus status = am.authenticateForm(realm, user, formData);
        Assert.assertEquals(AuthenticationStatus.INVALID_CREDENTIALS, status);
    }

    @Test
    public void authFormWithTotpMissingTotp() {
        authFormWithTotp();

        formData.remove(CredentialRepresentation.TOTP);

        AuthenticationStatus status = am.authenticateForm(realm, user, formData);
        Assert.assertEquals(AuthenticationStatus.MISSING_TOTP, status);
    }

    @Before
    public void before() throws Exception {
        factory = KeycloakApplication.buildSessionFactory();
        identitySession = factory.createSession();
        identitySession.getTransaction().begin();
        adapter = new RealmManager(identitySession);

        realm = adapter.createRealm("Test");
        realm.setAccessCodeLifespan(100);
        realm.setCookieLoginAllowed(true);
        realm.setEnabled(true);
        realm.setName("Test");
        realm.setPrivateKeyPem("0234234");
        realm.setPublicKeyPem("0234234");
        realm.setTokenLifespan(1000);
        realm.addRequiredCredential(CredentialRepresentation.PASSWORD);

        am = new AuthenticationManager();

        user = realm.addUser("test");

        UserCredentialModel credential = new UserCredentialModel();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue("password");

        realm.updateCredential(user, credential);

        formData = new MultivaluedHashMap<String, String>();
        formData.add(CredentialRepresentation.PASSWORD, "password");

        otp = new TimeBasedOTP();
    }

}
