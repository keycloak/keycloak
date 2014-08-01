package org.keycloak.testsuite;

import org.keycloak.federation.ldap.PartitionManagerRegistry;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.picketlink.IdentityManagerProvider;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.User;

import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPTestUtils {

    public static void setLdapPassword(Map<String, String> ldapConfig, String username, String password) {
        // Update password directly in ldap. It's workaround, but LDIF import doesn't seem to work on windows for ApacheDS
        try {
            PartitionManager partitionManager = PartitionManagerRegistry.createPartitionManager(ldapConfig);
            IdentityManager identityManager = partitionManager.createIdentityManager();
            User user = BasicModel.getUser(identityManager, username);
            identityManager.updateCredential(user, new Password(password.toCharArray()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
