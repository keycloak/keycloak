package org.keycloak.models;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RequiredCredentialModel {
    protected String type;
    protected boolean input;
    protected boolean secret;
    protected String formLabel;

    public RequiredCredentialModel() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isInput() {
        return input;
    }

    public void setInput(boolean input) {
        this.input = input;
    }

    public boolean isSecret() {
        return secret;
    }

    public void setSecret(boolean secret) {
        this.secret = secret;
    }

    public String getFormLabel() {
        return formLabel;
    }

    public void setFormLabel(String formLabel) {
        this.formLabel = formLabel;
    }

    public static final Map<String, RequiredCredentialModel> BUILT_IN;
    public static final RequiredCredentialModel PASSWORD;
    public static final RequiredCredentialModel TOTP;
    public static final RequiredCredentialModel CLIENT_CERT;
    public static final RequiredCredentialModel SECRET;

    static {
        Map<String, RequiredCredentialModel> map = new HashMap<String, RequiredCredentialModel>();
        PASSWORD = new RequiredCredentialModel();
        PASSWORD.setType(UserCredentialModel.PASSWORD);
        PASSWORD.setInput(true);
        PASSWORD.setSecret(true);
        PASSWORD.setFormLabel("password");
        map.put(PASSWORD.getType(), PASSWORD);
        SECRET = new RequiredCredentialModel();
        SECRET.setType(UserCredentialModel.SECRET);
        SECRET.setInput(false);
        SECRET.setSecret(true);
        SECRET.setFormLabel("secret");
        map.put(SECRET.getType(), SECRET);
        TOTP = new RequiredCredentialModel();
        TOTP.setType(UserCredentialModel.TOTP);
        TOTP.setInput(true);
        TOTP.setSecret(false);
        TOTP.setFormLabel("authenticatorCode");
        map.put(TOTP.getType(), TOTP);
        CLIENT_CERT = new RequiredCredentialModel();
        CLIENT_CERT.setType(UserCredentialModel.CLIENT_CERT);
        CLIENT_CERT.setInput(false);
        CLIENT_CERT.setSecret(false);
        CLIENT_CERT.setFormLabel("clientCertificate");
        map.put(CLIENT_CERT.getType(), CLIENT_CERT);
        BUILT_IN = Collections.unmodifiableMap(map);
    }
}
