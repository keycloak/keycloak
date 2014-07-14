package org.keycloak.model.test;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelProvider;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.util.JsonSerialization;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AbstractModelTest {

    protected static KeycloakSessionFactory sessionFactory;

    protected KeycloakSession session;
    protected RealmManager realmManager;
    protected ModelProvider model;

    @BeforeClass
    public static void beforeClass() {
        sessionFactory = KeycloakApplication.createSessionFactory();

        KeycloakSession session = sessionFactory.create();
        try {
            session.getTransaction().begin();
            new ApplianceBootstrap().bootstrap(session, "/auth");
            session.getTransaction().commit();
        } finally {
            session.close();
        }
    }

    @AfterClass
    public static void afterClass() {
        sessionFactory.close();
    }

    @Before
    public void before() throws Exception {
        session = sessionFactory.create();
        session.getTransaction().begin();
        model = session.model();
        realmManager = new RealmManager(session);
    }

    @After
    public void after() throws Exception {
        session.getTransaction().commit();
        session.close();

        session = sessionFactory.create();
        try {
            session.getTransaction().begin();
            model = session.model();

            RealmManager rm = new RealmManager(session);
            for (RealmModel realm : model.getRealms()) {
                if (!realm.getName().equals(Config.getAdminRealm())) {
                    rm.removeRealm(realm);
                }
            }

            session.getTransaction().commit();
        } finally {
            session.close();
        }

    }

    protected void commit() {
        commit(false);
    }

    protected void commit(boolean rollback) {
        if (rollback) {
            session.getTransaction().rollback();
        } else {
            session.getTransaction().commit();
        }
        resetSession();
    }

    protected void resetSession() {
        if (session.getTransaction().isActive()) {
            session.getTransaction().rollback();
        }

        session.close();

        session = sessionFactory.create();
        session.getTransaction().begin();
        model = session.model();
        realmManager = new RealmManager(session);
    }

    public static RealmRepresentation loadJson(String path) throws IOException {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int c;
        while ((c = is.read()) != -1) {
            os.write(c);
        }
        byte[] bytes = os.toByteArray();
        return JsonSerialization.readValue(bytes, RealmRepresentation.class);
    }


    // Helper methods for role equality

    public static void assertRolesEquals(Set<RoleModel> expected, Set<RoleModel> actual) {
        Assert.assertEquals(expected.size(), actual.size());
        for (RoleModel current : actual) {
            assertRolesContains(current, expected);
        }
    }

    public static void assertRolesContains(RoleModel expected, Set<RoleModel> actual) {
        for (RoleModel current : actual) {
            if (current.getId().equals(expected.getId())) {
                assertRolesEquals(current, expected);
                return;
            }
        }

        Assert.fail("Role with id=" + expected.getId() + " name=" + expected.getName() + " not in set " + actual);
    }

    public static void assertRolesEquals(RoleModel expected, RoleModel actual) {
        Assert.assertEquals(expected.getId(), actual.getId());
        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertEquals(expected.getDescription(), actual.getDescription());
        Assert.assertEquals(expected.getContainer(), actual.getContainer());
        Assert.assertEquals(expected.getComposites().size(), actual.getComposites().size());
    }
}
