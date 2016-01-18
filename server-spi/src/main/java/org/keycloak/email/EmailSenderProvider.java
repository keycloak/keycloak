package org.keycloak.email;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface EmailSenderProvider extends Provider {

    void send(RealmModel realm, UserModel user, String subject, String textBody, String htmlBody) throws EmailException;

}
