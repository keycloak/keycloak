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

package org.keycloak.representations.idm;

import org.keycloak.common.util.MultivaluedHashMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CredentialRepresentation {
    public static final String SECRET = "secret";
    public static final String PASSWORD = "password";
    public static final String PASSWORD_TOKEN = "password-token";
    public static final String TOTP = "totp";
    public static final String HOTP = "hotp";
    public static final String CLIENT_CERT = "cert";
    public static final String KERBEROS = "kerberos";

    protected String type;
    protected String device;

    // Plain-text value of credential (used for example during import from manually created JSON file)
    protected String value;

    // Value stored in DB (used for example during export/import)
    protected String hashedSaltedValue;
    protected String salt;
    protected Integer hashIterations;
    protected Integer counter;
    private String algorithm;
    private Integer digits;
    private Integer period;
    private Long createdDate;
    private MultivaluedHashMap<String, String> config;

    // only used when updating a credential.  Might set required action
    protected Boolean temporary;

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

    public Boolean isTemporary() {
        return temporary;
    }

    public void setTemporary(Boolean temporary) {
        this.temporary = temporary;
    }

    public Integer getCounter() {
        return counter;
    }

    public void setCounter(Integer counter) {
        this.counter = counter;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public Integer getDigits() {
        return digits;
    }

    public void setDigits(Integer digits) {
        this.digits = digits;
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public Long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
    }

    public MultivaluedHashMap<String, String> getConfig() {
        return config;
    }

    public void setConfig(MultivaluedHashMap<String, String> config) {
        this.config = config;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((algorithm == null) ? 0 : algorithm.hashCode());
        result = prime * result + ((config == null) ? 0 : config.hashCode());
        result = prime * result + ((counter == null) ? 0 : counter.hashCode());
        result = prime * result + ((createdDate == null) ? 0 : createdDate.hashCode());
        result = prime * result + ((device == null) ? 0 : device.hashCode());
        result = prime * result + ((digits == null) ? 0 : digits.hashCode());
        result = prime * result + ((hashIterations == null) ? 0 : hashIterations.hashCode());
        result = prime * result + ((hashedSaltedValue == null) ? 0 : hashedSaltedValue.hashCode());
        result = prime * result + ((period == null) ? 0 : period.hashCode());
        result = prime * result + ((salt == null) ? 0 : salt.hashCode());
        result = prime * result + ((temporary == null) ? 0 : temporary.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CredentialRepresentation other = (CredentialRepresentation) obj;
        if (algorithm == null) {
            if (other.algorithm != null)
                return false;
        } else if (!algorithm.equals(other.algorithm))
            return false;
        if (config == null) {
            if (other.config != null)
                return false;
        } else if (!config.equals(other.config))
            return false;
        if (counter == null) {
            if (other.counter != null)
                return false;
        } else if (!counter.equals(other.counter))
            return false;
        if (createdDate == null) {
            if (other.createdDate != null)
                return false;
        } else if (!createdDate.equals(other.createdDate))
            return false;
        if (device == null) {
            if (other.device != null)
                return false;
        } else if (!device.equals(other.device))
            return false;
        if (digits == null) {
            if (other.digits != null)
                return false;
        } else if (!digits.equals(other.digits))
            return false;
        if (hashIterations == null) {
            if (other.hashIterations != null)
                return false;
        } else if (!hashIterations.equals(other.hashIterations))
            return false;
        if (hashedSaltedValue == null) {
            if (other.hashedSaltedValue != null)
                return false;
        } else if (!hashedSaltedValue.equals(other.hashedSaltedValue))
            return false;
        if (period == null) {
            if (other.period != null)
                return false;
        } else if (!period.equals(other.period))
            return false;
        if (salt == null) {
            if (other.salt != null)
                return false;
        } else if (!salt.equals(other.salt))
            return false;
        if (temporary == null) {
            if (other.temporary != null)
                return false;
        } else if (!temporary.equals(other.temporary))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }
}
