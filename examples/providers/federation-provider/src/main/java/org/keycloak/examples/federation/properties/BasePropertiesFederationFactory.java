package org.keycloak.examples.federation.properties;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class BasePropertiesFederationFactory implements UserFederationProviderFactory {
    static final Set<String> configOptions = new HashSet<String>();
    protected ConcurrentHashMap<String, Properties> files = new ConcurrentHashMap<String, Properties>();

    static {
        configOptions.add("path");
    }

    @Override
    public UserFederationProvider getInstance(KeycloakSession session, UserFederationProviderModel model) {
        // first get the path to our properties file from the stored configuration of this provider instance.
        String path = model.getConfig().get("path");
        if (path == null) {
            throw new IllegalStateException("Path attribute not configured for provider");
        }
        // see if we already loaded the config file
        Properties props = files.get(path);
        if (props != null) return createProvider(session, model, props);


        props = new Properties();
        InputStream is = getPropertiesFileStream(path);
        try {
            props.load(is);
            is.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // remember the properties file for next time
        files.put(path, props);
        return createProvider(session, model, props);
    }

    protected abstract InputStream getPropertiesFileStream(String path);

    protected abstract BasePropertiesFederationProvider createProvider(KeycloakSession session, UserFederationProviderModel model, Properties props);

    /**
     * List the configuration options to render and display in the admin console's generic management page for this
     * plugin
     *
     * @return
     */
    @Override
    public Set<String> getConfigurationOptions() {
        return configOptions;
    }

    @Override
    public UserFederationProvider create(KeycloakSession session) {
        return null;
    }

    /**
     * You can import additional plugin configuration from keycloak-server.json here.
     *
     * @param config
     */
    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void close() {

    }

    @Override
    public void syncAllUsers(KeycloakSessionFactory sessionFactory, final String realmId, final UserFederationProviderModel model) {
        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                RealmModel realm = session.realms().getRealm(realmId);
                BasePropertiesFederationProvider federationProvider = (BasePropertiesFederationProvider)getInstance(session, model);
                Set<String> allUsernames = federationProvider.getProperties().stringPropertyNames();
                UserProvider localProvider = session.userStorage();
                for (String username : allUsernames) {
                    UserModel localUser = localProvider.getUserByUsername(username, realm);

                    if (localUser == null) {
                        // New user, let's import him
                        federationProvider.getUserByUsername(realm, username);
                    }
                }
            }

        });
    }

    @Override
    public void syncChangedUsers(KeycloakSessionFactory sessionFactory, final String realmId, final UserFederationProviderModel model, Date lastSync) {
        syncAllUsers(sessionFactory, realmId, model);
    }
}
