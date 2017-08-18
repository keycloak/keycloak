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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@NamedQueries({
        @NamedQuery(name="deleteUserConsentProtMappersByRealm", query=
                "delete from UserConsentProtocolMapperEntity csm where csm.userConsent IN (select consent from UserConsentEntity consent where consent.user IN (select user from UserEntity user where user.realmId = :realmId))"),
        @NamedQuery(name="deleteUserConsentProtMappersByUser", query="delete from UserConsentProtocolMapperEntity csm where csm.userConsent IN (select consent from UserConsentEntity consent where consent.user = :user)"),
        @NamedQuery(name="deleteUserConsentProtMappersByRealmAndLink", query="delete from UserConsentProtocolMapperEntity csm where csm.userConsent IN (select consent from UserConsentEntity consent where consent.user IN (select u from UserEntity u where u.realmId=:realmId and u.federationLink=:link))"),
        @NamedQuery(name="deleteUserConsentProtMappersByProtocolMapper", query="delete from UserConsentProtocolMapperEntity csm where csm.protocolMapperId = :protocolMapperId)"),
        @NamedQuery(name="deleteUserConsentProtMappersByClient", query="delete from UserConsentProtocolMapperEntity csm where csm.userConsent IN (select consent from UserConsentEntity consent where consent.clientId = :clientId))"),
})
@Entity
@Table(name="USER_CONSENT_PROT_MAPPER")
@IdClass(UserConsentProtocolMapperEntity.Key.class)
public class UserConsentProtocolMapperEntity {

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "USER_CONSENT_ID")
    protected UserConsentEntity userConsent;

    @Id
    @Column(name="PROTOCOL_MAPPER_ID")
    protected String protocolMapperId;

    public UserConsentEntity getUserConsent() {
        return userConsent;
    }

    public void setUserConsent(UserConsentEntity userConsent) {
        this.userConsent = userConsent;
    }

    public String getProtocolMapperId() {
        return protocolMapperId;
    }

    public void setProtocolMapperId(String protocolMapperId) {
        this.protocolMapperId = protocolMapperId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof UserConsentProtocolMapperEntity)) return false;

        UserConsentProtocolMapperEntity that = (UserConsentProtocolMapperEntity)o;
        Key myKey = new Key(this.userConsent, this.protocolMapperId);
        Key hisKey = new Key(that.userConsent, that.protocolMapperId);
        return myKey.equals(hisKey);
    }

    @Override
    public int hashCode() {
        Key myKey = new Key(this.userConsent, this.protocolMapperId);
        return myKey.hashCode();
    }

    public static class Key implements Serializable {

        protected UserConsentEntity userConsent;

        protected String protocolMapperId;

        public Key() {
        }

        public Key(UserConsentEntity userConsent, String protocolMapperId) {
            this.userConsent = userConsent;
            this.protocolMapperId = protocolMapperId;
        }

        public UserConsentEntity getUserConsent() {
            return userConsent;
        }

        public String getProtocolMapperId() {
            return protocolMapperId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (userConsent != null ? !userConsent.getId().equals(key.userConsent != null ? key.userConsent.getId() : null) : key.userConsent != null) return false;
            if (protocolMapperId != null ? !protocolMapperId.equals(key.protocolMapperId) : key.protocolMapperId != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = userConsent != null ? userConsent.getId().hashCode() : 0;
            result = 31 * result + (protocolMapperId != null ? protocolMapperId.hashCode() : 0);
            return result;
        }
    }


}
