package org.aerogear.ups.security;

import org.jboss.resteasy.core.Dispatcher;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UpsSecurityApplication extends KeycloakApplication {
    public UpsSecurityApplication(@Context ServletContext context, @Context Dispatcher dispatcher) {
        super(context, dispatcher);
    }

    @Override
    protected void setupDefaultRealm(String contextPath) {
        super.setupDefaultRealm(contextPath);

        KeycloakSession session = sessionFactory.create();
        session.getTransaction().begin();

        // disable master realm by deleting the admin user.
        try {
            RealmManager manager = new RealmManager(session);
            RealmModel master = manager.getKeycloakAdminstrationRealm();
            UserModel admin = session.users().getUserByUsername("admin", master);
            if (admin != null) session.users().removeUser(master, admin);
            session.getTransaction().commit();
        } finally {
            session.close();
        }

    }
}
