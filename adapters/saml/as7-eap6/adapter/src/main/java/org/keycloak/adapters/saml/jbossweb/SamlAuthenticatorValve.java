package org.keycloak.adapters.saml.jbossweb;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.LoginConfig;
import org.keycloak.adapters.jbossweb.JBossWebPrincipalFactory;
import org.keycloak.adapters.saml.AbstractSamlAuthenticatorValve;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.tomcat.GenericPrincipalFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Keycloak authentication valve
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlAuthenticatorValve extends AbstractSamlAuthenticatorValve {
    public boolean authenticate(Request request, HttpServletResponse response, LoginConfig config) throws java.io.IOException {
        return authenticateInternal(request, response, config);
    }

    @Override
    protected boolean forwardToErrorPageInternal(Request request, HttpServletResponse response, Object loginConfig) throws IOException {
        if (loginConfig == null) return false;
        LoginConfig config = (LoginConfig)loginConfig;
        if (config.getErrorPage() == null) return false;
        forwardToErrorPage(request, (Response)response, config);
        return true;
    }

    @Override
    protected void forwardToLogoutPage(Request request, HttpServletResponse response, SamlDeployment deployment) {
        super.forwardToLogoutPage(request, response, deployment);
    }

    @Override
    public void start() throws LifecycleException {
        StandardContext standardContext = (StandardContext) context;
        standardContext.addLifecycleListener(this);
        super.start();
    }


    public void logout(Request request) {
        logoutInternal(request);
    }

    @Override
    protected GenericPrincipalFactory createPrincipalFactory() {
        return new JBossWebPrincipalFactory();
    }
}
