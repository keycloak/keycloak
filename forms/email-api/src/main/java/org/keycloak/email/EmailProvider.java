package org.keycloak.email;

import org.keycloak.events.Event;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface EmailProvider extends Provider {

    public EmailProvider setRealm(RealmModel realm);

    public EmailProvider setUser(UserModel user);

    public void sendEvent(Event event) throws EmailException;

    /**
     * Reset password sent from forgot password link on login
     *
     * @param link
     * @param expirationInMinutes
     * @throws EmailException
     */
    public void sendPasswordReset(String link, long expirationInMinutes) throws EmailException;

    /**
     * Change password email requested by admin
     *
     * @param link
     * @param expirationInMinutes
     * @throws EmailException
     */
    public void sendExecuteActions(String link, long expirationInMinutes) throws EmailException;

    public void sendVerifyEmail(String link, long expirationInMinutes) throws EmailException;

}
