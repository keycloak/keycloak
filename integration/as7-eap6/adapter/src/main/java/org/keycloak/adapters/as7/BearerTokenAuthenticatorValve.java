package org.keycloak.adapters.as7;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.LoginConfig;
import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.ResourceMetadata;
import org.keycloak.adapters.as7.config.ManagedResourceConfig;
import org.keycloak.adapters.as7.config.ManagedResourceConfigLoader;

import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Uses a configured remote auth server to do Bearer token authentication only.  SkeletonKeyTokens are used
 * to provide user data and role mappings.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class BearerTokenAuthenticatorValve extends AuthenticatorBase implements LifecycleListener {
    private static final Logger log = Logger.getLogger(BearerTokenAuthenticatorValve.class);
    protected ManagedResourceConfig remoteSkeletonKeyConfig;
    protected ResourceMetadata resourceMetadata;

    @Override
    public void start() throws LifecycleException {
        super.start();
        StandardContext standardContext = (StandardContext) context;
        standardContext.addLifecycleListener(this);
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (event.getType() == Lifecycle.AFTER_START_EVENT) init();
    }

    protected void init() {
        ManagedResourceConfigLoader managedResourceConfigLoader = new ManagedResourceConfigLoader(context);
        remoteSkeletonKeyConfig = managedResourceConfigLoader.getRemoteSkeletonKeyConfig();
        managedResourceConfigLoader.init(false);
        resourceMetadata = managedResourceConfigLoader.getResourceMetadata();
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        try {
            super.invoke(request, response);
        } finally {
            ResteasyProviderFactory.clearContextData(); // to clear push of SkeletonKeySession
        }
    }

    @Override
    protected boolean authenticate(Request request, HttpServletResponse response, LoginConfig config) throws IOException {
        try {
            CatalinaBearerTokenAuthenticator bearer = new CatalinaBearerTokenAuthenticator(resourceMetadata, !remoteSkeletonKeyConfig.isCancelPropagation(), true);
            if (bearer.login(request, response)) {
                return true;
            }
            return false;
        } catch (LoginException e) {
        }
        return false;
    }
}
