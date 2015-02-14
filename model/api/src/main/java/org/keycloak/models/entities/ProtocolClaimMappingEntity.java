package org.keycloak.models.entities;

import org.keycloak.models.ProtocolClaimMappingModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ProtocolClaimMappingEntity {
    protected String id;
    protected String protocolClaim;
    protected String protocol;
    protected ProtocolClaimMappingModel.Source source;
    protected String sourceAttribute;
    protected boolean appliedByDefault;

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

    public ProtocolClaimMappingModel.Source getSource() {
        return source;
    }

    public void setSource(ProtocolClaimMappingModel.Source source) {
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
}
