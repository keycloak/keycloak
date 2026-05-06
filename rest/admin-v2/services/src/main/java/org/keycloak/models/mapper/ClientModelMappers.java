package org.keycloak.models.mapper;
import java.util.Map;
import java.util.Optional;

import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;

public class ClientModelMappers {

    private final Map<String, BaseClientModelMapper<?>> mappers;

    public ClientModelMappers() {
        // TODO: this may be done via discovery later
        mappers = Map.of(OIDCClientRepresentation.PROTOCOL, new OIDCClientModelMapper(),
                SAMLClientRepresentation.PROTOCOL, new SAMLClientModelMapper());
    }

    public boolean isKnownField(String name) {
        return mappers.values().stream().anyMatch(f -> f.fields.containsKey(name));
    }

    public Optional<BaseClientModelMapper<?>> getMapper(String protocol) {
        return Optional.ofNullable(mappers.get(protocol));
    }

}
