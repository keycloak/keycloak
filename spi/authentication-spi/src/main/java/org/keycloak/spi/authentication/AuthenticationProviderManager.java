package org.keycloak.spi.authentication;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.models.AuthenticationLinkModel;
import org.keycloak.models.AuthenticationProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.util.ProviderLoader;

/**
 * Access point to authentication SPI. It finds configured and available {@link AuthenticationProvider} instances for current realm
 * and then delegates method call to them.
 *
 * Example of usage: AuthenticationProviderManager.getManager(realm).validateUser("joe", "password");
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticationProviderManager {

    private static final Logger logger = Logger.getLogger(AuthenticationProviderManager.class);

    private final RealmModel realm;
    private final Map<String, AuthenticationProvider> delegates;

    public static AuthenticationProviderManager getManager(RealmModel realm) {
        Iterable<AuthenticationProvider> providers = load();

        Map<String, AuthenticationProvider> providersMap = new HashMap<String, AuthenticationProvider>();
        for (AuthenticationProvider provider : providers) {
            providersMap.put(provider.getName(), provider);
        }

        return new AuthenticationProviderManager(realm, providersMap);
    }

    private static Iterable<AuthenticationProvider> load() {
        return ProviderLoader.load(AuthenticationProvider.class);
    }

    public AuthenticationProviderManager(RealmModel realm, Map<String, AuthenticationProvider> delegates) {
        this.realm = realm;
        this.delegates = delegates;
    }

    public AuthUser getUser(String username) {
        List<AuthenticationProviderModel> authProviderModels = getConfiguredProviderModels(realm);
        for (AuthenticationProviderModel providerModel : authProviderModels) {
            AuthenticationProvider delegate = getProvider(providerModel.getProviderName());
            if (delegate == null) {
                continue;
            }

            try {
                AuthUser authUser = delegate.getUser(realm, providerModel.getConfig(), username);
                if (authUser != null) {
                    logger.debugf("User '%s' found with provider '%s'", username, providerModel.getProviderName());
                    return authUser;
                }
            } catch (AuthenticationProviderException ape) {
                logger.warn(ape.getMessage(), ape);
            }
        }

        logger.debugf("User '%s' not found with any provider", username);
        return null;
    }

    public AuthProviderStatus validatePassword(UserModel user, String password) {
        AuthenticationLinkModel authLink = realm.getAuthenticationLink(user);
        if (authLink == null) {
            authLink = new AuthenticationLinkModel(AuthenticationProviderModel.DEFAULT_PROVIDER.getProviderName(), user.getId());
        }

        String providerName = authLink.getAuthProvider();

        AuthenticationProviderModel providerModel = getConfiguredProviderModel(realm, providerName);
        AuthenticationProvider delegate = getProvider(providerName);
        if (delegate == null || providerModel == null) {
            return AuthProviderStatus.FAILED;
        }

        try {
            checkCorrectAuthLink(delegate, providerModel, authLink, user.getLoginName());

            AuthProviderStatus currentResult = delegate.validatePassword(realm, providerModel.getConfig(), user.getLoginName(), password);
            logger.debugf("Authentication provider '%s' finished with '%s' for authentication of '%s'", delegate.getName(), currentResult.toString(), user.getLoginName());
            return currentResult;
        } catch (AuthenticationProviderException ape) {
            logger.warn(ape.getMessage(), ape);
            return AuthProviderStatus.FAILED;
        }
    }

    public boolean updatePassword(UserModel user, String password) throws AuthenticationProviderException {
        AuthenticationLinkModel authLink = realm.getAuthenticationLink(user);
        if (authLink == null) {
            authLink = new AuthenticationLinkModel(AuthenticationProviderModel.DEFAULT_PROVIDER.getProviderName(), user.getId());
        }

        String providerName = authLink.getAuthProvider();

        AuthenticationProviderModel providerModel = getConfiguredProviderModel(realm, providerName);
        if (providerModel == null) {
            return false;
        }

        String username = user.getLoginName();

        // Update just those, which support password update
        if (providerModel.isPasswordUpdateSupported()) {
            try {
                AuthenticationProvider delegate = getProvider(providerName);
                if (delegate == null) {
                    return false;
                }

                checkCorrectAuthLink(delegate, providerModel, authLink, username);

                if (delegate.updateCredential(realm,providerModel.getConfig(), user.getLoginName(), password)) {
                    logger.debugf("Updated password in authentication provider '%s' for user '%s'", providerName, username);
                    return true;
                } else {
                    logger.warnf("Password not updated in authentication provider '%s' for user '%s'", providerName, username);
                    return false;
                }
            } catch (AuthenticationProviderException ape) {
                // Rethrow it to upper layer
                logger.warn("Failed to update password: " + ape.getMessage());
                throw ape;
            }
        } else {
            logger.warnf("Skip password update for authentication provider '%s' for user '%s'", providerName, username);
            return false;
        }
    }

    private AuthenticationProvider getProvider(String providerName) {
        AuthenticationProvider delegate = delegates.get(providerName);
        if (delegate == null) {
            logger.warnf("Provider '%s' not available on classpath", providerName);
        }
        return delegate;
    }

    private List<AuthenticationProviderModel> getConfiguredProviderModels(RealmModel realm) {
        List<AuthenticationProviderModel> configuredProviders = realm.getAuthenticationProviders();

        // Use model based authentication of current realm by default
        if (configuredProviders == null || configuredProviders.isEmpty()) {
            configuredProviders = Collections.EMPTY_LIST;
            logger.warnf("No authentication providers found");
        }

        return configuredProviders;
    }

    private AuthenticationProviderModel getConfiguredProviderModel(RealmModel realm, String providerName) {
        List<AuthenticationProviderModel> providers = getConfiguredProviderModels(realm);
        for (AuthenticationProviderModel provider : providers) {
            if (providerName.equals(provider.getProviderName())) {
                return provider;
            }
        }

        logger.warnf("Provider '%s' not configured in realm", providerName);
        return null;
    }

    // Check if ID of linked AuthUser is same as expected ID from authenticationLink . It should catch the case when for example user "john" was deleted in LDAP
    // and then user "john" has been created again, but it's actually different user with different ID
    private void checkCorrectAuthLink(AuthenticationProvider authProvider, AuthenticationProviderModel providerModel,
                                      AuthenticationLinkModel authLinkModel, String username) throws AuthenticationProviderException {
        AuthUser authUser = authProvider.getUser(realm, providerModel.getConfig(), username);
        String userExternalId = authUser.getId();
        if (!userExternalId.equals(authLinkModel.getAuthUserId())) {
            throw new AuthenticationProviderException("ID did not match! ID from provider: " + userExternalId + ", ID from authentication link: " + authLinkModel.getAuthUserId());
        }
    }
}
