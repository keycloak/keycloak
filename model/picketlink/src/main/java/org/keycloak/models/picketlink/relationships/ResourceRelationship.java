package org.keycloak.models.picketlink.relationships;

import org.picketlink.idm.model.AbstractAttributedType;
import org.picketlink.idm.model.Attribute;
import org.picketlink.idm.model.Relationship;
import org.picketlink.idm.model.annotation.AttributeProperty;
import org.picketlink.idm.query.AttributeParameter;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResourceRelationship extends AbstractAttributedType implements Relationship {
    private static final long serialVersionUID = 1L;

    public static final AttributeParameter REALM = new AttributeParameter("realm");
    public static final AttributeParameter RESOURCE = new AttributeParameter("resource");

    public ResourceRelationship() {
    }

    @AttributeProperty
    public String getRealm() {
        return (String)getAttribute("realm").getValue();
    }

    public void setRealm(String realm) {
        setAttribute(new Attribute<String>("realm", realm));
    }


    @AttributeProperty
    public String getResource() {
        return (String)getAttribute("resource").getValue();
    }

    public void setResource(String realm) {
        setAttribute(new Attribute<String>("resource", realm));
    }

}
