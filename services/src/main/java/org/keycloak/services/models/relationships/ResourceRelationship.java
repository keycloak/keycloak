package org.keycloak.services.models.relationships;

import org.picketlink.idm.model.AbstractAttributedType;
import org.picketlink.idm.model.Agent;
import org.picketlink.idm.model.Relationship;
import org.picketlink.idm.model.User;
import org.picketlink.idm.model.annotation.AttributeProperty;
import org.picketlink.idm.model.annotation.IdentityProperty;
import org.picketlink.idm.query.RelationshipQueryParameter;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResourceRelationship extends AbstractAttributedType implements Relationship {
    private static final long serialVersionUID = 1L;

    public static final RelationshipQueryParameter REALM_AGENT = new RelationshipQueryParameter() {

        @Override
        public String getName() {
            return "realmAgent";
        }
    };

    protected Agent realmAgent;
    protected User resourceUser;
    protected String resourceId;
    protected String resourceName;
    protected String managementUrl = ""; // Picketlink doesn't like null attribute values
    protected boolean surrogateAuthRequired;
    protected boolean enabled;

    @IdentityProperty
    public Agent getRealmAgent() {
        return realmAgent;
    }

    public void setRealmAgent(Agent realmAgent) {
        this.realmAgent = realmAgent;
    }

    @IdentityProperty
    public User getResourceUser() {
        return resourceUser;
    }

    public void setResourceUser(User resourceUser) {
        this.resourceUser = resourceUser;
    }

    @AttributeProperty
    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    @AttributeProperty
    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    @AttributeProperty
    public boolean getSurrogateAuthRequired() {
        return surrogateAuthRequired;
    }

    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        this.surrogateAuthRequired = surrogateAuthRequired;
    }

    @AttributeProperty
    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @AttributeProperty
    public String getManagementUrl()
    {
        return managementUrl;
    }

    public void setManagementUrl(String managementUrl) {
        if (managementUrl == null) managementUrl = ""; // Picketlink doesn't like NULL attribute values.
        this.managementUrl = managementUrl;
    }
}
