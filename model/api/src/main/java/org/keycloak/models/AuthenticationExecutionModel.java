package org.keycloak.models;

import java.util.Comparator;
import java.io.Serializable;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public class AuthenticationExecutionModel implements Serializable {
    private static final long serialVersionUID = 1L;

    public static class ExecutionComparator implements Comparator<AuthenticationExecutionModel> {
        public static final ExecutionComparator SINGLETON = new ExecutionComparator();

        @Override
        public int compare(AuthenticationExecutionModel o1, AuthenticationExecutionModel o2) {
            return o1.priority - o2.priority;
        }
    }

    private String id;
    private String authenticator;
    private boolean autheticatorFlow;
    private Requirement requirement;
    private boolean userSetupAllowed;
    private int priority;
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

    public Requirement getRequirement() {
        return requirement;
    }

    public void setRequirement(Requirement requirement) {
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

    public String getParentFlow() {
        return parentFlow;
    }

    public void setParentFlow(String parentFlow) {
        this.parentFlow = parentFlow;
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

    public enum Requirement {
        REQUIRED,
        OPTIONAL,
        ALTERNATIVE,
        DISABLED
    }

    public boolean isRequired() {
        return requirement == Requirement.REQUIRED;
    }
    public boolean isOptional() {
        return requirement == Requirement.OPTIONAL;
    }
    public boolean isAlternative() {
        return requirement == Requirement.ALTERNATIVE;
    }
    public boolean isDisabled() {
        return requirement == Requirement.DISABLED;
    }
    public boolean isEnabled() {
        return requirement != Requirement.DISABLED;
    }
}
