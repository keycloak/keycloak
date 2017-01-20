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

import org.keycloak.storage.jpa.KeyUtils;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="getFederatedUserIds", query="select f.id from FederatedUser f where f.realmId=:realmId"),
        @NamedQuery(name="getFederatedUserCount", query="select count(u) from FederatedUser u where u.realmId = :realmId"),
        @NamedQuery(name="deleteFederatedUserByUser", query="delete from  FederatedUser f where f.id = :userId and f.realmId=:realmId"),
        @NamedQuery(name="deleteFederatedUsersByRealm", query="delete from  FederatedUser f where f.realmId=:realmId"),
        @NamedQuery(name="deleteFederatedUsersByStorageProvider", query="delete from FederatedUser f where f.storageProviderId=:storageProviderId"),
        @NamedQuery(name="deleteFederatedUsersByRealmAndLink", query="delete from  FederatedUser f where f.id IN (select u.id from UserEntity u where u.realmId=:realmId and u.federationLink=:link)")
})
@Entity
@Table(name="FEDERATED_USER")
public class FederatedUser {

    @Id
    @Column(name="ID")
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @Column(name = "REALM_ID")
    protected String realmId;

    @Column(name = "STORAGE_PROVIDER_ID")
    protected String storageProviderId;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        KeyUtils.assertValidKey(id);
        this.id = id;
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
}
