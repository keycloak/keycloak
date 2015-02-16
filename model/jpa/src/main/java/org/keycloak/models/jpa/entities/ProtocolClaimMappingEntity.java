package org.keycloak.models.jpa.entities;

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
@Entity
@NamedQueries({
        @NamedQuery(name="deleteProtocolClaimMappingsByRealm", query="delete from ProtocolClaimMappingEntity attr where attr.realm = :realm")
})
@Table(name="PROTOCOL_CLAIM_MAPPING")
public class ProtocolClaimMappingEntity {

    @Id
    @Column(name="ID", length = 36)
    protected String id;

    @Column(name = "PROTOCOL_CLAIM")
    protected String protocolClaim;
    @Column(name = "PROTOCOL")
    protected String protocol;
    @Column(name = "SOURCE")
    protected String source;
    @Column(name = "SOURCE_ATTRIBUTE")
    protected String sourceAttribute;
    @Column(name = "APPLIED_BY_DEFAULT")
    protected boolean appliedByDefault;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    protected RealmEntity realm;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProtocolClaim() {
        return protocolClaim;
    }

    public void setProtocolClaim(String protocolClaim) {
        this.protocolClaim = protocolClaim;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceAttribute() {
        return sourceAttribute;
    }

    public void setSourceAttribute(String sourceAttribute) {
        this.sourceAttribute = sourceAttribute;
    }

    public boolean isAppliedByDefault() {
        return appliedByDefault;
    }

    public void setAppliedByDefault(boolean appliedByDefault) {
        this.appliedByDefault = appliedByDefault;
    }

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProtocolClaimMappingEntity that = (ProtocolClaimMappingEntity) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
