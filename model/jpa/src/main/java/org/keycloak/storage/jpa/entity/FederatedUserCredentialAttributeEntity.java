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
        @NamedQuery(name="deleteFederatedCredentialAttributeByCredential", query="delete from  FederatedUserCredentialAttributeEntity attr where attr.credential = :credential"),
        @NamedQuery(name="deleteFederatedCredentialAttributeByStorageProvider", query="delete from  FederatedUserCredentialAttributeEntity attr where attr.credential IN (select cred from FederatedUserCredentialEntity cred where cred.storageProviderId=:storageProviderId)"),
        @NamedQuery(name="deleteFederatedCredentialAttributeByRealm", query="delete from  FederatedUserCredentialAttributeEntity attr where attr.credential IN (select cred from FederatedUserCredentialEntity cred where cred.realmId=:realmId)"),
        @NamedQuery(name="deleteFederatedCredentialAttributeByRealmAndLink", query="delete from  FederatedUserCredentialAttributeEntity attr where attr.credential IN (select cred from FederatedUserCredentialEntity cred where cred.userId IN (select u.id from UserEntity u where u.realmId=:realmId and u.federationLink=:link))"),
        @NamedQuery(name="deleteFederatedCredentialAttributeByUser", query="delete from  FederatedUserCredentialAttributeEntity attr where attr.credential IN (select cred from FederatedUserCredentialEntity cred where cred.userId = :userId and cred.realmId = :realmId)"),
})
@Table(name="FED_CREDENTIAL_ATTRIBUTE")
@Entity
public class FederatedUserCredentialAttributeEntity {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "CREDENTIAL_ID")
    protected FederatedUserCredentialEntity credential;

    @Column(name = "NAME")
    protected String name;
    @Column(name = "VALUE")
    protected String value;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public FederatedUserCredentialEntity getCredential() {
        return credential;
    }

    public void setCredential(FederatedUserCredentialEntity credential) {
        this.credential = credential;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof FederatedUserCredentialAttributeEntity)) return false;

        FederatedUserCredentialAttributeEntity that = (FederatedUserCredentialAttributeEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
