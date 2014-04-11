package org.keycloak.server;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.models.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.KeycloakUriBuilder;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class KeycloakServerApplication extends KeycloakApplication {

    private static final Logger log = Logger.getLogger(KeycloakServerApplication.class);

    public KeycloakServerApplication(@Context ServletContext servletContext,@Context Dispatcher dispatcher) throws FileNotFoundException {
        super(servletContext, dispatcher);
        KeycloakSession session = factory.createSession();
        session.getTransaction().begin();
        try {
            InputStream is = servletContext.getResourceAsStream("/WEB-INF/testrealm.json");
            RealmRepresentation rep = loadJson(is, RealmRepresentation.class);
            RealmModel realm = importRealm(session, rep);
            AdapterDeploymentContext deploymentContext = (AdapterDeploymentContext)servletContext.getAttribute(AdapterDeploymentContext.class.getName());
            AdapterConfig adapterConfig = new AdapterConfig();
            String host = (String)servletContext.getInitParameter("host-port");
            String uri = KeycloakUriBuilder.fromUri("http://" + host).path(servletContext.getContextPath()).build().toString();
            log.info("**** auth server url: " + uri);
            adapterConfig.setRealm("demo");
            adapterConfig.setResource("customer-portal");
            adapterConfig.setRealmKey(realm.getPublicKeyPem());
            Map<String, String> creds = new HashMap<String, String>();
            creds.put(CredentialRepresentation.SECRET, "password");
            adapterConfig.setCredentials(creds);
            adapterConfig.setAuthServerUrl(uri);
            adapterConfig.setSslNotRequired(true);
            deploymentContext.updateDeployment(adapterConfig);
            session.getTransaction().commit();
        } finally {
            session.close();
        }

    }

    public RealmModel importRealm(KeycloakSession session, RealmRepresentation rep) {
        RealmManager manager = new RealmManager(session);

        RealmModel realm = manager.getRealmByName(rep.getRealm());
        if (realm != null) {
            log.info("Not importing realm " + rep.getRealm() + " realm already exists");
            return realm;
        }

        realm = manager.createRealm(rep.getId(), rep.getRealm());
        manager.importRealm(rep, realm);

        log.info("Imported realm " + realm.getName());
        return realm;
    }

    private static <T> T loadJson(InputStream is, Class<T> type) {
        try {
            return JsonSerialization.readValue(is, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse json", e);
        }
    }

}
