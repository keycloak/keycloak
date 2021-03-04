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

package org.keycloak.credential;

import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.util.JsonSerialization;

/**
 * Used just in cases when we want to "directly" update or retrieve the hash or salt of user credential (For example during export/import)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CredentialModel implements Serializable {

    @Deprecated /** Use PasswordCredentialModel.TYPE instead **/
    public static final String PASSWORD = "password";

    @Deprecated /** Use PasswordCredentialModel.PASSWORD_HISTORY instead **/
    public static final String PASSWORD_HISTORY = "password-history";

    @Deprecated /** Legacy stuff. Not used in Keycloak anymore **/
    public static final String PASSWORD_TOKEN = "password-token";

    @Deprecated /** Use OTPCredentialModel.TYPE instead **/
    public static final String OTP = "otp";

    @Deprecated /** Use OTPCredentialModel.TOTP instead **/
    public static final String TOTP = "totp";

    @Deprecated /** Use OTPCredentialModel.HOTP instead **/
    public static final String HOTP = "hotp";

    // Secret is same as password but it is not hashed
    public static final String SECRET = "secret";
    public static final String CLIENT_CERT = "cert";
    public static final String KERBEROS = "kerberos";


    private String id;
    private String type;
    private String userLabel;
    private Long createdDate;

    private String secretData;
    private String credentialData;

    public CredentialModel shallowClone() {
        CredentialModel res = new CredentialModel();
        res.id = id;
        res.type = type;
        res.userLabel = userLabel;
        res.createdDate = createdDate;
        res.secretData = secretData;
        res.credentialData = credentialData;
        return res;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public String getUserLabel() {
        return userLabel;
    }
    public void setUserLabel(String userLabel) {
        this.userLabel = userLabel;
    }

    public Long getCreatedDate() {
        return createdDate;
    }
    public void setCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
    }

    public String getSecretData() {
        return secretData;
    }
    public void setSecretData(String secretData) {
        this.secretData = secretData;
    }

    public String getCredentialData() {
        return credentialData;
    }
    public void setCredentialData(String credentialData) {
        this.credentialData = credentialData;
    }

    public static Comparator<CredentialModel> comparingByStartDateDesc() {
        return (o1, o2) -> { // sort by date descending
            Long o1Date = o1.getCreatedDate() == null ? Long.MIN_VALUE : o1.getCreatedDate();
            Long o2Date = o2.getCreatedDate() == null ? Long.MIN_VALUE : o2.getCreatedDate();
            return (-o1Date.compareTo(o2Date));
        };
    }

    // DEPRECATED - the methods below exists for the backwards compatibility

    /**
     * @deprecated Recommended to use PasswordCredentialModel.getPasswordSecretData().getValue() or OTPCredentialModel.getOTPSecretData().getValue()
     */
    @Deprecated
    @JsonIgnore
    public String getValue() {
        return readString("value", true);
    }

    /**
     * @deprecated See {@link #getValue()}
     */
    @Deprecated
    public void setValue(String value) {
        writeProperty("value", value, true);
    }

    /**
     * @deprecated Recommended to use OTPCredentialModel.getOTPCredentialData().getDevice()
     */
    @Deprecated
    @JsonIgnore
    public String getDevice() {
        return readString("device", false);
    }

    /**
     * @deprecated See {@link #getDevice()}
     */
    @Deprecated
    public void setDevice(String device) {
        writeProperty("device", device, false);
    }

    /**
     * @deprecated Recommended to use PasswordCredentialModel.getPasswordSecretData().getSalt()
     */
    @Deprecated
    @JsonIgnore
    public byte[] getSalt() {
        try {
            String saltStr = readString("salt", true);
            return saltStr == null ? null : Base64.decode(saltStr);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * @deprecated See {@link #getSalt()}
     */
    @Deprecated
    public void setSalt(byte[] salt) {
        String saltStr = salt == null ? null : Base64.encodeBytes(salt);
        writeProperty("salt", saltStr, true);
    }

    /**
     * @deprecated Recommended to use PasswordCredentialModel.getPasswordCredentialData().getHashIterations()
     */
    @Deprecated
    @JsonIgnore
    public int getHashIterations() {
        return readInt("hashIterations", false);
    }

    /**
     * @deprecated See {@link #getHashIterations()}
     */
    @Deprecated
    public void setHashIterations(int iterations) {
        writeProperty("hashIterations", iterations, false);
    }

    /**
     * @deprecated Recommended to use OTPCredentialModel.getOTPCredentialData().getCounter()
     */
    @Deprecated
    @JsonIgnore
    public int getCounter() {
        return readInt("counter", false);
    }

    /**
     * @deprecated See {@link #getCounter()}
     */
    @Deprecated
    public void setCounter(int counter) {
        writeProperty("counter", counter, false);
    }

    /**
     * @deprecated Recommended to use PasswordCredentialModel.getPasswordCredentialData().getAlgorithm() or OTPCredentialModel.getOTPCredentialData().getAlgorithm()
     */
    @Deprecated
    @JsonIgnore
    public String getAlgorithm() {
        return readString("algorithm", false);
    }

    /**
     * @deprecated See {@link #getAlgorithm()}
     */
    @Deprecated
    public void setAlgorithm(String algorithm) {
        writeProperty("algorithm", algorithm, false);
    }

    /**
     * @deprecated Recommended to use OTPCredentialModel.getOTPCredentialData().getDigits()
     */
    @Deprecated
    @JsonIgnore
    public int getDigits() {
        return readInt("digits", false);
    }

    /**
     * @deprecated See {@link #setDigits(int)}
     */
    @Deprecated
    public void setDigits(int digits) {
        writeProperty("digits", digits, false);
    }

    /**
     * @deprecated Recommended to use OTPCredentialModel.getOTPCredentialData().getPeriod()
     */
    @Deprecated
    @JsonIgnore
    public int getPeriod() {
        return readInt("period", false);
    }

    /**
     * @deprecated See {@link #setPeriod(int)}
     */
    @Deprecated
    public void setPeriod(int period) {
        writeProperty("period", period, false);
    }

    /**
     * @deprecated Recommended to use {@link #getCredentialData()} instead and use the subtype of CredentialData specific to your credential
     */
    @Deprecated
    @JsonIgnore
    public MultivaluedHashMap<String, String> getConfig() {
        Map<String, Object> credentialData = readMapFromJson(false);
        if (credentialData == null) {
            return null;
        }

        Object obj = credentialData.get("config");
        return obj == null ? null : new MultivaluedHashMap<>((Map)obj);
    }

    /**
     * @deprecated Recommended to use {@link #setCredentialData(String)} instead and use the subtype of CredentialData specific to your credential
     */
    @Deprecated
    public void setConfig(MultivaluedHashMap<String, String> config) {
        writeProperty("config", config, false);
    }

    private Map<String, Object> readMapFromJson(boolean secret) {
        String jsonStr = secret ? secretData : credentialData;
        if (jsonStr == null) {
            return new HashMap<>();
        }

        try {
            return JsonSerialization.readValue(jsonStr, Map.class);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private void writeMapAsJson(Map<String, Object> map, boolean secret) {
        try {
            String jsonStr = JsonSerialization.writeValueAsString(map);
            if (secret) {
                this.secretData = jsonStr;
            } else {
                this.credentialData = jsonStr;
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private String readString(String key, boolean secret) {
        Map<String, Object> credentialDataMap = readMapFromJson(secret);
        return (String) credentialDataMap.get(key);
    }

    private int readInt(String key, boolean secret) {
        Map<String, Object> credentialDataMap = readMapFromJson(secret);
        Object obj = credentialDataMap.get(key);
        return obj == null ? 0 : (Integer) obj;
    }

    private void writeProperty(String key, Object value, boolean secret) {
        Map<String, Object> credentialDataMap = readMapFromJson(secret);
        if (value == null) {
            credentialDataMap.remove(key);
        } else {
            credentialDataMap.put(key, value);
        }
        writeMapAsJson(credentialDataMap, secret);
    }
}
