package org.keycloak.model.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.jboss.resteasy.logging.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AbstractModelTest {

    protected KeycloakSessionFactory factory;
    protected KeycloakSession identitySession;
    protected RealmManager realmManager;

    @Before
    public void before() throws Exception {
        factory = KeycloakApplication.createSessionFactory();
        identitySession = factory.createSession();
        identitySession.getTransaction().begin();
        realmManager = new RealmManager(identitySession);

        new ApplianceBootstrap().bootstrap(identitySession);
    }

    @After
    public void after() throws Exception {
        identitySession.getTransaction().commit();
        identitySession.close();
        factory.close();
    }

    protected void commit() {
        identitySession.getTransaction().commit();
        identitySession.close();
        identitySession = factory.createSession();
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
        System.out.println(new String(bytes));

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
