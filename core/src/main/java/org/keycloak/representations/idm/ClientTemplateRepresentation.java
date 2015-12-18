package org.keycloak.representations.idm;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientTemplateRepresentation {
    /**
     * Use this value in ClientRepresentation.setClientTemplate when you want to clear this value
     */
    public static final String NONE = "NONE";
    protected String id;
    protected String name;
    protected String description;
    protected String protocol;
    protected Boolean fullScopeAllowed;
    protected List<ProtocolMapperRepresentation> protocolMappers;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public List<ProtocolMapperRepresentation> getProtocolMappers() {
        return protocolMappers;
    }

    public void setProtocolMappers(List<ProtocolMapperRepresentation> protocolMappers) {
        this.protocolMappers = protocolMappers;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Boolean isFullScopeAllowed() {
        return fullScopeAllowed;
    }

    public void setFullScopeAllowed(Boolean fullScopeAllowed) {
        this.fullScopeAllowed = fullScopeAllowed;
    }
}
