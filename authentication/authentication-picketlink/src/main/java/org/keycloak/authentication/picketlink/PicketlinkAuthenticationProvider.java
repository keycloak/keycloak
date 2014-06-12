package org.keycloak.authentication.picketlink;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthProviderConstants;
import org.keycloak.authentication.AuthProviderStatus;
import org.keycloak.authentication.AuthUser;
import org.keycloak.authentication.AuthenticationProvider;
import org.keycloak.authentication.AuthenticationProviderException;
import org.keycloak.models.RealmModel;
import org.keycloak.picketlink.IdentityManagerProvider;
import org.picketlink.idm.IdentityManagementException;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.credential.Credentials;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.credential.UsernamePasswordCredentials;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.User;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AuthenticationProvider, which delegates authentication to picketlink
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PicketlinkAuthenticationProvider implements AuthenticationProvider {

    private static final Logger logger = Logger.getLogger(PicketlinkAuthenticationProvider.class);

    private final IdentityManagerProvider identityManagerProvider;

    public PicketlinkAuthenticationProvider(IdentityManagerProvider identityManagerProvider) {
        this.identityManagerProvider = identityManagerProvider;
    }

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
    public String registerUser(RealmModel realm, Map<String, String> configuration, String username) throws AuthenticationProviderException {
        IdentityManager identityManager = getIdentityManager(realm);

        try {
            User picketlinkUser = new User(username);
            identityManager.add(picketlinkUser);

            // Hack needed due to ActiveDirectory bug in Picketlink TODO: Remove once https://issues.jboss.org/browse/PLINK-485 fixed and updated in keycloak master
            picketlinkUser = BasicModel.getUser(identityManager, picketlinkUser.getLoginName());

            return picketlinkUser.getId();
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

    @Override
    public void close() {
    }

    public IdentityManager getIdentityManager(RealmModel realm) throws AuthenticationProviderException {
        return identityManagerProvider.getIdentityManager(realm);
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
