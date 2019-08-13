/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@NamedQueries({
        @NamedQuery(name="deleteUserFederatedDevicesByUser", query="delete from  FederatedUserDeviceInfoEntity d where d.userId = :userId and d.realmId=:realmId"),
        @NamedQuery(name="deleteUserFederatedDevicesByRealm", query="delete from  FederatedUserDeviceInfoEntity d where d.realmId=:realmId"),
        @NamedQuery(name="deleteFederatedDevicesByStorageProvider", query="delete from FederatedUserDeviceInfoEntity d where d.storageProviderId=:storageProviderId"),
        @NamedQuery(name="findDevicesByUser", query="select d from FederatedUserDeviceInfoEntity d where d.userId = :userId"),
        @NamedQuery(name="deleteUserFederatedDevicesByLastAccessedTime", query="delete from FederatedUserDeviceInfoEntity d where d.lastAccess < :lastAccess")
})
@Entity
@Table(name="FED_USER_DEVICE_INFO")
public class FederatedUserDeviceInfoEntity {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY)
    private String id;

    @Column(name = "USER_ID")
    protected String userId;

    @Column(name = "REALM_ID")
    protected String realmId;

    @Column(name = "STORAGE_PROVIDER_ID")
    protected String storageProviderId;

    @Column(name = "DEVICE")
    private String device;

    @Column(name = "BROWSER")
    private String browser;

    @Column(name = "OS")
    private String os;

    @Column(name = "OS_VERSION")
    private String osVersion;

    @Column(name = "IP")
    private String ip;

    @Column(name = "CREATED")
    private int created;

    @Column(name = "LAST_ACCESS")
    private int lastAccess;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getCreated() {
        return created;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public int getLastAccess() {
        return lastAccess;
    }

    public void setLastAccess(int lastAccess) {
        this.lastAccess = lastAccess;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof FederatedUserDeviceInfoEntity)) return false;

        FederatedUserDeviceInfoEntity that = (FederatedUserDeviceInfoEntity) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
