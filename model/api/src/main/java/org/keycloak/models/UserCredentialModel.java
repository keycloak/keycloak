package org.keycloak.models;

import java.util.UUID;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserCredentialModel {
    public static final String PASSWORD = "password";
    public static final String PASSWORD_HISTORY = "password-history";
    public static final String PASSWORD_TOKEN = "password-token";

    // Secret is same as password but it is not hashed
    public static final String SECRET = "secret";
    public static final String TOTP = "totp";
    public static final String HOTP = "hotp";
    public static final String CLIENT_CERT = "cert";
    public static final String KERBEROS = "kerberos";

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
    public static UserCredentialModel passwordToken(String passwordToken) {
        UserCredentialModel model = new UserCredentialModel();
        model.setType(PASSWORD_TOKEN);
        model.setValue(passwordToken);
        return model;
    }

    public static UserCredentialModel secret(String password) {
        UserCredentialModel model = new UserCredentialModel();
        model.setType(SECRET);
        model.setValue(password);
        return model;
    }

    public static UserCredentialModel otp(String type, String key) {
        if (type.equals(HOTP)) return hotp(key);
        if (type.equals(TOTP)) return totp(key);
        throw new RuntimeException("Unknown OTP type");
    }

    public static UserCredentialModel totp(String key) {
        UserCredentialModel model = new UserCredentialModel();
        model.setType(TOTP);
        model.setValue(key);
        return model;
    }

    public static UserCredentialModel hotp(String key) {
        UserCredentialModel model = new UserCredentialModel();
        model.setType(HOTP);
        model.setValue(key);
        return model;
    }

    public static UserCredentialModel kerberos(String token) {
        UserCredentialModel model = new UserCredentialModel();
        model.setType(KERBEROS);
        model.setValue(token);
        return model;
    }

    public static UserCredentialModel generateSecret() {
        UserCredentialModel model = new UserCredentialModel();
        model.setType(SECRET);
        model.setValue(UUID.randomUUID().toString());
        return model;
    }

    public static boolean isOtp(String type) {
        return TOTP.equals(type) || HOTP.equals(type);
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
