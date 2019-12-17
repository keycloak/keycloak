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

package org.keycloak.models.jpa.entities;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="credentialByUser", query="select cred from CredentialEntity cred where cred.user = :user order by cred.priority"),
        @NamedQuery(name="deleteCredentialsByRealm", query="delete from CredentialEntity cred where cred.user IN (select u from UserEntity u where u.realmId=:realmId)"),
        @NamedQuery(name="deleteCredentialsByRealmAndLink", query="delete from CredentialEntity cred where cred.user IN (select u from UserEntity u where u.realmId=:realmId and u.federationLink=:link)")

})
@Table(name="CREDENTIAL")
@Entity
public class CredentialEntity {
    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    protected String id;

    @Column(name="TYPE")
    protected String type;

    @Column(name="USER_LABEL")
    protected String userLabel;

    @Column(name="CREATED_DATE")
    protected Long createdDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="USER_ID")
    protected UserEntity user;

    @Column(name="SECRET_DATA")
    protected String secretData;

    @Column(name="CREDENTIAL_DATA")
    protected String credentialData;

    @Column(name="PRIORITY")
    protected int priority;

    @Deprecated // Needed just for backwards compatibility when migrating old credentials
    @Column(name="SALT")
    protected byte[] salt;

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

    public UserEntity getUser() {
        return user;
    }
    public void setUser(UserEntity user) {
        this.user = user;
    }

    @Deprecated
    public byte[] getSalt() {
        return salt;
    }

    @Deprecated
    public void setSalt(byte[] salt) {
        this.salt = salt;
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

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof CredentialEntity)) return false;

        CredentialEntity that = (CredentialEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
