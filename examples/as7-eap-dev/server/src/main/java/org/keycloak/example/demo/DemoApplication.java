package org.keycloak.example.demo;

import org.jboss.resteasy.jwt.JsonSerialization;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DemoApplication extends KeycloakApplication {

    public DemoApplication(@Context ServletContext servletContext) {
        super(servletContext);
        KeycloakSession session = factory.createSession();
        session.getTransaction().begin();
        ApplianceBootstrap bootstrap = new ApplianceBootstrap();
        bootstrap.bootstrap(session);
        install(new RealmManager(session));
        session.getTransaction().commit();
    }

    public void install(RealmManager manager) {
        RealmRepresentation rep = loadJson("META-INF/testrealm.json");
        RealmModel realm = manager.createRealm("demo", rep.getRealm());
        manager.importRealm(rep, realm);

    }

    public static RealmRepresentation loadJson(String path)
    {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int c;
        try {
            while ( (c = is.read()) != -1)
            {
                os.write(c);
            }
            byte[] bytes = os.toByteArray();
            //System.out.println(new String(bytes));

            return JsonSerialization.fromBytes(RealmRepresentation.class, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
