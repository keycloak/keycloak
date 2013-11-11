package org.keycloak.test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.services.managers.ApplicationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;

import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ApplicationModelTest extends AbstractKeycloakServerTest {
    private KeycloakSessionFactory factory;
    private KeycloakSession identitySession;
    private RealmManager manager;
    private ApplicationModel application;
    private RealmModel realm;
    private ApplicationManager appManager;

    @Before
    public void before() throws Exception {
        factory = KeycloakApplication.buildSessionFactory();
        identitySession = factory.createSession();
        identitySession.getTransaction().begin();
        manager = new RealmManager(identitySession);

        appManager = new ApplicationManager(manager);

        realm = manager.createRealm("original");
        application = realm.addApplication("application");
        application.setBaseUrl("http://base");
        application.setManagementUrl("http://management");
        application.setName("app-name");
        application.addRole("role-1");
        application.addRole("role-2");
        application.addDefaultRole("role-1");
        application.addDefaultRole("role-2");

        application.getApplicationUser().addRedirectUri("redirect-1");
        application.getApplicationUser().addRedirectUri("redirect-2");

        application.getApplicationUser().addWebOrigin("origin-1");
        application.getApplicationUser().addWebOrigin("origin-2");

        application.updateApplication();
    }

    @After
    public void after() throws Exception {
        identitySession.getTransaction().commit();
        identitySession.close();
        factory.close();
    }

    @Test
    public void persist() {
        RealmModel persisted = manager.getRealm(realm.getId());

        assertEquals(application, persisted.getApplications().get(0));
    }

    @Test
    public void json() {
        ApplicationRepresentation representation = appManager.toRepresentation(application);

        RealmModel realm = manager.createRealm("copy");
        ApplicationModel copy = appManager.createApplication(realm, representation);

        assertEquals(application, copy);
    }

    public static void assertEquals(ApplicationModel expected, ApplicationModel actual) {
        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertEquals(expected.getBaseUrl(), actual.getBaseUrl());
        Assert.assertEquals(expected.getManagementUrl(), actual.getManagementUrl());
        Assert.assertEquals(expected.getDefaultRoles(), actual.getDefaultRoles());

        UserModel auser = actual.getApplicationUser();
        UserModel euser = expected.getApplicationUser();

        Assert.assertTrue(euser.getRedirectUris().containsAll(auser.getRedirectUris()));
        Assert.assertTrue(euser.getWebOrigins().containsAll(auser.getWebOrigins()));
    }

    public static void assertEquals(List<RoleModel> expected, List<RoleModel> actual) {
        Assert.assertEquals(expected.size(), actual.size());
        Iterator<RoleModel> exp = expected.iterator();
        Iterator<RoleModel> act = actual.iterator();
        while (exp.hasNext()) {
            Assert.assertEquals(exp.next().getName(), act.next().getName());
        }
    }

}

