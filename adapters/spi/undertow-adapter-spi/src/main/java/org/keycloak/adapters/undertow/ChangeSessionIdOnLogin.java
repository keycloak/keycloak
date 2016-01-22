package org.keycloak.adapters.undertow;

import io.undertow.servlet.api.DeploymentInfo;

import java.lang.reflect.Method;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ChangeSessionIdOnLogin {
    /**
     * This is a hack to be backward compatible between Undertow 1.3+ and versions lower.  In Undertow 1.3, a new
     * switch was added setChangeSessionIdOnLogin, this screws up session management for keycloak as after the session id
     * is uploaded to Keycloak, undertow changes the session id and it can't be invalidated.
     *
     * @param deploymentInfo
     */
    public static void turnOffChangeSessionIdOnLogin(DeploymentInfo deploymentInfo) {
        try {
            Method method = DeploymentInfo.class.getMethod("setChangeSessionIdOnLogin", boolean.class);
            method.invoke(deploymentInfo, false);
        } catch (Exception ignore) {

        }
    }
}
