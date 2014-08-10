package org.keycloak.exportimport.util;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.exportimport.Strategy;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        RealmProvider model = session.realms();
        RealmModel realm = model.getRealmByName(realmName);

        if (realm != null) {
            if (strategy == Strategy.IGNORE_EXISTING) {
                logger.infof("Realm '%s' already exists. Import skipped", realmName);
                return realm;
            } else {
                logger.infof("Realm '%s' already exists. Removing it before import", realmName);
                if (Config.getAdminRealm().equals(realm.getId())) {
                    // Delete all masterAdmin apps due to foreign key constraints
                    for (RealmModel currRealm : model.getRealms()) {
                        currRealm.setMasterAdminApp(null);
                    }
                }
                // TODO: For migration between versions, it should be possible to delete just realm but keep it's users
                model.removeRealm(realm.getId());
            }
        }

        realm = rep.getId() != null ? model.createRealm(rep.getId(), realmName) : model.createRealm(realmName);

        RepresentationToModel.importRealm(session, rep, realm);

        refreshMasterAdminApps(model, realm);

        logger.infof("Realm '%s' imported", realmName);
        return realm;
    }

    private static void refreshMasterAdminApps(RealmProvider model, RealmModel realm) {
        String adminRealmId = Config.getAdminRealm();
        if (adminRealmId.equals(realm.getId())) {
            // We just imported master realm. All 'masterAdminApps' need to be refreshed
            RealmModel adminRealm = realm;
            for (RealmModel currentRealm : model.getRealms()) {
                ApplicationModel masterApp = adminRealm.getApplicationByName(KeycloakModelUtils.getMasterRealmAdminApplicationName(currentRealm));
                if (masterApp != null) {
                    currentRealm.setMasterAdminApp(masterApp);
                }  else {
                    setupMasterAdminManagement(model, currentRealm);
                }
            }
        } else {
            // Need to refresh masterApp for current realm
            RealmModel adminRealm = model.getRealm(adminRealmId);
            ApplicationModel masterApp = adminRealm.getApplicationByName(KeycloakModelUtils.getMasterRealmAdminApplicationName(realm));
            if (masterApp != null) {
                realm.setMasterAdminApp(masterApp);
            }  else {
                setupMasterAdminManagement(model, realm);
            }
        }
    }

    // TODO: We need method here, so we are able to refresh masterAdmin applications after import. Should be RealmManager moved to model/api instead?
    public static void setupMasterAdminManagement(RealmProvider model, RealmModel realm) {
        RealmModel adminRealm;
        RoleModel adminRole;

        if (realm.getName().equals(Config.getAdminRealm())) {
            adminRealm = realm;

            adminRole = realm.addRole(AdminRoles.ADMIN);

            RoleModel createRealmRole = realm.addRole(AdminRoles.CREATE_REALM);
            adminRole.addCompositeRole(createRealmRole);
        } else {
            adminRealm = model.getRealmByName(Config.getAdminRealm());
            adminRole = adminRealm.getRole(AdminRoles.ADMIN);
        }

        ApplicationModel realmAdminApp = KeycloakModelUtils.createApplication(adminRealm, KeycloakModelUtils.getMasterRealmAdminApplicationName(realm));
        realmAdminApp.setBearerOnly(true);
        realm.setMasterAdminApp(realmAdminApp);

        for (String r : AdminRoles.ALL_REALM_ROLES) {
            RoleModel role = realmAdminApp.addRole(r);
            adminRole.addCompositeRole(role);
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

                List<RealmRepresentation> realmReps = new ArrayList<RealmRepresentation>();
                while (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                    RealmRepresentation realmRep = parser.readValueAs(RealmRepresentation.class);
                    parser.nextToken();

                    // Ensure that master realm is imported first
                    if (Config.getAdminRealm().equals(realmRep.getRealm())) {
                        realmReps.add(0, realmRep);
                    } else {
                        realmReps.add(realmRep);
                    }
                }

                for (RealmRepresentation realmRep : realmReps) {
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
        RealmProvider model = session.realms();
        JsonFactory factory = mapper.getJsonFactory();
        JsonParser parser = factory.createJsonParser(is);
        try {
            parser.nextToken();

            while (parser.nextToken() == JsonToken.FIELD_NAME) {
                if ("realm".equals(parser.getText())) {
                    parser.nextToken();
                    String currRealmName = parser.getText();
                    if (!currRealmName.equals(realmName)) {
                        throw new IllegalStateException("Trying to import users into invalid realm. Realm name: " + realmName + ", Expected realm name: " + currRealmName);
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

                    importUsers(session, model, realmName, userReps);

                    if (parser.getCurrentToken() == JsonToken.END_ARRAY) {
                        parser.nextToken();
                    }
                }
            }
        } finally {
            parser.close();
        }
    }

    private static void importUsers(KeycloakSession session, RealmProvider model, String realmName, List<UserRepresentation> userReps) {
        RealmModel realm = model.getRealmByName(realmName);
        Map<String, ApplicationModel> apps = realm.getApplicationNameMap();
        for (UserRepresentation user : userReps) {
            RepresentationToModel.createUser(session, realm, user, apps);
        }
    }

}
