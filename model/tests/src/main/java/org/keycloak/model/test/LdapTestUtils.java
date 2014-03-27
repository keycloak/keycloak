package org.keycloak.model.test;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.spi.authentication.picketlink.PicketlinkAuthenticationProvider;
import org.keycloak.util.KeycloakRegistry;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.User;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LdapTestUtils {

    public static void setLdapPassword(RealmModel realm, String username, String password) {
        // TODO: Workaround... should be improved once we have KeycloakSession with available application-scoped components
        KeycloakRegistry registry = ResteasyProviderFactory.getContextData(KeycloakRegistry.class);
        if (registry == null) {
            ResteasyProviderFactory.pushContext(KeycloakRegistry.class, new KeycloakRegistry());
        }

        // Update password directly in ldap. It's workaround, but LDIF import doesn't seem to work on windows for ApacheDS
        try {
            IdentityManager identityManager = new PicketlinkAuthenticationProvider().getIdentityManager(realm);
            User user = BasicModel.getUser(identityManager, username);
            identityManager.updateCredential(user, new Password(password.toCharArray()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
