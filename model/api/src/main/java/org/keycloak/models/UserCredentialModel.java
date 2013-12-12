package org.keycloak.models;

import java.security.SecureRandom;

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
    protected String salt;
    protected String device;

    public UserCredentialModel() {
        SecureRandom random = new SecureRandom();
        byte saltBytes[] = new byte[16];
        random.nextBytes(saltBytes);

        StringBuilder sb = new StringBuilder();
        for (byte b : saltBytes) {
            sb.append(String.format("%02X", b));
        }

        this.salt = sb.toString();
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

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }
}
