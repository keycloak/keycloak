package org.keycloak.model.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.jboss.resteasy.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AbstractModelTest {

    private final Logger log = Logger.getLogger(getClass());

    protected KeycloakSessionFactory factory;
    protected KeycloakSession identitySession;
    protected RealmManager realmManager;

    @Before
    public void before() throws Exception {
        factory = KeycloakApplication.createSessionFactory();
        identitySession = factory.createSession();
        identitySession.getTransaction().begin();
        realmManager = new RealmManager(identitySession);
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
}
