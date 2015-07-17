package org.keycloak.representations.idm;

import java.io.Serializable;
import java.util.Comparator;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public class AuthenticationExecutionRepresentation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String authenticatorConfig;
    private String authenticator;
    private String flowAlias;
    private boolean autheticatorFlow;
    private String requirement;
    private boolean userSetupAllowed;
    private int priority;

    public String getAuthenticatorConfig() {
        return authenticatorConfig;
    }

    public void setAuthenticatorConfig(String authenticatorConfig) {
        this.authenticatorConfig = authenticatorConfig;
    }

    public String getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(String authenticator) {
        this.authenticator = authenticator;
    }

    public String getRequirement() {
        return requirement;
    }

    public void setRequirement(String requirement) {
        this.requirement = requirement;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isUserSetupAllowed() {
        return userSetupAllowed;
    }

    public void setUserSetupAllowed(boolean userSetupAllowed) {
        this.userSetupAllowed = userSetupAllowed;
    }

    /**
     * If this execution is a flow, this is the flowId pointing to an AuthenticationFlowModel
     *
     * @return
     */
    public String getFlowAlias() {
        return flowAlias;
    }

    public void setFlowAlias(String flowId) {
        this.flowAlias = flowId;
    }

    /**
     * Is the referenced authenticator a flow?
     *
     * @return
     */
    public boolean isAutheticatorFlow() {
        return autheticatorFlow;
    }

    public void setAutheticatorFlow(boolean autheticatorFlow) {
        this.autheticatorFlow = autheticatorFlow;
    }

}
