package org.keycloak.models.mapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;

public class ClientModelMappers {
    
    private Map<String, BaseClientModelMapper<?>> mappers = new HashMap<>();
    
    public ClientModelMappers() {
        mappers.put(OIDCClientRepresentation.PROTOCOL, new OIDCClientModelMapper());
        mappers.put(SAMLClientRepresentation.PROTOCOL, new SAMLClientModelMapper());
    }
    
    public boolean isKnownField(String name) {
        return mappers.values().stream().anyMatch(f -> f.fields.containsKey(name));
    }
    
    public Optional<BaseClientModelMapper<?>> getMapper(String protocol) {
        return Optional.ofNullable(mappers.get(protocol));
    }
    
}
