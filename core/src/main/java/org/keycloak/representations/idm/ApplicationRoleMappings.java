package org.keycloak.representations.idm;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApplicationRoleMappings {
    protected String applicationId;
    protected String application;
    protected String username;

    protected List<RoleRepresentation> mappings;

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<RoleRepresentation> getMappings() {
        return mappings;
    }

    public void setMappings(List<RoleRepresentation> mappings) {
        this.mappings = mappings;
    }
}
