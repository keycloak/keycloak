package org.keycloak.example.demo;

import org.jboss.resteasy.jwt.JsonSerialization;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.models.KeycloakSession;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RequiredCredentialModel;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.resources.RegistrationService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DemoApplication extends KeycloakApplication {

    public DemoApplication() {
        super();
        KeycloakSession session = factory.createSession();
        session.getTransaction().begin();
        RealmManager realmManager = new RealmManager(session);
        if (realmManager.defaultRealm() == null) {
            install(realmManager);
        }
        session.getTransaction().commit();
    }

    public void install(RealmManager manager) {
        RealmModel defaultRealm = manager.createRealm(RealmModel.DEFAULT_REALM, RealmModel.DEFAULT_REALM);
        defaultRealm.setName(RealmModel.DEFAULT_REALM);
        defaultRealm.setEnabled(true);
        defaultRealm.setTokenLifespan(300);
        defaultRealm.setAccessCodeLifespan(60);
        defaultRealm.setSslNotRequired(false);
        defaultRealm.setCookieLoginAllowed(true);
        defaultRealm.setRegistrationAllowed(true);
        manager.generateRealmKeys(defaultRealm);
        defaultRealm.addRequiredCredential(RequiredCredentialModel.PASSWORD);
        defaultRealm.addRole(RegistrationService.REALM_CREATOR_ROLE);

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
