package org.keycloak.representations.idm;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CredentialRepresentation {
    public static final String SECRET = "secret";
    public static final String PASSWORD = "password";
    public static final String PASSWORD_TOKEN = "password-token";
    public static final String TOTP = "totp";
    public static final String CLIENT_CERT = "cert";

    protected String type;
    protected String device;

    // Plain-text value of credential (used for example during import from manually created JSON file)
    protected String value;

    // Value stored in DB (used for example during export/import)
    protected String hashedSaltedValue;
    protected String salt;
    protected Integer hashIterations;
    // only used when updating a credential.  Might set required action
    protected boolean temporary;

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

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getHashedSaltedValue() {
        return hashedSaltedValue;
    }

    public void setHashedSaltedValue(String hashedSaltedValue) {
        this.hashedSaltedValue = hashedSaltedValue;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public Integer getHashIterations() {
        return hashIterations;
    }

    public void setHashIterations(Integer hashIterations) {
        this.hashIterations = hashIterations;
    }

    public boolean isTemporary() {
        return temporary;
    }

    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }
}
