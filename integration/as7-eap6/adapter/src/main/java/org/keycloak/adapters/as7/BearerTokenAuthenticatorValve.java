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
import org.keycloak.adapters.ResourceMetadata;
import org.keycloak.adapters.as7.config.CatalinaAdapterConfigLoader;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.adapters.config.AdapterConfigLoader;

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
    protected AdapterConfig adapterConfig;
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
        AdapterConfigLoader adapterConfigLoader = new CatalinaAdapterConfigLoader(context);
        adapterConfig = adapterConfigLoader.getAdapterConfig();
        adapterConfigLoader.init();
        resourceMetadata = adapterConfigLoader.getResourceMetadata();
        AuthenticatedActionsValve actions = new AuthenticatedActionsValve(adapterConfig, getNext(), getContainer(), getController());
        setNext(actions);
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        try {
            log.debugv("{0} {1}", request.getMethod(), request.getRequestURI());
            if (adapterConfig.isCors() && new CorsPreflightChecker(adapterConfig).checkCorsPreflight(request, response)) {
                return;
            }
            super.invoke(request, response);
        } finally {
        }
    }

    @Override
    protected boolean authenticate(Request request, HttpServletResponse response, LoginConfig config) throws IOException {
        try {
            CatalinaBearerTokenAuthenticator bearer = new CatalinaBearerTokenAuthenticator(resourceMetadata, true, adapterConfig.isUseResourceRoleMappings());
            if (bearer.login(request, response)) {
                return true;
            }
            return false;
        } catch (LoginException e) {
        }
        return false;
    }
}
