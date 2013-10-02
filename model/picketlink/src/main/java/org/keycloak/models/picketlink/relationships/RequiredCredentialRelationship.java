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
public class RequiredCredentialRelationship extends AbstractAttributedType implements Relationship {
    private static final long serialVersionUID = 1L;

    public static final AttributeParameter REALM = new AttributeParameter("realm");


    //protected String realm;
    //protected String credentialType;
    //protected boolean input;
    //protected boolean secret;

    public RequiredCredentialRelationship() {
    }

    /*
    @AttributeProperty
    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }*/

    public String getRealm() {
        return (String)getAttribute("realm").getValue();
    }

    public void setRealm(String realm) {
        setAttribute(new Attribute<String>("realm", realm));
    }

    @AttributeProperty
    public String getCredentialType() {
        return (String)getAttribute("credentialType").getValue();
    }

    public void setCredentialType(String credentialType) {
        setAttribute(new Attribute<String>("credentialType", credentialType));
    }

    @AttributeProperty
    public boolean isInput() {
        return (Boolean)getAttribute("input").getValue();
    }

    public void setInput(boolean input) {
        setAttribute(new Attribute<Boolean>("input", input));
    }

    @AttributeProperty
    public boolean isSecret() {
        return (Boolean)getAttribute("secret").getValue();
    }

    public void setSecret(boolean secret) {
        setAttribute(new Attribute<Boolean>("secret", secret));
    }

    @AttributeProperty
    public String getFormLabel() {
        return (String)getAttribute("formLabel").getValue();
    }

    public void setFormLabel(String label) {
        setAttribute(new Attribute<String>("formLabel", label));
    }

}
