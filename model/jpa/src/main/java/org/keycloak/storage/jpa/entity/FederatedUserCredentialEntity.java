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

package org.keycloak.storage.jpa.entity;

import org.keycloak.models.jpa.entities.UserEntity;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="federatedUserCredentialByUser", query="select cred from FederatedUserCredentialEntity cred where cred.userId = :userId"),
        @NamedQuery(name="federatedUserCredentialByUserAndType", query="select cred from FederatedUserCredentialEntity cred where cred.userId = :userId and cred.type = :type"),
        @NamedQuery(name="deleteFederatedUserCredentialByUser", query="delete from FederatedUserCredentialEntity cred where cred.userId = :userId and cred.realmId = :realmId"),
        @NamedQuery(name="deleteFederatedUserCredentialByUserAndType", query="delete from FederatedUserCredentialEntity cred where cred.userId = :userId and cred.type = :type"),
        @NamedQuery(name="deleteFederatedUserCredentialByUserAndTypeAndDevice", query="delete from FederatedUserCredentialEntity cred where cred.userId = :userId and cred.type = :type and cred.device = :device"),
        @NamedQuery(name="deleteFederatedUserCredentialsByRealm", query="delete from FederatedUserCredentialEntity cred where cred.realmId=:realmId"),
        @NamedQuery(name="deleteFederatedUserCredentialsByStorageProvider", query="delete from FederatedUserCredentialEntity cred where cred.storageProviderId=:storageProviderId"),
        @NamedQuery(name="deleteFederatedUserCredentialsByRealmAndLink", query="delete from FederatedUserCredentialEntity cred where cred.userId IN (select u.id from UserEntity u where u.realmId=:realmId and u.federationLink=:link)")

})
@Table(name="FED_USER_CREDENTIAL")
@Entity
public class FederatedUserCredentialEntity {
    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @Column(name="TYPE")
    protected String type;
    @Column(name="VALUE")
    protected String value;
    @Column(name="DEVICE")
    protected String device;
    @Column(name="SALT")
    protected byte[] salt;
    @Column(name="HASH_ITERATIONS")
    protected int hashIterations;
    @Column(name="CREATED_DATE")
    protected Long createdDate;
    
    @Column(name="USER_ID")
    protected String userId;

    @Column(name = "REALM_ID")
    protected String realmId;

    @Column(name = "STORAGE_PROVIDER_ID")
    protected String storageProviderId;



    @Column(name="COUNTER")
    protected int counter;

    @Column(name="ALGORITHM")
    protected String algorithm;
    @Column(name="DIGITS")
    protected int digits;
    @Column(name="PERIOD")
    protected int period;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getStorageProviderId() {
        return storageProviderId;
    }

    public void setStorageProviderId(String storageProviderId) {
        this.storageProviderId = storageProviderId;
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

    public void setHashIterations(int hashIterations) {
        this.hashIterations = hashIterations;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof FederatedUserCredentialEntity)) return false;

        FederatedUserCredentialEntity that = (FederatedUserCredentialEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
