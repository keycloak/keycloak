package org.keycloak.models.mapper;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;

public abstract class BaseClientModelMapper<T extends BaseClientRepresentation> implements ClientModelMapper {
    
    public static class MappedField<T> {
        
        Function<T, Object> repGetter;
        BiConsumer<T, Object> repSetter;
        Function<ClientModel, Object> modelGetter;
        BiConsumer<ClientModel, Object> modelSetter;
        
        void fromModel(ClientModel model, T rep) {
            if (repSetter != null && modelGetter != null) {
                repSetter.accept(rep, modelGetter.apply(model));
            }
        }
        
        void toModel(T rep, ClientModel model) {
            if (hasGetter() && modelSetter != null) {
                // TODO: exception handling to make things clearer when things fail
                modelSetter.accept(model, getValue(rep));
            }
        }
        
        public boolean hasGetter() {
            return repGetter != null;
        }
        
        public <V> V getValue(T rep) {
            if (repGetter != null) {
                return (V) repGetter.apply(rep);
            }
            return null;
        }
    }
 
    final Map<String, MappedField<BaseClientRepresentation>> fields = new LinkedHashMap<String, MappedField<BaseClientRepresentation>>();
    
    protected <F> void addMapping(String name, Function<T, F> repGetter, BiConsumer<T, F> repSetter, Function<ClientModel, F> modelGetter, BiConsumer<ClientModel, F> modelSetter) {
        MappedField prop = new MappedField<>();
        prop.repGetter = repGetter;
        prop.repSetter = repSetter;
        prop.modelGetter = modelGetter;
        prop.modelSetter = modelSetter;
        this.fields.put(name, prop);
    }
        
    public BaseClientModelMapper() {
        this.addMapping("protocol", BaseClientRepresentation::getProtocol, null, ClientModel::getProtocol, ClientModel::setProtocol);
        this.addMapping("uuid", BaseClientRepresentation::getUuid, BaseClientRepresentation::setUuid, ClientModel::getId, null);
        this.addMapping("enabled", BaseClientRepresentation::getEnabled, BaseClientRepresentation::setEnabled, ClientModel::isEnabled, (model, enabled) -> model.setEnabled(Boolean.TRUE.equals(enabled)));
        this.addMapping("clientId", BaseClientRepresentation::getClientId, BaseClientRepresentation::setClientId, ClientModel::getClientId, ClientModel::setClientId);
        this.addMapping("description", BaseClientRepresentation::getDescription, BaseClientRepresentation::setDescription, ClientModel::getDescription, ClientModel::setDescription);
        this.addMapping("displayName", BaseClientRepresentation::getDisplayName, BaseClientRepresentation::setDisplayName, ClientModel::getName, ClientModel::setName);
        this.addMapping("appUrl", BaseClientRepresentation::getAppUrl, BaseClientRepresentation::setAppUrl, ClientModel::getBaseUrl, ClientModel::setBaseUrl);
        // TODO: consider built-in logic for copying collections
        this.addMapping("redirectUris", BaseClientRepresentation::getRedirectUris, BaseClientRepresentation::setRedirectUris, model -> new LinkedHashSet<>(model.getRedirectUris()), (model, uris) -> model.setRedirectUris(new LinkedHashSet<>(uris)));
        this.addMapping("roles", BaseClientRepresentation::getRoles, BaseClientRepresentation::setRoles, model -> model.getRolesStream().map(RoleModel::getName).collect(Collectors.toSet()), null);
    }
    
    @Override
    public BaseClientRepresentation fromModel(ClientModel model, Set<String> includeFields) {
        // We don't want reps to depend on any unnecessary fields deps, hence no generated builder.

        T rep = createClientRepresentation();
        
        var stream = fields.entrySet().stream();
        if (includeFields != null && !includeFields.isEmpty()) {
            stream = stream.filter(e -> includeFields.contains(e.getKey()));
        }
        stream.forEach(e -> e.getValue().fromModel(model, rep));

        return rep;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void toModel(BaseClientRepresentation rep, ClientModel existingModel) {
        fields.values().forEach(m -> m.toModel(rep, existingModel));
    }

    protected abstract T createClientRepresentation();

}
