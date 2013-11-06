package org.keycloak.models.picketlink.relationships;

import org.picketlink.idm.model.AbstractAttributedType;
import org.picketlink.idm.model.Attribute;
import org.picketlink.idm.model.Relationship;
import org.picketlink.idm.query.AttributeParameter;

/**
 * Picketlink doesn't allow you to query for all partitions, thus this stupid relationship...
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmListingRelationship extends AbstractAttributedType implements Relationship {
    private static final long serialVersionUID = 1L;

    public static final AttributeParameter REALM = new AttributeParameter("realm");

    public String getRealm() {
        return (String)getAttribute("realm").getValue();
    }

    public void setRealm(String realm) {
        setAttribute(new Attribute<String>("realm", realm));
    }
}
