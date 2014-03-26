package org.keycloak.spi.authentication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.models.AuthenticationProviderModel;
import org.keycloak.models.RealmModel;
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
    private static final AuthenticationProviderModel DEFAULT_PROVIDER = new AuthenticationProviderModel(AuthProviderConstants.PROVIDER_NAME_MODEL, true, Collections.EMPTY_MAP);

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

    public AuthResult validatePassword(String username, String password) {
        List<AuthenticationProviderModel> configuredProviders = getConfiguredProviders(realm);
        boolean userExists = false;

        for (AuthenticationProviderModel authProviderConfig : configuredProviders) {
            String providerName = authProviderConfig.getProviderName();

            AuthenticationProvider delegate = getDelegate(providerName);
            if (delegate == null) {
                continue;
            }

            try {
                AuthResult currentResult = delegate.validatePassword(realm, authProviderConfig.getConfig(), username, password);
                logger.debugf("Authentication provider '%s' finished with '%s' for authentication of '%s'", delegate.getName(), currentResult.getAuthProviderStatus().toString(), username);

                if (currentResult.getAuthProviderStatus() == AuthProviderStatus.SUCCESS) {
                    return currentResult;
                } else if (currentResult.getAuthProviderStatus() == AuthProviderStatus.INVALID_CREDENTIALS) {
                    userExists = true;
                }
            } catch (AuthenticationProviderException ape) {
                logger.warn(ape.getMessage(), ape);
            }
        }

        AuthProviderStatus status = userExists ? AuthProviderStatus.INVALID_CREDENTIALS : AuthProviderStatus.USER_NOT_FOUND;
        logger.debugf("Not able to authenticate '%s' with any authentication provider. Status: '%s'", username, status.toString());

        return new AuthResult(status);
    }

    public void updatePassword(String username, String password) throws AuthenticationProviderException {
        List<AuthenticationProviderModel> configuredProviders = getConfiguredProviders(realm);

        for (AuthenticationProviderModel authProviderConfig : configuredProviders) {

            // Update just those, which support password update
            if (authProviderConfig.isPasswordUpdateSupported()) {
                String providerName = authProviderConfig.getProviderName();
                AuthenticationProvider delegate = getDelegate(providerName);
                if (delegate == null) {
                    continue;
                }

                try {
                    if (delegate.updateCredential(realm, authProviderConfig.getConfig(), username, password)) {
                        logger.debugf("Updated password in authentication provider '%s' for user '%s'", delegate.getName(), username);
                    } else {
                        logger.debugf("Password not updated in authentication provider '%s' for user '%s'", delegate.getName(), username);
                    }
                } catch (AuthenticationProviderException ape) {
                    // Rethrow it to upper layer
                    logger.warn("Failed to update password: " + ape.getMessage());
                    throw ape;
                }
            } else {
                logger.debugf("Skip password update for authentication provider '%s' for user '%s'", authProviderConfig.getProviderName(), username);
            }
        }
    }

    private AuthenticationProvider getDelegate(String providerName) {
        AuthenticationProvider delegate = delegates.get(providerName);
        if (delegate == null) {
            logger.warnf("Configured provider with name '%s' not found", providerName);
        }
        return delegate;
    }

    private List<AuthenticationProviderModel> getConfiguredProviders(RealmModel realm) {
        List<AuthenticationProviderModel> configuredProviders = realm.getAuthenticationProviders();

        // Use model based authentication of current realm by default
        if (configuredProviders == null || configuredProviders.isEmpty()) {
            configuredProviders = new ArrayList<AuthenticationProviderModel>();
            configuredProviders.add(DEFAULT_PROVIDER);
        }

        return configuredProviders;
    }
}
