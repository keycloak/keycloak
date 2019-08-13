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

package org.keycloak.models.jpa.entities;

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
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@NamedQueries({
        @NamedQuery(name="deleteUserDeviceInfoByRealm", query="delete from UserDeviceInfoEntity d where d.user IN (select u from UserEntity u where u.realmId=:realmId)"),
        @NamedQuery(name="deleteUserDeviceInfoByRealmAndLink", query="delete from UserDeviceInfoEntity d where d.user IN (select u from UserEntity u where u.realmId=:realmId and u.federationLink=:link)"),
        @NamedQuery(name="deleteDevicesByLastAccessedTime", query="delete from UserDeviceInfoEntity d where d.lastAccess < :lastAccess")
})
@Entity
@Table(name="USER_DEVICE_INFO")
public class UserDeviceInfoEntity {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY)
    private String id;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="USER_ID")
    private UserEntity user;

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

    @Column(name = "REALM_ID")
    private String realm;

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

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof UserDeviceInfoEntity)) return false;

        UserDeviceInfoEntity that = (UserDeviceInfoEntity) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
