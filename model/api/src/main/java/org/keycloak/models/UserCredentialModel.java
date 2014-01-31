package org.keycloak.models;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserCredentialModel {
    public static final String PASSWORD = "password";
    public static final String TOTP = "totp";
    public static final String CLIENT_CERT = "cert";

    protected String type;
    protected String value;
    protected String device;

    public UserCredentialModel() {
    }

    public static UserCredentialModel password(String password) {
        UserCredentialModel model = new UserCredentialModel();
        model.setType(PASSWORD);
        model.setValue(password);
        return model;
    }


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
}
