package org.keycloak.models.cache.infinispan.entities;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.cache.infinispan.DefaultLazyLoader;
import org.keycloak.models.cache.infinispan.LazyLoader;

abstract class AbstractCachedClientScope<D extends ClientScopeModel> extends AbstractRevisioned implements InRealm {

    private final LazyLoader<D, Map<String, ProtocolMapperModel>> mappersById;
    private final LazyLoader<D, Map<String, String>> mappersByName;
    private final LazyLoader<D, Map<String, List<String>>> mappersByType;

    public AbstractCachedClientScope(Long revision, ClientScopeModel model) {
        super(revision, model.getId());
        mappersById = new DefaultLazyLoader<>(scope -> scope.getProtocolMappersStream()
                .collect(Collectors.toMap(ProtocolMapperModel::getId, ProtocolMapperModel::new)),
                Collections::emptyMap);
        mappersByName = new DefaultLazyLoader<>(scope -> scope.getProtocolMappersStream()
                .collect(Collectors.toMap(mapper -> mapper.getProtocol() + "." + mapper.getName(),
                        ProtocolMapperModel::getId)),
                Collections::emptyMap);
        mappersByType = new DefaultLazyLoader<>(scope ->
                scope.getProtocolMappersStream()
                        .collect(Collectors.groupingBy(ProtocolMapperModel::getProtocolMapper,
                                Collectors.mapping(ProtocolMapperModel::getId, Collectors.toList()))),
                Collections::emptyMap);
    }

    public Stream<ProtocolMapperModel> getProtocolMappers(KeycloakSession session, Supplier<D> model) {
        return mappersById.get(session, model).values().stream();
    }

    public ProtocolMapperModel getProtocolMapperById(KeycloakSession session, Supplier<D> model, String id) {
        if (id == null) {
            return null;
        }
        return mappersById.get(session, model).get(id);
    }

    public List<ProtocolMapperModel> getProtocolMapperByType(KeycloakSession session, Supplier<D> model, String type) {
        return mappersByType.get(session, model).getOrDefault(type, List.of()).stream()
                .map(id -> getProtocolMapperById(session, model, id))
                .collect(Collectors.toList());
    }

    public ProtocolMapperModel getProtocolMapperByName(KeycloakSession session, Supplier<D> model, String protocol, String name) {
        String id = mappersByName.get(session, model).get(protocol + "." + name);
        return getProtocolMapperById(session, model, id);
    }
}
