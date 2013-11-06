package org.keycloak.server;

import org.jboss.resteasy.jwt.JsonSerialization;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.KeycloakApplication;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class KeycloakServerApplication extends KeycloakApplication {

    public KeycloakServerApplication(@Context ServletContext servletContext) {
        super(servletContext);
        KeycloakSession session = factory.createSession();
        session.getTransaction().begin();
        ApplianceBootstrap bootstrap = new ApplianceBootstrap();
        bootstrap.bootstrap(session);
        session.getTransaction().commit();
    }

}
