package org.keycloak.testsuite.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.util.JsonSerialization;

/**
 *
 * @author tkyjovsk
 */
public class IOUtil {

    public static <T> T loadJson(InputStream is, Class<T> type) {
        try {
            return JsonSerialization.readValue(is, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load json.", e);
        }
    }

    public static RealmRepresentation loadRealm(String realmConfig) {
        return loadRealm(IOUtil.class.getResourceAsStream(realmConfig));
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
