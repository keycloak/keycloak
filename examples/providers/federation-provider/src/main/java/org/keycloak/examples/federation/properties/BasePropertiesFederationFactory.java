package org.keycloak.examples.federation.properties;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;

import java.io.IOException;
import java.io.InputStream;
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
        String path = model.getConfig().get("path");
        if (path == null) {
            throw new IllegalStateException("Path attribute not configured for provider");
        }
        Properties props = files.get(path);
        if (props != null) return createProvider(session, model, props);


        props = new Properties();
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (is == null) {
            throw new IllegalStateException("Path attribute not configured for provider");

        }
        try {
            props.load(is);
            is.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        props.put(path, props);
        return createProvider(session, model, props);
    }

    protected abstract BasePropertiesFederationProvider createProvider(KeycloakSession session, UserFederationProviderModel model, Properties props);


    @Override
    public Set<String> getConfigurationOptions() {
        return configOptions;
    }

    @Override
    public UserFederationProvider create(KeycloakSession session) {
        return null;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void close() {

    }
}
