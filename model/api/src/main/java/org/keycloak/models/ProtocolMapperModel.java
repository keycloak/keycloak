package org.keycloak.models;

/**
 * Specifies a mapping from user data to a protocol claim assertion.  If protocolMapper is set, this points
 * to a @Provider that will perform the mapping.  If you have this set, then no other attributes of this class need to be set.
 * If you don't have it set, then this is a simple one to one mapping between the protocolClaim and the sourceAttribute.
 * SourceAttribute is the user data, protocolClaim is the name of the data you want to store in the protocols document or token.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ProtocolMapperModel {
    public static enum Source {
        USER_MODEL,
        USER_ATTRIBUTE,
        USER_SESSION_NOTE,
        CLIENT_SESSION_NOTE
    }

    protected String id;
    protected String name;
    protected String protocolClaim;
    protected String protocol;
    protected Source source;
    protected String sourceAttribute;
    protected String protocolMapper;
    protected boolean appliedByDefault;


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

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public String getProtocolMapper() {
        return protocolMapper;
    }

    public void setProtocolMapper(String protocolMapper) {
        this.protocolMapper = protocolMapper;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProtocolMapperModel that = (ProtocolMapperModel) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
