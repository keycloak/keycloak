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
    public static final String TOTP = "totp";
    public static final String HOTP = "hotp";
    public static final String KERBEROS = "kerberos";

    private String id;
    private String type;
    private String userLabel;
    private Long createdDate;
    private String secretData;
    private String credentialData;
    private Integer priority;

    private String value;

    // only used when updating a credential.  Might set required action
    protected Boolean temporary;

    // All those fields are just for backwards compatibility
    @Deprecated
    protected String device;
    @Deprecated
    protected String hashedSaltedValue;
    @Deprecated
    protected String salt;
    @Deprecated
    protected Integer hashIterations;
    @Deprecated
    protected Integer counter;
    @Deprecated
    private String algorithm;
    @Deprecated
    private Integer digits;
    @Deprecated
    private Integer period;
    @Deprecated
    private MultivaluedHashMap<String, String> config;
    private String federationLink;

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

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Long getCreatedDate() {
        return createdDate;
    }
    public void setCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
    }


    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }

    public Boolean isTemporary() {
        return temporary;
    }
    public void setTemporary(Boolean temporary) {
        this.temporary = temporary;
    }

    @Deprecated
    public String getDevice() {
        return device;
    }

    @Deprecated
    public String getHashedSaltedValue() {
        return hashedSaltedValue;
    }

    @Deprecated
    public String getSalt() {
        return salt;
    }

    @Deprecated
    public Integer getHashIterations() {
        return hashIterations;
    }

    @Deprecated
    public Integer getCounter() {
        return counter;
    }

    @Deprecated
    public String getAlgorithm() {
        return algorithm;
    }

    @Deprecated
    public Integer getDigits() {
        return digits;
    }

    @Deprecated
    public Integer getPeriod() {
        return period;
    }

    @Deprecated
    public MultivaluedHashMap<String, String> getConfig() {
        return config;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((createdDate == null) ? 0 : createdDate.hashCode());
        result = prime * result + ((userLabel == null) ? 0 : userLabel.hashCode());
        result = prime * result + ((secretData == null) ? 0 : secretData.hashCode());
        result = prime * result + ((credentialData == null) ? 0 : credentialData.hashCode());
        result = prime * result + ((temporary == null) ? 0 : temporary.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        result = prime * result + ((priority == null) ? 0 : priority);
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
        if (secretData == null) {
            if (other.secretData != null)
                return false;
        } else if (!secretData.equals(other.secretData))
            return false;
        if (credentialData == null) {
            if (other.credentialData != null)
                return false;
        } else if (!credentialData.equals(other.credentialData))
            return false;
        if (createdDate == null) {
            if (other.createdDate != null)
                return false;
        } else if (!createdDate.equals(other.createdDate))
            return false;
        if (userLabel == null) {
            if (other.userLabel != null)
                return false;
        } else if (!userLabel.equals(other.userLabel))
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
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        if (priority == null) {
            if (other.priority != null)
                return false;
        } else if (!priority.equals(other.priority))
            return false;
        return true;
    }

    public void setFederationLink(String federationLink) {
        this.federationLink = federationLink;
    }

    public String getFederationLink() {
        return federationLink;
    }
}
