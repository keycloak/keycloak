package org.keycloak.server;

import org.jboss.resteasy.jwt.JsonSerialization;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.KeycloakApplication;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class KeycloakServerApplication extends KeycloakApplication {

    private static final Logger log = Logger.getLogger(KeycloakServerApplication.class);

    public KeycloakServerApplication(@Context ServletContext servletContext) throws FileNotFoundException {
        super(servletContext);
        KeycloakSession session = factory.createSession();
        session.getTransaction().begin();
        ApplianceBootstrap bootstrap = new ApplianceBootstrap();
        bootstrap.bootstrap(session);

        String importRealm = System.getProperty("keycloak.import");
        if (importRealm != null) {
            RealmRepresentation rep = loadJson(new FileInputStream(importRealm), RealmRepresentation.class);
            importRealm(session, rep);
        }

        session.getTransaction().commit();
    }

    public void importRealm(KeycloakSession session, RealmRepresentation rep ) {
        try {
            RealmManager manager = new RealmManager(session);

            if (rep.getId() == null) {
                throw new RuntimeException("Realm id not specified");
            }

            if (manager.getRealm(rep.getId()) != null) {
                log.info("Not importing realm " + rep.getRealm() + " realm already exists");
                return;
            }

            RealmModel realm = manager.createRealm(rep.getId(), rep.getRealm());
            manager.importRealm(rep, realm);

            log.info("Imported realm " + realm.getName());

            session.getTransaction().commit();
        } finally {
            session.close();
        }
    }

    private static <T> T loadJson(InputStream is, Class<T> type) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int c;
            while ((c = is.read()) != -1) {
                os.write(c);
            }
            byte[] bytes = os.toByteArray();
            return JsonSerialization.fromBytes(type, bytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse json", e);
        }
    }

}
