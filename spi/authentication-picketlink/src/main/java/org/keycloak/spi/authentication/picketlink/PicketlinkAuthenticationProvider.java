package org.keycloak.spi.authentication.picketlink;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.spi.authentication.AuthProviderStatus;
import org.keycloak.spi.authentication.AuthProviderConstants;
import org.keycloak.spi.authentication.AuthUser;
import org.keycloak.spi.authentication.AuthenticationProvider;
import org.keycloak.spi.authentication.AuthenticationProviderException;
import org.keycloak.spi.picketlink.PartitionManagerProvider;
import org.keycloak.util.ProviderLoader;
import org.picketlink.idm.IdentityManagementException;
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
    public List<String> getAvailableOptions() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public AuthUser getUser(RealmModel realm, Map<String, String> configuration, String username) throws AuthenticationProviderException {
        IdentityManager identityManager = getIdentityManager(realm);

        try {
            User picketlinkUser = BasicModel.getUser(identityManager, username);
            return picketlinkUser == null ? null : new AuthUser(picketlinkUser.getId(), picketlinkUser.getLoginName(), getName())
                    .setName(picketlinkUser.getFirstName(), picketlinkUser.getLastName())
                    .setEmail(picketlinkUser.getEmail())
                    .setProviderName(getName());
        } catch (IdentityManagementException ie) {
            throw convertIDMException(ie);
        }
    }

    @Override
    public AuthProviderStatus validatePassword(RealmModel realm, Map<String, String> configuration, String username, String password) throws AuthenticationProviderException {
        IdentityManager identityManager = getIdentityManager(realm);

        try {
            UsernamePasswordCredentials credential = new UsernamePasswordCredentials();
            credential.setUsername(username);
            credential.setPassword(new Password(password.toCharArray()));
            identityManager.validateCredentials(credential);
            if (credential.getStatus() == Credentials.Status.VALID) {
                return AuthProviderStatus.SUCCESS;
            } else {
                return AuthProviderStatus.INVALID_CREDENTIALS;
            }
        } catch (IdentityManagementException ie) {
            throw convertIDMException(ie);
        }
    }

    @Override
    public boolean updateCredential(RealmModel realm, Map<String, String> configuration, String username, String password) throws AuthenticationProviderException {
        IdentityManager identityManager = getIdentityManager(realm);

        try {
            User picketlinkUser = BasicModel.getUser(identityManager, username);
            if (picketlinkUser == null) {
                logger.debugf("User '%s' doesn't exists. Skip password update", username);
                return false;
            }

            identityManager.updateCredential(picketlinkUser, new Password(password.toCharArray()));
            return true;
        } catch (IdentityManagementException ie) {
            throw convertIDMException(ie);
        }
    }

    public IdentityManager getIdentityManager(RealmModel realm) throws AuthenticationProviderException {
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

    private AuthenticationProviderException convertIDMException(IdentityManagementException ie) {
        Throwable realCause = ie;
        while (realCause.getCause() != null) {
            realCause = realCause.getCause();
        }

        // Use the message from the realCause
        return new AuthenticationProviderException(realCause.getMessage(), ie);
    }
}
