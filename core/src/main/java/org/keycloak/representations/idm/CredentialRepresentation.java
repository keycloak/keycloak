package org.keycloak.representations.idm;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public class CredentialRepresentation {
    protected String type;
    protected String value;
    protected boolean hashed;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isHashed() {
        return hashed;
    }

    public void setHashed(boolean hashed) {
        this.hashed = hashed;
    }
}
