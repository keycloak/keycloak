package org.keycloak.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.PreMatching;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.constants.GenericConstants;
import org.osgi.framework.BundleContext;

/**
 * Variant of JaxrsBearerTokenFilter, which can be used to properly use resources from current osgi bundle
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class OsgiJaxrsBearerTokenFilterImpl extends JaxrsBearerTokenFilterImpl {

    private final static Logger log = Logger.getLogger("" + JaxrsBearerTokenFilterImpl.class);

    private BundleContext bundleContext;

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        attemptStart();
    }

    @Override
    protected boolean isInitialized() {
        return super.isInitialized() && bundleContext != null;
    }

    @Override
    protected Class<? extends KeycloakConfigResolver> loadResolverClass() {
        String resolverClass = getKeycloakConfigResolverClass();
        try {
            return (Class<? extends KeycloakConfigResolver>) bundleContext.getBundle().loadClass(resolverClass);
        } catch (ClassNotFoundException cnfe) {
            log.warning("Not able to find class from bundleContext. Fallback to current classloader");
            return super.loadResolverClass();
        }
    }

    @Override
    protected InputStream loadKeycloakConfigFile() {
        String keycloakConfigFile = getKeycloakConfigFile();
        if (keycloakConfigFile.startsWith(GenericConstants.PROTOCOL_CLASSPATH)) {

            // Load from classpath of current bundle
            String classPathLocation = keycloakConfigFile.replace(GenericConstants.PROTOCOL_CLASSPATH, "");
            log.fine("Loading config from classpath on location: " + classPathLocation);

            URL cfgUrl = bundleContext.getBundle().getResource(classPathLocation);
            if (cfgUrl == null) {
                log.warning("Not able to find configFile from bundleContext. Fallback to current classloader");
                return super.loadKeycloakConfigFile();
            }

            try {
                return cfgUrl.openStream();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        } else {
            return super.loadKeycloakConfigFile();
        }
    }
}
