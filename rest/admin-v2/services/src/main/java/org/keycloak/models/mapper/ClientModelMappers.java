package org.keycloak.models.mapper;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.keycloak.representations.admin.v2.BaseClientRepresentation;
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

    public Object resolveFieldValue(String name, BaseClientRepresentation rep) {
        String protocol = rep.getProtocol();
        var mapper = protocol != null ? mappers.get(protocol) : null;
        if (mapper != null) {
            var field = mapper.fields.get(name);
            if (field != null) {
                return field.getValue(rep);
            }
        }
        return null;
    }

    public void applyProjection(BaseClientRepresentation rep, Set<String> includeFields) {
        String protocol = rep.getProtocol();
        var mapper = protocol != null ? mappers.get(protocol) : null;
        if (mapper != null) {
            mapper.applyProjection(rep, includeFields);
        }
    }

    public Optional<BaseClientModelMapper<?>> getMapper(String protocol) {
        return Optional.ofNullable(mappers.get(protocol));
    }

}
