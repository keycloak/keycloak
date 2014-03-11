package org.keycloak.adapters.undertow;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMechanismFactory;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.AuthMethodConfig;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.servlet.api.ServletSessionConfig;
import java.io.ByteArrayInputStream;
import org.jboss.logging.Logger;
import org.keycloak.adapters.AdapterConstants;
import org.keycloak.adapters.AuthenticatedActionsHandler;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;

import javax.servlet.ServletContext;
import java.io.InputStream;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakServletExtension implements ServletExtension {

    protected static Logger log = Logger.getLogger(KeycloakServletExtension.class);

    // todo when this DeploymentInfo method of the same name is fixed.
    public boolean isAuthenticationMechanismPresent(DeploymentInfo deploymentInfo, final String mechanismName) {
        LoginConfig loginConfig = deploymentInfo.getLoginConfig();
        if(loginConfig != null) {
            for(AuthMethodConfig method : loginConfig.getAuthMethods()) {
                if(method.getName().equalsIgnoreCase(mechanismName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private InputStream getJSONFromServletContext(ServletContext servletContext) {
        String json = servletContext.getInitParameter(AdapterConstants.AUTH_DATA_PARAM_NAME);
        if (json == null) {
            return null;
        }
        return new ByteArrayInputStream(json.getBytes());
    }

    @Override
    public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
        if (!isAuthenticationMechanismPresent(deploymentInfo, "KEYCLOAK")) {
            log.info("auth-method is not keycloak!");
            return;
        }
        log.info("KeycloakServletException initialization");
        InputStream is = getJSONFromServletContext(servletContext);
        if (is == null) {
            is = servletContext.getResourceAsStream("/WEB-INF/keycloak.json");
        }
        if (is == null) throw new RuntimeException("Unable to find realm config in /WEB-INF/keycloak.json or in keycloak subsystem.");
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(is);
        UndertowUserSessionManagement userSessionManagement = new UndertowUserSessionManagement(deployment);
        final ServletKeycloakAuthMech mech = createAuthenticationMechanism(deploymentInfo, deployment, userSessionManagement);

        UndertowAuthenticatedActionsHandler.Wrapper actions = new UndertowAuthenticatedActionsHandler.Wrapper(deployment);

        // setup handlers

        deploymentInfo.addOuterHandlerChainWrapper(new ServletPreAuthActionsHandler.Wrapper(deployment, userSessionManagement));
        deploymentInfo.addAuthenticationMechanism("KEYCLOAK", new AuthenticationMechanismFactory() {
            @Override
            public AuthenticationMechanism create(String s, FormParserFactory formParserFactory, Map<String, String> stringStringMap) {
                return mech;
            }
        }); // authentication
        deploymentInfo.addInnerHandlerChainWrapper(actions); // handles authenticated actions and cors.

        deploymentInfo.setIdentityManager(new IdentityManager() {
            @Override
            public Account verify(Account account) {
                return account;
            }

            @Override
            public Account verify(String id, Credential credential) {
                throw new IllegalStateException("Should never be called in Keycloak flow");
            }

            @Override
            public Account verify(Credential credential) {
                throw new IllegalStateException("Should never be called in Keycloak flow");
            }
        });

        log.info("Setting jsession cookie path to: " + deploymentInfo.getContextPath());
        ServletSessionConfig cookieConfig = new ServletSessionConfig();
        cookieConfig.setPath(deploymentInfo.getContextPath());
        deploymentInfo.setServletSessionConfig(cookieConfig);
    }

    protected ServletKeycloakAuthMech createAuthenticationMechanism(DeploymentInfo deploymentInfo, KeycloakDeployment deployment, UndertowUserSessionManagement userSessionManagement) {
       log.info("creating ServletKeycloakAuthMech");
       return new ServletKeycloakAuthMech(deployment, userSessionManagement, deploymentInfo.getConfidentialPortManager());
    }
}
