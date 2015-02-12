package org.keycloak.connections.mongo.updater.updates;

import java.util.Map;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class Update1_2_0_Beta1 extends Update {

    @Override
    public String getId() {
        return "1.2.0.Beta1";
    }

    @Override
    public void update() {
        convertSocialToIdFedRealms();
        convertSocialToIdFedUsers();
    }


    private void convertSocialToIdFedRealms() {
        DBCollection realms = db.getCollection("realms");
        DBCursor realmsCursor = realms.find();
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
                    }
                }
            }

            // Remove obsolete keys from realm
            realm.remove("social");
            realm.remove("updateProfileOnInitialSocialLogin");
            realm.remove("socialConfig");

            // Update realm in DB now
            realms.save(realm);
        }
    }

    private void convertSocialToIdFedUsers() {
        DBCollection users = db.getCollection("users");
        DBCursor usersCursor = users.find();
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
            }
        }
    }
}
