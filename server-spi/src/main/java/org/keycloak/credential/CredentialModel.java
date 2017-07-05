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

import org.keycloak.common.util.MultivaluedHashMap;

import java.io.Serializable;

/**
 * Used just in cases when we want to "directly" update or retrieve the hash or salt of user credential (For example during export/import)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CredentialModel implements Serializable {
    public static final String PASSWORD = "password";
    public static final String PASSWORD_HISTORY = "password-history";
    public static final String PASSWORD_TOKEN = "password-token";

    // Secret is same as password but it is not hashed
    public static final String SECRET = "secret";
    public static final String TOTP = "totp";
    public static final String HOTP = "hotp";
    public static final String CLIENT_CERT = "cert";
    public static final String KERBEROS = "kerberos";
    public static final String OTP = "otp";



    private String id;
    private String type;
    private String value;
    private String device;
    private byte[] salt;
    private int hashIterations;
    private Long createdDate;

    // otp stuff
    private int counter;
    private String algorithm;
    private int digits;
    private int period;
    private MultivaluedHashMap<String, String> config;

    public CredentialModel shallowClone() {
        CredentialModel res = new CredentialModel();
        res.id = id;
        res.type = type;
        res.value = value;
        res.device = device;
        res.salt = salt;
        res.hashIterations = hashIterations;
        res.createdDate = createdDate;
        res.counter = counter;
        res.algorithm = algorithm;
        res.digits = digits;
        res.period = period;
        res.config = config;
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

    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    public int getHashIterations() {
        return hashIterations;
    }

    public void setHashIterations(int iterations) {
        this.hashIterations = iterations;
    }

    public Long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public int getDigits() {
        return digits;
    }

    public void setDigits(int digits) {
        this.digits = digits;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public MultivaluedHashMap<String, String> getConfig() {
        return config;
    }

    public void setConfig(MultivaluedHashMap<String, String> config) {
        this.config = config;
    }
}
