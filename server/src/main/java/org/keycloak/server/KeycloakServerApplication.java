package org.keycloak.server;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.models.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.util.JsonSerialization;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class KeycloakServerApplication extends KeycloakApplication {

    private static final Logger log = Logger.getLogger(KeycloakServerApplication.class);

    public KeycloakServerApplication(@Context ServletContext servletContext) throws FileNotFoundException {
        super(servletContext);

        String importRealm = System.getProperty("keycloak.import");
        if (importRealm != null) {
            KeycloakSession session = factory.createSession();
            session.getTransaction().begin();
            RealmRepresentation rep = loadJson(new FileInputStream(importRealm), RealmRepresentation.class);
            importRealm(session, rep);
        }
    }

    public void importRealm(KeycloakSession session, RealmRepresentation rep) {
        try {
            RealmManager manager = new RealmManager(session);

            if (rep.getId() == null) {
                throw new RuntimeException("Realm id not specified");
            }

            if (manager.getRealmByName(rep.getRealm()) != null) {
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
            return JsonSerialization.readValue(is, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse json", e);
        }
    }

}
