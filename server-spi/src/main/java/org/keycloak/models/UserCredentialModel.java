/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models;

import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.credential.PasswordUserCredentialModel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserCredentialModel implements CredentialInput {
    public static final String PASSWORD = CredentialModel.PASSWORD;
    public static final String PASSWORD_HISTORY = CredentialModel.PASSWORD_HISTORY;
    public static final String PASSWORD_TOKEN = CredentialModel.PASSWORD_TOKEN;

    // Secret is same as password but it is not hashed
    public static final String SECRET = CredentialModel.SECRET;
    public static final String TOTP = CredentialModel.TOTP;
    public static final String HOTP = CredentialModel.HOTP;
    public static final String CLIENT_CERT = CredentialModel.CLIENT_CERT;
    public static final String KERBEROS = CredentialModel.KERBEROS;

    protected String type;
    protected String value;
    protected String device;
    protected String algorithm;

    // Additional context informations
    protected Map<String, Object> notes = new HashMap<>();

    public UserCredentialModel() {
    }

    public static PasswordUserCredentialModel password(String password) {
        return password(password, false);
    }

    public static PasswordUserCredentialModel password(String password, boolean adminRequest) {
        PasswordUserCredentialModel model = new PasswordUserCredentialModel();
        model.setType(PASSWORD);
        model.setValue(password);
        model.setAdminRequest(adminRequest);
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

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public void setNote(String key, String value) {
        this.notes.put(key, value);
    }

    public void removeNote(String key) {
        this.notes.remove(key);
    }

    public Object getNote(String key) {
        return this.notes.get(key);
    }
}
