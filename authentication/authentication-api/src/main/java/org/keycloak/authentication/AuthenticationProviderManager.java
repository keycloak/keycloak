package org.keycloak.authentication;

import org.jboss.logging.Logger;
import org.keycloak.models.AuthenticationLinkModel;
import org.keycloak.models.AuthenticationProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static AuthenticationProviderManager getManager(RealmModel realm, KeycloakSession session) {
        Iterable<AuthenticationProvider> providers = session.getAllProviders(AuthenticationProvider.class);

        Map<String, AuthenticationProvider> providersMap = new HashMap<String, AuthenticationProvider>();
        for (AuthenticationProvider provider : providers) {
            providersMap.put(provider.getName(), provider);
        }

        return new AuthenticationProviderManager(realm, providersMap);
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
        AuthenticationLinkModel authLink = user.getAuthenticationLink();
        if (authLink == null) {
            // User not yet linked with any authenticationProvider. Find provider with biggest priority where he is and link
            AuthUser authUser = getUser(user.getUsername());
            authLink = new AuthenticationLinkModel(authUser.getProviderName(), authUser.getId());
            user.setAuthenticationLink(authLink);
            logger.infof("User '%s' linked with provider '%s'", authUser.getUsername(), authUser.getProviderName());
        }

        String providerName = authLink.getAuthProvider();

        AuthenticationProviderModel providerModel = getConfiguredProviderModel(realm, providerName);
        AuthenticationProvider delegate = getProvider(providerName);
        if (delegate == null || providerModel == null) {
            return AuthProviderStatus.FAILED;
        }

        try {
            checkCorrectAuthLink(delegate, providerModel, authLink, user.getUsername());

            AuthProviderStatus currentResult = delegate.validatePassword(realm, providerModel.getConfig(), user.getUsername(), password);
            logger.debugf("Authentication provider '%s' finished with '%s' for authentication of '%s'", delegate.getName(), currentResult.toString(), user.getUsername());
            return currentResult;
        } catch (AuthenticationProviderException ape) {
            logger.warn(ape.getMessage(), ape);
            return AuthProviderStatus.FAILED;
        }
    }

    public boolean updatePassword(UserModel user, String password) throws AuthenticationProviderException {
        AuthenticationLinkModel authLink = user.getAuthenticationLink();
        if (authLink == null) {
            // Find provider with biggest priority where password update is supported. Then register user here and link him
            List<AuthenticationProviderModel> configuredProviders = getConfiguredProviderModels(realm);
            for (AuthenticationProviderModel providerModel : configuredProviders) {
                if (providerModel.isPasswordUpdateSupported()) {
                    AuthenticationProvider delegate = getProvider(providerModel.getProviderName());
                    if (delegate != null) {
                        AuthUser authUser = delegate.getUser(realm, providerModel.getConfig(), user.getUsername());
                        if (authUser != null) {
                            // Linking existing user supported just for "model" provider. In other cases throw exception
                            if (providerModel.getProviderName().equals(AuthenticationProviderModel.DEFAULT_PROVIDER.getProviderName())) {
                                authLink = new AuthenticationLinkModel(providerModel.getProviderName(), authUser.getId());
                                user.setAuthenticationLink(authLink);
                                logger.infof("User '%s' linked with provider '%s'", authUser.getUsername(), authUser.getProviderName());
                            } else {
                                throw new AuthenticationProviderException("User " + authUser.getUsername() + " exists in provider "
                                        + authUser.getProviderName() + " but is not linked with model user");
                            }
                        } else {
                            String userIdInProvider = delegate.registerUser(realm, providerModel.getConfig(), user);
                            authLink = new AuthenticationLinkModel(providerModel.getProviderName(), userIdInProvider);
                            user.setAuthenticationLink(authLink);
                            logger.infof("User '%s' registered in provider '%s' and linked", user.getUsername(), providerModel.getProviderName());
                        }
                        break;
                    }
                }
            }

            if (authLink == null) {
                logger.warnf("No providers found where password update is supported for user '%s'", user.getUsername());
                return false;
            }
        }

        String providerName = authLink.getAuthProvider();

        AuthenticationProviderModel providerModel = getConfiguredProviderModel(realm, providerName);
        if (providerModel == null) {
            return false;
        }

        String username = user.getUsername();

        // Update just if password update is supported
        if (providerModel.isPasswordUpdateSupported()) {
            try {
                AuthenticationProvider delegate = getProvider(providerName);
                if (delegate == null) {
                    return false;
                }

                checkCorrectAuthLink(delegate, providerModel, authLink, username);

                if (delegate.updateCredential(realm,providerModel.getConfig(), user.getUsername(), password)) {
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

    private static List<AuthenticationProviderModel> getConfiguredProviderModels(RealmModel realm) {
        List<AuthenticationProviderModel> configuredProviders = realm.getAuthenticationProviders();

        // Use model based authentication of current realm by default
        if (configuredProviders == null || configuredProviders.isEmpty()) {
            configuredProviders = Collections.EMPTY_LIST;
            logger.warnf("No authentication providers found");
        }

        return configuredProviders;
    }

    public static AuthenticationProviderModel getConfiguredProviderModel(RealmModel realm, String providerName) {
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
        if (authUser == null) {
            throw new AuthenticationProviderException("User " + username + " not found in authentication provider " + providerModel.getProviderName());
        }
        String userExternalId = authUser.getId();
        if (!userExternalId.equals(authLinkModel.getAuthUserId())) {
            throw new AuthenticationProviderException("ID did not match! ID from provider: " + userExternalId + ", ID from authentication link: " + authLinkModel.getAuthUserId());
        }
    }
}
