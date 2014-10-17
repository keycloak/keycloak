package org.keycloak.testsuite.model;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.services.managers.ApplicationManager;

import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ApplicationModelTest extends AbstractModelTest {
    private ApplicationModel application;
    private RealmModel realm;
    private ApplicationManager appManager;

    @Before
    @Override
    public void before() throws Exception {
        super.before();
        appManager = new ApplicationManager(realmManager);

        realm = realmManager.createRealm("original");
        application = realm.addApplication("application");
        application.setBaseUrl("http://base");
        application.setManagementUrl("http://management");
        application.setName("app-name");
        application.addRole("role-1");
        application.addRole("role-2");
        application.addRole("role-3");
        application.addDefaultRole("role-1");
        application.addDefaultRole("role-2");

        application.addRedirectUri("redirect-1");
        application.addRedirectUri("redirect-2");

        application.addWebOrigin("origin-1");
        application.addWebOrigin("origin-2");

        application.registerNode("node1", 10);
        application.registerNode("10.20.30.40", 50);

        application.updateApplication();
    }

    @Test
    public void persist() {
        RealmModel persisted = realmManager.getRealm(realm.getId());

        ApplicationModel actual = persisted.getApplicationNameMap().get("app-name");
        assertEquals(application, actual);
    }

    @Test
    public void json() {
        ApplicationRepresentation representation = ModelToRepresentation.toRepresentation(application);
        representation.setId(null);

        RealmModel realm = realmManager.createRealm("copy");
        ApplicationModel copy = RepresentationToModel.createApplication(realm, representation, true);

        assertEquals(application, copy);
    }

    @Test
    public void testAddApplicationWithId() {
        application = realm.addApplication("app-123", "application2");
        commit();
        application = realmManager.getRealm(realm.getId()).getApplicationById("app-123");
        Assert.assertNotNull(application);
    }


    public static void assertEquals(ApplicationModel expected, ApplicationModel actual) {
        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertEquals(expected.getBaseUrl(), actual.getBaseUrl());
        Assert.assertEquals(expected.getManagementUrl(), actual.getManagementUrl());
        Assert.assertEquals(expected.getDefaultRoles(), actual.getDefaultRoles());

        Assert.assertTrue(expected.getRedirectUris().containsAll(actual.getRedirectUris()));
        Assert.assertTrue(expected.getWebOrigins().containsAll(actual.getWebOrigins()));
        Assert.assertTrue(expected.getRegisteredNodes().equals(actual.getRegisteredNodes()));
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

