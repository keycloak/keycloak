package org.keycloak.examples.providers.authentication;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthProviderStatus;
import org.keycloak.authentication.AuthUser;
import org.keycloak.authentication.AuthenticationProvider;
import org.keycloak.authentication.AuthenticationProviderException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PropertiesAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = Logger.getLogger(PropertiesAuthenticationProvider.class);

    private final Properties properties;

    public PropertiesAuthenticationProvider(Properties properties) {
        this.properties = properties;
    }

    @Override
    public String getName() {
        return "properties";
    }

    @Override
    public List<String> getAvailableOptions() {
        return Collections.emptyList();
    }

    @Override
    public AuthUser getUser(RealmModel realm, Map<String, String> configuration, String username) throws AuthenticationProviderException {
        if (properties.getProperty(username) != null) {
            return new AuthUser(username, username, getName());
        } else {
            return null;
        }
    }

    @Override
    public String registerUser(RealmModel realm, Map<String, String> configuration, UserModel user) throws AuthenticationProviderException {
        // Registration ignored
        return user.getLoginName();
    }

    @Override
    public AuthProviderStatus validatePassword(RealmModel realm, Map<String, String> configuration, String username, String password) throws AuthenticationProviderException {
        String propertyFilePassword = properties.getProperty(username);
        if (propertyFilePassword != null && propertyFilePassword.equals(password)) {
            return AuthProviderStatus.SUCCESS;
        } else {
            return AuthProviderStatus.INVALID_CREDENTIALS;
        }
    }

    @Override
    public boolean updateCredential(RealmModel realm, Map<String, String> configuration, String username, String password) throws AuthenticationProviderException {
        log.info("Going to update password for user " + username + " in PropertiesAuthenticationProvider");
        properties.put(username, password);
        return true;
    }

    @Override
    public void close() {
    }

}
