package org.keycloak.model.test;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.AuthenticationProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationManager.AuthenticationStatus;

import javax.ws.rs.core.MultivaluedMap;

import java.util.Arrays;
import java.util.UUID;

public class AuthenticationManagerTest extends AbstractModelTest {

    private AuthenticationManager am;
    private MultivaluedMap<String, String> formData;
    private TimeBasedOTP otp;
    private RealmModel realm;
    private UserModel user;

    @Test
    public void authForm() {
        AuthenticationStatus status = am.authenticateForm(null, realm, formData);
        Assert.assertEquals(AuthenticationStatus.SUCCESS, status);
    }

    @Test
    public void authFormInvalidPassword() {
        formData.remove(CredentialRepresentation.PASSWORD);
        formData.add(CredentialRepresentation.PASSWORD, "invalid");

        AuthenticationStatus status = am.authenticateForm(null, realm, formData);
        Assert.assertEquals(AuthenticationStatus.INVALID_CREDENTIALS, status);
    }

    @Test
    public void authFormMissingUsername() {
        formData.remove("username");

        AuthenticationStatus status = am.authenticateForm(null, realm, formData);
        Assert.assertEquals(AuthenticationStatus.INVALID_USER, status);
    }

    @Test
    public void authFormMissingPassword() {
        formData.remove(CredentialRepresentation.PASSWORD);

        AuthenticationStatus status = am.authenticateForm(null, realm, formData);
        Assert.assertEquals(AuthenticationStatus.MISSING_PASSWORD, status);
    }

    @Test
    public void authFormRequiredAction() {
        realm.addRequiredCredential(CredentialRepresentation.TOTP);
        user.addRequiredAction(RequiredAction.CONFIGURE_TOTP);
        
        AuthenticationStatus status = am.authenticateForm(null, realm, formData);
        Assert.assertEquals(AuthenticationStatus.ACTIONS_REQUIRED, status);
    }

    @Test
    public void authFormUserDisabled() {
        user.setEnabled(false);

        AuthenticationStatus status = am.authenticateForm(null, realm, formData);
        Assert.assertEquals(AuthenticationStatus.ACCOUNT_DISABLED, status);
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

        AuthenticationStatus status = am.authenticateForm(null, realm, formData);
        Assert.assertEquals(AuthenticationStatus.SUCCESS, status);
    }

    @Test
    public void authFormWithTotpInvalidPassword() {
        authFormWithTotp();

        formData.remove(CredentialRepresentation.PASSWORD);
        formData.add(CredentialRepresentation.PASSWORD, "invalid");

        AuthenticationStatus status = am.authenticateForm(null, realm, formData);
        Assert.assertEquals(AuthenticationStatus.INVALID_CREDENTIALS, status);
    }

    @Test
    public void authFormWithTotpInvalidTotp() {
        authFormWithTotp();

        formData.remove(CredentialRepresentation.TOTP);
        formData.add(CredentialRepresentation.TOTP, "invalid");

        AuthenticationStatus status = am.authenticateForm(null, realm, formData);
        Assert.assertEquals(AuthenticationStatus.INVALID_CREDENTIALS, status);
    }

    @Test
    public void authFormWithTotpMissingTotp() {
        authFormWithTotp();

        formData.remove(CredentialRepresentation.TOTP);

        AuthenticationStatus status = am.authenticateForm(null, realm, formData);
        Assert.assertEquals(AuthenticationStatus.MISSING_TOTP, status);
    }

    @Before
    @Override
    public void before() throws Exception {
        super.before();
        realm = realmManager.createRealm("Test");
        realm.setAccessCodeLifespan(100);
        realm.setEnabled(true);
        realm.setName("Test");
        realm.setPrivateKeyPem("0234234");
        realm.setPublicKeyPem("0234234");
        realm.setAccessTokenLifespan(1000);
        realm.addRequiredCredential(CredentialRepresentation.PASSWORD);
        realm.setAuthenticationProviders(Arrays.asList(AuthenticationProviderModel.DEFAULT_PROVIDER));

        am = new AuthenticationManager(providerSession);

        user = realm.addUser("test");
        user.setEnabled(true);

        UserCredentialModel credential = new UserCredentialModel();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue("password");

        realm.updateCredential(user, credential);

        formData = new MultivaluedMapImpl<String, String>();
        formData.add("username", "test");
        formData.add(CredentialRepresentation.PASSWORD, "password");

        otp = new TimeBasedOTP();
    }

}
