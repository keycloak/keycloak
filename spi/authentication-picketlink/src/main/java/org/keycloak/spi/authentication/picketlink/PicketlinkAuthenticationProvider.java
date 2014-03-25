package org.keycloak.spi.authentication.picketlink;

import java.util.Map;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.spi.authentication.AuthProviderStatus;
import org.keycloak.spi.authentication.AuthResult;
import org.keycloak.spi.authentication.AuthProviderConstants;
import org.keycloak.spi.authentication.AuthenticatedUser;
import org.keycloak.spi.authentication.AuthenticationProvider;
import org.keycloak.spi.authentication.AuthenticationProviderException;
import org.keycloak.spi.picketlink.PartitionManagerProvider;
import org.keycloak.util.ProviderLoader;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.credential.Credentials;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.credential.UsernamePasswordCredentials;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.User;

/**
 * AuthenticationProvider, which delegates authentication to picketlink
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PicketlinkAuthenticationProvider implements AuthenticationProvider {

    private static final Logger logger = Logger.getLogger(PicketlinkAuthenticationProvider.class);

    @Override
    public String getName() {
        return AuthProviderConstants.PROVIDER_NAME_PICKETLINK;
    }

    @Override
    public AuthResult validatePassword(RealmModel realm, Map<String, String> configuration, String username, String password) throws AuthenticationProviderException {
        IdentityManager identityManager = getIdentityManager(realm);

        UsernamePasswordCredentials credential = new UsernamePasswordCredentials();
        credential.setUsername(username);
        credential.setPassword(new Password(password.toCharArray()));
        identityManager.validateCredentials(credential);

        AuthResult result;
        if (credential.getStatus() == Credentials.Status.VALID) {
            result = new AuthResult(AuthProviderStatus.SUCCESS);

            User picketlinkUser = BasicModel.getUser(identityManager, username);
            AuthenticatedUser authenticatedUser = new AuthenticatedUser(picketlinkUser.getId(), picketlinkUser.getLoginName())
                    .setName(picketlinkUser.getFirstName(), picketlinkUser.getLastName())
                    .setEmail(picketlinkUser.getEmail());
            result.setUser(authenticatedUser).setProviderName(getName());
            return result;
        } else {
            return new AuthResult(AuthProviderStatus.IGNORE);
        }
    }

    @Override
    public boolean updateCredential(RealmModel realm, Map<String, String> configuration, String username, String password) throws AuthenticationProviderException {
        IdentityManager identityManager = getIdentityManager(realm);

        User picketlinkUser = BasicModel.getUser(identityManager, username);
        if (picketlinkUser == null) {
            logger.debugf("User '%s' doesn't exists. Skip password update", username);
            return false;
        }

        identityManager.updateCredential(picketlinkUser, new Password(password.toCharArray()));
        return true;
    }

    protected IdentityManager getIdentityManager(RealmModel realm) throws AuthenticationProviderException {
        IdentityManager identityManager = ResteasyProviderFactory.getContextData(IdentityManager.class);
        if (identityManager == null) {
            Iterable<PartitionManagerProvider> providers = ProviderLoader.load(PartitionManagerProvider.class);

            // TODO: Priority?
            PartitionManager partitionManager = null;
            for (PartitionManagerProvider provider : providers) {
                partitionManager = provider.getPartitionManager(realm);
                if (partitionManager != null) {
                    break;
                }
            }

            if (partitionManager == null) {
                throw new AuthenticationProviderException("Not able to locate PartitionManager with any PartitionManagerProvider");
            }

            identityManager = partitionManager.createIdentityManager();
            ResteasyProviderFactory.pushContext(IdentityManager.class, identityManager);
        }
        return identityManager;
    }
}
