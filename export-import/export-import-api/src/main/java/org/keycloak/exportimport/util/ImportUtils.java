package org.keycloak.exportimport.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.io.SerializedString;
import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.exportimport.Strategy;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelProvider;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ImportUtils {

    private static final Logger logger = Logger.getLogger(ImportUtils.class);

    /**
     * Fully import realm from representation, save it to model and return model of newly created realm
     *
     * @param session
     * @param rep
     * @param strategy specifies whether to overwrite or ignore existing realm or user entries
     * @return newly imported realm (or existing realm if ignoreExisting is true and realm of this name already exists)
     */
    public static RealmModel importRealm(KeycloakSession session, RealmRepresentation rep, Strategy strategy) {
        String realmName = rep.getRealm();
        ModelProvider model = session.getModel();
        RealmModel realm = model.getRealmByName(realmName);

        if (realm != null) {
            if (strategy == Strategy.IGNORE_EXISTING) {
                logger.infof("Realm '%s' already exists. Import skipped", realmName);
                return realm;
            } else {
                logger.infof("Realm '%s' already exists. Removing it before import", realmName);
                model.removeRealm(realm.getId());
            }
        }

        realm = rep.getId() != null ? model.createRealm(rep.getId(), realmName) : model.createRealm(realmName);

        RepresentationToModel.importRealm(rep, realm);

        refreshMasterAdminApps(model, realm);

        logger.infof("Realm '%s' imported", realmName);
        return realm;
    }

    private static void refreshMasterAdminApps(ModelProvider model, RealmModel realm) {
        String adminRealmId = Config.getAdminRealm();
        if (adminRealmId.equals(realm.getId())) {
            // We just imported master realm. All 'masterAdminApps' need to be refreshed
            RealmModel adminRealm = realm;
            for (RealmModel currentRealm : model.getRealms()) {
                ApplicationModel masterApp = adminRealm.getApplicationByName(ExportImportUtils.getMasterRealmAdminApplicationName(currentRealm));
                currentRealm.setMasterAdminApp(masterApp);
            }
        } else {
            // Need to refresh masterApp for current realm
            RealmModel adminRealm = model.getRealm(adminRealmId);
            ApplicationModel masterApp = adminRealm.getApplicationByName(ExportImportUtils.getMasterRealmAdminApplicationName(realm));
            realm.setMasterAdminApp(masterApp);
        }
    }


    /**
     * Fully import realm (or more realms from particular stream)
     *
     * @param session
     * @param mapper
     * @param is
     * @param strategy
     * @throws IOException
     */
    public static void importFromStream(KeycloakSession session, ObjectMapper mapper, InputStream is, Strategy strategy) throws IOException {
        JsonFactory factory = mapper.getJsonFactory();
        JsonParser parser = factory.createJsonParser(is);
        try {
            parser.nextToken();

            if (parser.getCurrentToken() == JsonToken.START_ARRAY) {
                // Case with more realms in stream
                parser.nextToken();
                while (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                    RealmRepresentation realmRep = parser.readValueAs(RealmRepresentation.class);
                    parser.nextToken();
                    importRealm(session, realmRep, strategy);
                }
            } else if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                // Case with single realm in stream
                RealmRepresentation realmRep = parser.readValueAs(RealmRepresentation.class);
                importRealm(session, realmRep, strategy);
            }
        } finally {
            parser.close();
        }
    }

    // Assuming that it's invoked inside transaction
    public static void importUsersFromStream(KeycloakSession session, String realmName, ObjectMapper mapper, InputStream is) throws IOException {
        ModelProvider model = session.getModel();
        JsonFactory factory = mapper.getJsonFactory();
        JsonParser parser = factory.createJsonParser(is);
        try {
            parser.nextToken();

            while (parser.nextToken() == JsonToken.FIELD_NAME) {
                if ("realm".equals(parser.getText())) {
                    parser.nextToken();
                    String currRealmName = parser.getText();
                    if (!currRealmName.equals(realmName)) {
                        throw new IllegalStateException("Trying to import users into invalid realm. Realm name: " + realmName + ", Expected realm name: " + realmName);
                    }
                } else if ("users".equals(parser.getText())) {
                    parser.nextToken();

                    if (parser.getCurrentToken() == JsonToken.START_ARRAY) {
                        parser.nextToken();
                    }

                    // TODO: support for more transactions per single users file (if needed)
                    List<UserRepresentation> userReps = new ArrayList<UserRepresentation>();
                    while (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                        UserRepresentation user = parser.readValueAs(UserRepresentation.class);
                        userReps.add(user);
                        parser.nextToken();
                    }

                    importUsers(model, realmName, userReps);

                    if (parser.getCurrentToken() == JsonToken.END_ARRAY) {
                        parser.nextToken();
                    }
                }
            }
        } finally {
            parser.close();
        }
    }

    private static void importUsers(ModelProvider model, String realmName, List<UserRepresentation> userReps) {
        RealmModel realm = model.getRealmByName(realmName);
        Map<String, ApplicationModel> apps = realm.getApplicationNameMap();
        for (UserRepresentation user : userReps) {
            RepresentationToModel.createUser(realm, user, apps);
        }
    }

}
