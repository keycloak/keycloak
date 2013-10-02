package org.keycloak.models.picketlink.mappings;

import org.picketlink.idm.model.AbstractPartition;
import org.picketlink.idm.model.annotation.AttributeProperty;
import org.picketlink.idm.model.sample.User;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApplicationData extends AbstractPartition {
    private String resourceName;
    private boolean enabled;
    private boolean surrogateAuthRequired;
    private String managementUrl;
    private User resourceUser;

    public ApplicationData() {
        super(null);
    }
    public ApplicationData(String name) {
        super(name);
    }

    @AttributeProperty
    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public User getResourceUser() {
        return resourceUser;
    }

    public void setResourceUser(User resourceUser) {
        this.resourceUser = resourceUser;
    }

    @AttributeProperty
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @AttributeProperty
    public boolean isSurrogateAuthRequired() {
        return surrogateAuthRequired;
    }

    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        this.surrogateAuthRequired = surrogateAuthRequired;
    }

    @AttributeProperty
    public String getManagementUrl() {
        return managementUrl;
    }

    public void setManagementUrl(String managementUrl) {
        this.managementUrl = managementUrl;
    }
}
