package org.keycloak.model.test;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ModelToRepresentation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ModelTest extends AbstractModelTest {

    @Test
    public void importExportRealm() {
        RealmModel realm = realmManager.createRealm("original");
        realm.setRegistrationAllowed(true);
        realm.setResetPasswordAllowed(true);
        realm.setSocial(true);
        realm.setSslNotRequired(true);
        realm.setVerifyEmail(true);
        realm.setAccessTokenLifespan(1000);
        realm.setPasswordPolicy(new PasswordPolicy("length"));
        realm.setAccessCodeLifespan(1001);
        realm.setAccessCodeLifespanUserAction(1002);
        realm.setPublicKeyPem("0234234");
        realm.setPrivateKeyPem("1234234");
        realm.addDefaultRole("default-role");

        HashMap<String, String> smtp = new HashMap<String,String>();
        smtp.put("from", "auto@keycloak");
        smtp.put("hostname", "localhost");
        realm.setSmtpConfig(smtp);

        HashMap<String, String> social = new HashMap<String,String>();
        social.put("google.key", "1234");
        social.put("google.secret", "5678");
        realm.setSocialConfig(social);

        RealmModel persisted = realmManager.getRealm(realm.getId());
        assertEquals(realm, persisted);

        RealmModel copy = importExport(realm, "copy");
        assertEquals(realm, copy);
    }

    public static void assertEquals(RealmModel expected, RealmModel actual) {
        Assert.assertEquals(expected.isUpdateProfileOnInitialSocialLogin(),
                actual.isUpdateProfileOnInitialSocialLogin());
        Assert.assertEquals(expected.isRegistrationAllowed(), actual.isRegistrationAllowed());
        Assert.assertEquals(expected.isResetPasswordAllowed(), actual.isResetPasswordAllowed());
        Assert.assertEquals(expected.isSocial(), actual.isSocial());
        Assert.assertEquals(expected.isSslNotRequired(), actual.isSslNotRequired());
        Assert.assertEquals(expected.isVerifyEmail(), actual.isVerifyEmail());
        Assert.assertEquals(expected.getAccessTokenLifespan(), actual.getAccessTokenLifespan());

        Assert.assertEquals(expected.getAccessCodeLifespan(), actual.getAccessCodeLifespan());
        Assert.assertEquals(expected.getAccessCodeLifespanUserAction(), actual.getAccessCodeLifespanUserAction());
        Assert.assertEquals(expected.getPublicKeyPem(), actual.getPublicKeyPem());
        Assert.assertEquals(expected.getPrivateKeyPem(), actual.getPrivateKeyPem());

        Assert.assertEquals(expected.getDefaultRoles(), actual.getDefaultRoles());

        Assert.assertEquals(expected.getSmtpConfig(), actual.getSmtpConfig());
        Assert.assertEquals(expected.getSocialConfig(), actual.getSocialConfig());
    }

    private RealmModel importExport(RealmModel src, String copyName) {
        RealmRepresentation representation = ModelToRepresentation.toRepresentation(src);
        RealmModel copy = realmManager.createRealm(copyName);
        realmManager.importRealm(representation, copy);
        return realmManager.getRealm(copy.getId());
    }

}
