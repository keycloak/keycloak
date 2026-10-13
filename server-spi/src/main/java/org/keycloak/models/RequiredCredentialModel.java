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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.PasswordCredentialModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RequiredCredentialModel implements Serializable {

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
    public static final RequiredCredentialModel KERBEROS;

    static {
        Map<String, RequiredCredentialModel> map = new HashMap<>();
        PASSWORD = new RequiredCredentialModel();
        PASSWORD.setType(PasswordCredentialModel.TYPE);
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
        TOTP.setType(OTPCredentialModel.TYPE);
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
        KERBEROS = new RequiredCredentialModel();
        KERBEROS.setType(UserCredentialModel.KERBEROS);
        KERBEROS.setInput(false);
        KERBEROS.setSecret(false);
        KERBEROS.setFormLabel("kerberos");
        map.put(KERBEROS.getType(), KERBEROS);
        BUILT_IN = Collections.unmodifiableMap(map);
    }
}
