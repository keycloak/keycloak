package org.keycloak.model.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.Config;
import org.keycloak.models.cache.CacheKeycloakSession;
import org.keycloak.models.cache.SimpleCache;
import org.keycloak.provider.ProviderSession;
import org.keycloak.provider.ProviderSessionFactory;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AbstractModelTest {

    protected static ProviderSessionFactory providerSessionFactory;

    protected KeycloakSession identitySession;
    protected RealmManager realmManager;
    protected ProviderSession providerSession;

    @BeforeClass
    public static void beforeClass() {
        providerSessionFactory = KeycloakApplication.createProviderSessionFactory();

        ProviderSession providerSession = providerSessionFactory.createSession();
        KeycloakSession identitySession = providerSession.getProvider(CacheKeycloakSession.class, "simple");
        try {
            identitySession.getTransaction().begin();
            new ApplianceBootstrap().bootstrap(identitySession, "/auth");
            identitySession.getTransaction().commit();
        } finally {
            providerSession.close();
        }
    }

    @AfterClass
    public static void afterClass() {
        providerSessionFactory.close();
    }

    @Before
    public void before() throws Exception {
        providerSession = providerSessionFactory.createSession();

        identitySession = providerSession.getProvider(CacheKeycloakSession.class, "simple");
        identitySession.getTransaction().begin();
        realmManager = new RealmManager(identitySession);
    }

    @After
    public void after() throws Exception {
        identitySession.getTransaction().commit();
        providerSession.close();

        providerSession = providerSessionFactory.createSession();
        identitySession = providerSession.getProvider(CacheKeycloakSession.class, "simple");
        try {
            identitySession.getTransaction().begin();

            RealmManager rm = new RealmManager(identitySession);
            for (RealmModel realm : identitySession.getRealms()) {
                if (!realm.getName().equals(Config.getAdminRealm())) {
                    rm.removeRealm(realm);
                }
            }

            identitySession.getTransaction().commit();
        } finally {
            providerSession.close();
        }

    }

    protected void commit() {
        commit(false);
    }

    protected void commit(boolean rollback) {
        if (rollback) {
            identitySession.getTransaction().rollback();
        } else {
            identitySession.getTransaction().commit();
        }
        resetSession();
    }

    protected void resetSession() {
        providerSession.close();

        providerSession = providerSessionFactory.createSession();
        identitySession = providerSession.getProvider(CacheKeycloakSession.class, "simple");
        identitySession.getTransaction().begin();
        realmManager = new RealmManager(identitySession);
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
