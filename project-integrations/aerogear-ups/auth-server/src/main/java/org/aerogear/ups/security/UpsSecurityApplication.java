package org.aerogear.ups.security;

import org.jboss.resteasy.core.Dispatcher;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderSession;
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
        ProviderSession providerSession = providerSessionFactory.createSession();
        KeycloakSession session = providerSession.getProvider(KeycloakSession.class);
        session.getTransaction().begin();

        // disable master realm by deleting the admin user.
        try {
            RealmManager manager = new RealmManager(session);
            RealmModel master = manager.getKeycloakAdminstrationRealm();
            UserModel admin = master.getUser("admin");
            if (admin != null) master.removeUser(admin.getLoginName());
            session.getTransaction().commit();
        } finally {
            providerSession.close();
        }

    }
}
