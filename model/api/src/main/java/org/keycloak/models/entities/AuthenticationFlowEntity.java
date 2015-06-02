package org.keycloak.models.entities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AuthenticationFlowEntity {
    protected String id;
    protected String alias;
    protected String description;
    List<AuthenticationExecutionEntity> executions = new ArrayList<AuthenticationExecutionEntity>();
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public List<AuthenticationExecutionEntity> getExecutions() {
        return executions;
    }

    public void setExecutions(List<AuthenticationExecutionEntity> executions) {
        this.executions = executions;
    }
}
