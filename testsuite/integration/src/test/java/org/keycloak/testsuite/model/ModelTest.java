package org.keycloak.testsuite.model;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.enums.SslRequired;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class ModelTest extends AbstractModelTest {

    @Test
    public void importExportRealm() {
        RealmModel realm = realmManager.createRealm("original");
        realm.setRegistrationAllowed(true);
        realm.setRegistrationEmailAsUsername(true);
        realm.setResetPasswordAllowed(true);
        realm.setSslRequired(SslRequired.EXTERNAL);
        realm.setVerifyEmail(true);
        realm.setAccessTokenLifespan(1000);
        realm.setPasswordPolicy(new PasswordPolicy("length"));
        realm.setAccessCodeLifespan(1001);
        realm.setAccessCodeLifespanUserAction(1002);
        KeycloakModelUtils.generateRealmKeys(realm);
        realm.addDefaultRole("default-role");

        HashMap<String, String> smtp = new HashMap<String,String>();
        smtp.put("from", "auto@keycloak");
        smtp.put("hostname", "localhost");
        realm.setSmtpConfig(smtp);

        realm.setDefaultLocale("en");
        realm.setAccessCodeLifespanLogin(100);
        realm.setInternationalizationEnabled(true);
        realm.setRegistrationEmailAsUsername(true);
        realm.setSupportedLocales(new HashSet<String>(Arrays.asList("en", "cz")));
        realm.setEventsListeners(new HashSet<String>(Arrays.asList("jpa", "mongo", "foo")));
        realm.setEventsExpiration(200);
        realm.setEventsEnabled(true);

        RealmModel persisted = realmManager.getRealm(realm.getId());
        assertEquals(realm, persisted);

        RealmModel copy = importExport(realm, "copy");
        assertEquals(realm, copy);
    }

    public static void assertEquals(RealmModel expected, RealmModel actual) {
        Assert.assertEquals(expected.isRegistrationAllowed(), actual.isRegistrationAllowed());
        Assert.assertEquals(expected.isRegistrationEmailAsUsername(), actual.isRegistrationEmailAsUsername());
        Assert.assertEquals(expected.isResetPasswordAllowed(), actual.isResetPasswordAllowed());
        Assert.assertEquals(expected.getSslRequired(), actual.getSslRequired());
        Assert.assertEquals(expected.isVerifyEmail(), actual.isVerifyEmail());
        Assert.assertEquals(expected.getAccessTokenLifespan(), actual.getAccessTokenLifespan());

        Assert.assertEquals(expected.getAccessCodeLifespan(), actual.getAccessCodeLifespan());
        Assert.assertEquals(expected.getAccessCodeLifespanUserAction(), actual.getAccessCodeLifespanUserAction());
        Assert.assertEquals(expected.getPublicKeyPem(), actual.getPublicKeyPem());
        Assert.assertEquals(expected.getPrivateKeyPem(), actual.getPrivateKeyPem());

        Assert.assertEquals(expected.getDefaultRoles(), actual.getDefaultRoles());

        Assert.assertEquals(expected.getSmtpConfig(), actual.getSmtpConfig());

        Assert.assertEquals(expected.getDefaultLocale(), actual.getDefaultLocale());
        Assert.assertEquals(expected.getAccessCodeLifespanLogin(), actual.getAccessCodeLifespanLogin());
        Assert.assertEquals(expected.isInternationalizationEnabled(), actual.isInternationalizationEnabled());
        Assert.assertEquals(expected.isRegistrationEmailAsUsername(), actual.isRegistrationEmailAsUsername());
        Assert.assertEquals(expected.getSupportedLocales(), actual.getSupportedLocales());
        Assert.assertEquals(expected.getEventsListeners(), actual.getEventsListeners());
        Assert.assertEquals(expected.getEventsExpiration(), actual.getEventsExpiration());
        Assert.assertEquals(expected.isEventsEnabled(), actual.isEventsEnabled());
    }

    private RealmModel importExport(RealmModel src, String copyName) {
        RealmRepresentation representation = ModelToRepresentation.toRepresentation(src, true);
        representation.setRealm(copyName);
        representation.setId(copyName);
        RealmModel copy = realmManager.importRealm(representation);
        return realmManager.getRealm(copy.getId());
    }

}
