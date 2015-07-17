package org.keycloak.representations.idm;

import java.io.Serializable;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AuthenticationFlowRepresentation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String alias;
    private String description;
    private String providerId;
    private boolean topLevel;
    private boolean builtIn;
    protected List<AuthenticationExecutionRepresentation> authenticationExecutions;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public boolean isTopLevel() {
        return topLevel;
    }

    public void setTopLevel(boolean topLevel) {
        this.topLevel = topLevel;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(boolean builtIn) {
        this.builtIn = builtIn;
    }

    public List<AuthenticationExecutionRepresentation> getAuthenticationExecutions() {
        return authenticationExecutions;
    }

    public void setAuthenticationExecutions(List<AuthenticationExecutionRepresentation> authenticationExecutions) {
        this.authenticationExecutions = authenticationExecutions;
    }
}
