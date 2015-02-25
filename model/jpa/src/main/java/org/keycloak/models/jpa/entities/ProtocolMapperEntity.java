package org.keycloak.models.jpa.entities;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Entity
@NamedQueries({
        @NamedQuery(name="getProtocolMapperByName", query="select mapper from ProtocolMapperEntity mapper where mapper.name = :name and mapper.realm = :realm")
})
@Table(name="PROTOCOL_MAPPER")
public class ProtocolMapperEntity {

    @Id
    @Column(name="ID", length = 36)
    protected String id;

    @Column(name="NAME")
    protected String name;

    @Column(name = "PROTOCOL")
    protected String protocol;
    @Column(name = "PROTOCOL_MAPPER_NAME")
    protected String protocolMapper;
    @Column(name = "APPLIED_BY_DEFAULT")
    protected boolean appliedByDefault;
    @Column(name="CONSENT_REQUIRED")
    protected boolean consentRequired;
    @Column(name="CONSENT_TEXT")
    protected String consentText;

    @ElementCollection
    @MapKeyColumn(name="name")
    @Column(name="value")
    @CollectionTable(name="PROTOCOL_MAPPER_CONFIG", joinColumns={ @JoinColumn(name="PROTOCOL_MAPPER_ID") })
    private Map<String, String> config;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    protected RealmEntity realm;

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

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getProtocolMapper() {
        return protocolMapper;
    }

    public void setProtocolMapper(String protocolMapper) {
        this.protocolMapper = protocolMapper;
    }

    public boolean isAppliedByDefault() {
        return appliedByDefault;
    }

    public void setAppliedByDefault(boolean appliedByDefault) {
        this.appliedByDefault = appliedByDefault;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }

    public boolean isConsentRequired() {
        return consentRequired;
    }

    public void setConsentRequired(boolean consentRequired) {
        this.consentRequired = consentRequired;
    }

    public String getConsentText() {
        return consentText;
    }

    public void setConsentText(String consentText) {
        this.consentText = consentText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProtocolMapperEntity that = (ProtocolMapperEntity) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
