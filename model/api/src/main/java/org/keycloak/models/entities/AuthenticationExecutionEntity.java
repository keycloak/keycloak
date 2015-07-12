package org.keycloak.models.entities;

import org.keycloak.models.AuthenticationExecutionModel;


/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AuthenticationExecutionEntity {
    protected String id;
    protected String authenticator;
    private String authenticatorConfig;
    protected String flowId;
    protected AuthenticationExecutionModel.Requirement requirement;
    protected int priority;
    private boolean userSetupAllowed;
    private boolean authenticatorFlow;
    private String parentFlow;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(String authenticator) {
        this.authenticator = authenticator;
    }

    public AuthenticationExecutionModel.Requirement getRequirement() {
        return requirement;
    }

    public void setRequirement(AuthenticationExecutionModel.Requirement requirement) {
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

    public boolean isAuthenticatorFlow() {
        return authenticatorFlow;
    }

    public void setAuthenticatorFlow(boolean authenticatorFlow) {
        this.authenticatorFlow = authenticatorFlow;
    }

    public String getParentFlow() {
        return parentFlow;
    }

    public void setParentFlow(String parentFlow) {
        this.parentFlow = parentFlow;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getAuthenticatorConfig() {
        return authenticatorConfig;
    }

    public void setAuthenticatorConfig(String authenticatorConfig) {
        this.authenticatorConfig = authenticatorConfig;
    }
}
