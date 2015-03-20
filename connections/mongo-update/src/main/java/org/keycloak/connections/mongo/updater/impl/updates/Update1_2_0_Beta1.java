package org.keycloak.connections.mongo.updater.impl.updates;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.keycloak.Config;
import org.keycloak.connections.mongo.impl.types.MapMapper;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.services.util.MigrationUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class Update1_2_0_Beta1 extends Update {

    @Override
    public String getId() {
        return "1.2.0.Beta1";
    }

    @Override
    public void update(KeycloakSession session) {
        convertSocialToIdFedRealms();
        convertSocialToIdFedUsers();
        addAccessCodeLoginTimeout();
        addNewAdminRoles();
        addDefaultProtocolMappers(session);
    }


    private void convertSocialToIdFedRealms() {
        DBCollection realms = db.getCollection("realms");
        DBCursor realmsCursor = realms.find();

        try {
            while (realmsCursor.hasNext()) {
                BasicDBObject realm = (BasicDBObject) realmsCursor.next();
                boolean updateProfileOnInitialSocialLogin = realm.getBoolean("updateProfileOnInitialSocialLogin");
                BasicDBObject socialConfig = (BasicDBObject) realm.get("socialConfig");

                BasicDBList identityProviders = (BasicDBList) realm.get("identityProviders");
                if (identityProviders == null) {
                    identityProviders = new BasicDBList();
                    realm.put("identityProviders", identityProviders);
                }

                if (socialConfig != null) {
                    for (Map.Entry<String, Object> entry : socialConfig.entrySet()) {
                        if (entry.getKey().endsWith("###key")) {
                            String socialProviderId = entry.getKey().substring(0, entry.getKey().indexOf("###"));
                            String clientId = (String) entry.getValue();
                            String clientSecret = socialConfig.getString(socialProviderId + "###secret");

                            DBObject identityProviderConfig = new BasicDBObjectBuilder()
                                    .add("clientId", clientId)
                                    .add("clientSecret", clientSecret).get();

                            DBObject identityProvider = new BasicDBObjectBuilder()
                                    .add("internalId", KeycloakModelUtils.generateId())
                                    .add("providerId", socialProviderId)
                                    .add("name", socialProviderId)
                                    .add("id", socialProviderId)
                                    .add("updateProfileFirstLogin", updateProfileOnInitialSocialLogin)
                                    .add("enabled", true)
                                    .add("storeToken", false)
                                    .add("authenticateByDefault", false)
                                    .add("config", identityProviderConfig).get();

                            identityProviders.add(identityProvider);
                            log.debugv("Converted social provider {0} to identity provider", socialProviderId);
                        }
                    }
                }

                // Remove obsolete keys from realm
                realm.remove("social");
                realm.remove("updateProfileOnInitialSocialLogin");
                realm.remove("socialConfig");

                // Update realm in DB now
                realms.save(realm);

                log.debugv("Social providers of realm {0} converted to identity providers", realm.get("_id"));
            }
        } finally {
            realmsCursor.close();
        }
    }

    private void convertSocialToIdFedUsers() {
        DBCollection users = db.getCollection("users");
        DBCursor usersCursor = users.find();

        try {
            while (usersCursor.hasNext()) {
                BasicDBObject user = (BasicDBObject) usersCursor.next();

                BasicDBList socialLinks = (BasicDBList) user.get("socialLinks");
                if (socialLinks != null) {
                    BasicDBList federatedIdentities = (BasicDBList) user.get("federatedIdentities");
                    if (federatedIdentities == null) {
                        federatedIdentities = new BasicDBList();
                        user.put("federatedIdentities", federatedIdentities);
                    }

                    for (Object socialLinkObj : socialLinks) {
                        BasicDBObject socialLink = (BasicDBObject) socialLinkObj;
                        BasicDBObject idFedLink = new BasicDBObject();
                        idFedLink.put("userName", socialLink.get("socialUsername"));
                        idFedLink.put("userId", socialLink.get("socialUserId"));
                        idFedLink.put("identityProvider", socialLink.get("socialProvider"));

                        federatedIdentities.add(idFedLink);
                    }

                    // Remove obsolete keys and save user
                    user.remove("socialLinks");
                    users.save(user);

                    if (log.isTraceEnabled()) {
                        log.tracev("Social links of user {0} converted to identity links", user.get("_id"));
                    }
                }
            }
        } finally {
            usersCursor.close();
        }

        log.debug("Social links of users converted to identity links");
    }

    private void addAccessCodeLoginTimeout() {
        DBCollection realms = db.getCollection("realms");
        DBCursor realmsCursor = realms.find();

        try {
            while (realmsCursor.hasNext()) {
                BasicDBObject realm = (BasicDBObject) realmsCursor.next();
                realm.put("accessCodeLifespanLogin", 1800);
                realms.save(realm);
            }
        } finally {
            realmsCursor.close();
        }
    }

    private void addNewAdminRoles() {
        DBCollection realms = db.getCollection("realms");
        String adminRealmName = Config.getAdminRealm();

        DBCursor realmsCursor = realms.find();
        try {
            while (realmsCursor.hasNext()) {
                BasicDBObject realm = (BasicDBObject) realmsCursor.next();
                if (adminRealmName.equals(realm.get("name"))) {
                    addNewAdminRolesToMasterRealm(realm);
                } else {
                    addNewAdminRolesToRealm(realm);
                }
            }
        } finally {
            realmsCursor.close();
        }
    }

    private void addNewAdminRolesToMasterRealm(BasicDBObject adminRealm) {
        DBCollection realms = db.getCollection("realms");
        DBCollection applications = db.getCollection("applications");
        DBCollection roles = db.getCollection("roles");

        DBCursor realmsCursor = realms.find();
        try {
            while (realmsCursor.hasNext()) {
                BasicDBObject currentRealm = (BasicDBObject) realmsCursor.next();
                String masterAdminAppName = currentRealm.getString("name") + "-realm";

                BasicDBObject masterAdminApp = (BasicDBObject) applications.findOne(new BasicDBObject().append("realmId", adminRealm.get("_id")).append("name", masterAdminAppName));

                String viewIdProvidersRoleId = insertApplicationRole(roles, AdminRoles.VIEW_IDENTITY_PROVIDERS, masterAdminApp.getString("_id"));
                String manageIdProvidersRoleId = insertApplicationRole(roles, AdminRoles.MANAGE_IDENTITY_PROVIDERS, masterAdminApp.getString("_id"));

                BasicDBObject adminRole = (BasicDBObject) roles.findOne(new BasicDBObject().append("realmId", adminRealm.get("_id")).append("name", AdminRoles.ADMIN));
                BasicDBList adminCompositeRoles = (BasicDBList) adminRole.get("compositeRoleIds");
                adminCompositeRoles.add(viewIdProvidersRoleId);
                adminCompositeRoles.add(manageIdProvidersRoleId);
                roles.save(adminRole);

                log.debugv("Added roles {0} and {1} to application {2}", AdminRoles.VIEW_IDENTITY_PROVIDERS, AdminRoles.MANAGE_IDENTITY_PROVIDERS, masterAdminAppName);
            }
        } finally {
            realmsCursor.close();
        }
    }

    private void addNewAdminRolesToRealm(BasicDBObject currentRealm) {
        DBCollection applications = db.getCollection("applications");
        DBCollection roles = db.getCollection("roles");

        BasicDBObject adminApp = (BasicDBObject) applications.findOne(new BasicDBObject().append("realmId", currentRealm.get("_id")).append("name", "realm-management"));

        String viewIdProvidersRoleId = insertApplicationRole(roles, AdminRoles.VIEW_IDENTITY_PROVIDERS, adminApp.getString("_id"));
        String manageIdProvidersRoleId = insertApplicationRole(roles, AdminRoles.MANAGE_IDENTITY_PROVIDERS, adminApp.getString("_id"));

        BasicDBObject adminRole = (BasicDBObject) roles.findOne(new BasicDBObject().append("applicationId", adminApp.get("_id")).append("name", AdminRoles.REALM_ADMIN));
        BasicDBList adminCompositeRoles = (BasicDBList) adminRole.get("compositeRoleIds");
        adminCompositeRoles.add(viewIdProvidersRoleId);
        adminCompositeRoles.add(manageIdProvidersRoleId);

        roles.save(adminRole);
        log.debugv("Added roles {0} and {1} to application realm-management of realm {2}", AdminRoles.VIEW_IDENTITY_PROVIDERS, AdminRoles.MANAGE_IDENTITY_PROVIDERS, currentRealm.get("name"));
    }

    private void addDefaultProtocolMappers(KeycloakSession session) {
        addDefaultMappers(session, db.getCollection("applications"));
        addDefaultMappers(session, db.getCollection("oauthClients"));
    }

    private void addDefaultMappers(KeycloakSession session, DBCollection clients) {
        DBCursor clientsCursor = clients.find();
        try {
            while (clientsCursor.hasNext()) {
                BasicDBObject currentClient = (BasicDBObject) clientsCursor.next();

                BasicDBList dbProtocolMappers = new BasicDBList();
                currentClient.put("protocolMappers", dbProtocolMappers);

                Object claimMask = currentClient.get("allowedClaimsMask");
                Collection<ProtocolMapperModel> clientProtocolMappers = MigrationUtils.getMappersForClaimMask(session, (Long) claimMask);

                for (ProtocolMapperModel protocolMapper : clientProtocolMappers) {
                    BasicDBObject dbMapper = new BasicDBObject();
                    dbMapper.put("id", KeycloakModelUtils.generateId());
                    dbMapper.put("protocol", protocolMapper.getProtocol());
                    dbMapper.put("name", protocolMapper.getName());
                    dbMapper.put("consentRequired", protocolMapper.isConsentRequired());
                    dbMapper.put("consentText", protocolMapper.getConsentText());
                    dbMapper.put("protocolMapper", protocolMapper.getProtocolMapper());

                    Map<String, String> config = protocolMapper.getConfig();
                    BasicDBObject dbConfig = MapMapper.convertMap(config);
                    dbMapper.put("config", dbConfig);

                    dbProtocolMappers.add(dbMapper);
                }

                currentClient.remove("allowedClaimsMask");

                log.debugv("Added default mappers to application {1}", currentClient.get("name"));
                clients.save(currentClient);
            }
        } finally {
            clientsCursor.close();
        }
    }

}
