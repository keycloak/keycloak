package org.keycloak.adapters.saml.wildfly;

import io.undertow.servlet.api.DeploymentInfo;
import org.keycloak.adapters.saml.SamlDeploymentContext;
import org.keycloak.adapters.saml.undertow.SamlServletExtension;
import org.keycloak.adapters.saml.undertow.ServletSamlAuthMech;
import org.keycloak.adapters.undertow.UndertowUserSessionManagement;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class WildflySamlExtension extends SamlServletExtension {
    @Override
    protected ServletSamlAuthMech createAuthMech(DeploymentInfo deploymentInfo, SamlDeploymentContext deploymentContext, UndertowUserSessionManagement userSessionManagement) {
        return new WildflySamlAuthMech(deploymentContext, userSessionManagement, getErrorPage(deploymentInfo));
    }
}
