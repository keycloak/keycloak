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
        @NamedQuery(name="deleteGrantedConsentProtMappersByRealm", query=
                "delete from GrantedConsentProtocolMapperEntity csm where csm.grantedConsent IN (select consent from GrantedConsentEntity consent where consent.user IN (select user from UserEntity user where user.realmId = :realmId))"),
        @NamedQuery(name="deleteGrantedConsentProtMappersByUser", query="delete from GrantedConsentProtocolMapperEntity csm where csm.grantedConsent IN (select consent from GrantedConsentEntity consent where consent.user = :user)"),
        @NamedQuery(name="deleteGrantedConsentProtMappersByProtocolMapper", query="delete from GrantedConsentProtocolMapperEntity csm where csm.protocolMapperId = :protocolMapperId)"),
        @NamedQuery(name="deleteGrantedConsentProtMappersByClient", query="delete from GrantedConsentProtocolMapperEntity csm where csm.grantedConsent IN (select consent from GrantedConsentEntity consent where consent.clientId = :clientId))"),
})
@Entity
@Table(name="GRANTED_CONSENT_PROT_MAPPER")
@IdClass(GrantedConsentProtocolMapperEntity.Key.class)
public class GrantedConsentProtocolMapperEntity {

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "GRANTED_CONSENT_ID")
    protected GrantedConsentEntity grantedConsent;

    @Id
    @Column(name="PROTOCOL_MAPPER_ID")
    protected String protocolMapperId;

    public GrantedConsentEntity getGrantedConsent() {
        return grantedConsent;
    }

    public void setGrantedConsent(GrantedConsentEntity grantedConsent) {
        this.grantedConsent = grantedConsent;
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
        if (o == null || getClass() != o.getClass()) return false;

        GrantedConsentProtocolMapperEntity that = (GrantedConsentProtocolMapperEntity)o;
        Key myKey = new Key(this.grantedConsent, this.protocolMapperId);
        Key hisKey = new Key(that.grantedConsent, that.protocolMapperId);
        return myKey.equals(hisKey);
    }

    @Override
    public int hashCode() {
        Key myKey = new Key(this.grantedConsent, this.protocolMapperId);
        return myKey.hashCode();
    }

    public static class Key implements Serializable {

        protected GrantedConsentEntity grantedConsent;

        protected String protocolMapperId;

        public Key() {
        }

        public Key(GrantedConsentEntity grantedConsent, String protocolMapperId) {
            this.grantedConsent = grantedConsent;
            this.protocolMapperId = protocolMapperId;
        }

        public GrantedConsentEntity getGrantedConsent() {
            return grantedConsent;
        }

        public String getProtocolMapperId() {
            return protocolMapperId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (grantedConsent != null ? !grantedConsent.getId().equals(key.grantedConsent != null ? key.grantedConsent.getId() : null) : key.grantedConsent != null) return false;
            if (protocolMapperId != null ? !protocolMapperId.equals(key.protocolMapperId) : key.protocolMapperId != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = grantedConsent != null ? grantedConsent.getId().hashCode() : 0;
            result = 31 * result + (protocolMapperId != null ? protocolMapperId.hashCode() : 0);
            return result;
        }
    }
}
