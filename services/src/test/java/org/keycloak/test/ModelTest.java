package org.keycloak.test;

import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.models.KeycloakSession;
import org.keycloak.services.models.KeycloakSessionFactory;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.resources.KeycloakApplication;

public class ModelTest extends AbstractKeycloakServerTest {
    private KeycloakSessionFactory factory;
    private KeycloakSession identitySession;
    private RealmManager manager;

    @Before
    public void before() throws Exception {
        factory = KeycloakApplication.buildSessionFactory();
        identitySession = factory.createSession();
        identitySession.getTransaction().begin();
        manager = new RealmManager(identitySession);
    }

    @After
    public void after() throws Exception {
        identitySession.getTransaction().commit();
        identitySession.close();
        factory.close();
    }

    @Test
    public void importExportRealm() {
        RealmModel realm = manager.createRealm("original");
        realm.setCookieLoginAllowed(true);
        realm.setRegistrationAllowed(true);
        realm.setResetPasswordAllowed(true);
        realm.setSocial(true);
        realm.setSslNotRequired(true);
        realm.setVerifyEmail(true);
        realm.setTokenLifespan(1000);
        realm.setAccessCodeLifespan(1001);
        realm.setAccessCodeLifespanUserAction(1002);
        realm.setPublicKeyPem("0234234");
        realm.setPrivateKeyPem("1234234");
        realm.addDefaultRole("default-role");

        RealmModel peristed = manager.getRealm(realm.getId());
        assertEquals(realm, peristed);

        RealmModel copy = importExport(realm, "copy");
        assertEquals(realm, copy);
    }

    public static void assertEquals(RealmModel expected, RealmModel actual) {
        Assert.assertEquals(expected.isAutomaticRegistrationAfterSocialLogin(),
                actual.isAutomaticRegistrationAfterSocialLogin());
        Assert.assertEquals(expected.isCookieLoginAllowed(), actual.isCookieLoginAllowed());
        Assert.assertEquals(expected.isRegistrationAllowed(), actual.isRegistrationAllowed());
        Assert.assertEquals(expected.isResetPasswordAllowed(), actual.isResetPasswordAllowed());
        Assert.assertEquals(expected.isSocial(), actual.isSocial());
        Assert.assertEquals(expected.isSslNotRequired(), actual.isSslNotRequired());
        Assert.assertEquals(expected.isVerifyEmail(), actual.isVerifyEmail());
        Assert.assertEquals(expected.getTokenLifespan(), actual.getTokenLifespan());

        Assert.assertEquals(expected.getAccessCodeLifespan(), actual.getAccessCodeLifespan());
        Assert.assertEquals(expected.getAccessCodeLifespanUserAction(), actual.getAccessCodeLifespanUserAction());
        Assert.assertEquals(expected.getPublicKeyPem(), actual.getPublicKeyPem());
        Assert.assertEquals(expected.getPrivateKeyPem(), actual.getPrivateKeyPem());

        assertEquals(expected.getDefaultRoles(), actual.getDefaultRoles());
    }

    public static void assertEquals(List<RoleModel> expected, List<RoleModel> actual) {
        Assert.assertEquals(expected.size(), actual.size());
        Iterator<RoleModel> exp = expected.iterator();
        Iterator<RoleModel> act = actual.iterator();
        while (exp.hasNext()) {
            Assert.assertEquals(exp.next().getName(), act.next().getName());
        }
    }

    private RealmModel importExport(RealmModel src, String copyName) {
        RealmRepresentation representation = manager.toRepresentation(src);
        RealmModel copy = manager.createRealm(copyName);
        manager.importRealm(representation, copy);
        return manager.getRealm(copy.getId());
    }

}
