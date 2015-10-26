package org.keycloak.services.scheduled;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.lang.invoke.MethodHandles;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Removes expired users who didn't verify their email addresses.
 */
public class ClearExpiredUserRegistrations implements ScheduledTask {

    public static final String CONFIGURATION_NAMESPACE = "user";
    public static final String ENABLED_KEY = "enableExpiredUsersEviction";
    public static final String EXPIRATION_KEY = "expiredUsersEvictionTimeInDays";

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void run(KeycloakSession session) {
        if(isEnabled()) {
            Date expirationTime = getExpirationTime();
            for(RealmModel realmModel : session.realms().getRealms()) {
                logger.debug("Clearing expired user registrations (realm: " + realmModel.getName() + ", expirationTime: " + expirationTime + ")");
                List<UserModel> expiredUsers = session.userStorage().searchForExpiredUsers(expirationTime, realmModel);
                for(UserModel user : expiredUsers) {
                    logger.debug("Removing expired user: " + user.getUsername());
                    session.userStorage().removeUser(realmModel, user);
                }
            }
        }
    }

    /**
     * @return <code>true</code> is eviction mechanism is enabled.
     */
    public boolean isEnabled() {
        return Config.scope(CONFIGURATION_NAMESPACE).getBoolean(ENABLED_KEY, true);
    }

    /**
     * @return Expiration date for evicted users.
     */
    public Date getExpirationTime() {
        int daysToExpire = Config.scope(CONFIGURATION_NAMESPACE).getInt(EXPIRATION_KEY, 30);
        Calendar expiredBefore = Calendar.getInstance();
        expiredBefore.add(Calendar.DATE, -daysToExpire);
        return expiredBefore.getTime();
    }
}
