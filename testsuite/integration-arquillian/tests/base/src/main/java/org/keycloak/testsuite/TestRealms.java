package org.keycloak.testsuite;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import javax.ws.rs.NotFoundException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import static org.keycloak.testsuite.util.Json.loadJson;

/**
 *
 * @author tkyjovsk
 */
public class TestRealms extends HashMap<String, RealmRepresentation> {

    private static final long serialVersionUID = 1L;

    public static final String CONSOLE_DEMO = "console-demo-realm";
    public static final String ADAPTER_SERVLETS_DEMO = "servlets-demo-realm";
    public static final String ADAPTER_PRECONFIGURED_DEMO = "preconfigured-demo-realm";
    public static final String ADAPTER_TENANT1 = "tenant1";
    public static final String ADAPTER_TENANT2 = "tenant2";

    public static void importRealm(Keycloak keycloak, RealmRepresentation realm) {
        System.out.println("importing realm: " + realm.getRealm());
        try { // TODO - figure out a way how to do this without try-catch
            RealmResource realmResource = keycloak.realms().realm(realm.getRealm());
            RealmRepresentation rRep = realmResource.toRepresentation();
            System.out.println(" realm already exists on server, re-importing");
            realmResource.remove();
        } catch (NotFoundException nfe) {
            // expected when realm does not exist
        }
        keycloak.realms().create(realm);
    }

    public static void removeRealm(Keycloak keycloak, RealmRepresentation testRealm) {
        keycloak.realm(testRealm.getRealm()).remove();
    }

    public static RealmRepresentation loadRealm(String realmConfig) {
        return loadRealm(TestRealms.class.getResourceAsStream(realmConfig));
    }

    public static RealmRepresentation loadRealm(File realmFile) {
        try {
            return loadRealm(new FileInputStream(realmFile));
        } catch (FileNotFoundException ex) {
            throw new IllegalStateException("Test realm file not found: " + realmFile);
        }
    }

    public static RealmRepresentation loadRealm(InputStream is) {
        RealmRepresentation realm = loadJson(is, RealmRepresentation.class);
        System.out.println("Loaded realm " + realm.getRealm());
        return realm;
    }

}
