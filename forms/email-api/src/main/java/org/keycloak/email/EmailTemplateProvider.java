package org.keycloak.email;

import org.keycloak.events.Event;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface EmailTemplateProvider extends Provider {

    String IDENTITY_PROVIDER_BROKER_CONTEXT = "identityProviderBrokerCtx";

    public EmailTemplateProvider setRealm(RealmModel realm);

    public EmailTemplateProvider setUser(UserModel user);

    public EmailTemplateProvider setAttribute(String name, Object value);

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
     * Send to confirm that user wants to link his account with identity broker link
     */
    void sendConfirmIdentityBrokerLink(String link, long expirationInMinutes) throws EmailException;

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
