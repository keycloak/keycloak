package org.keycloak.models.mapper;

import java.util.Set;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public interface RepModelMapper <T, U> {
    
    default T fromModel(U model) {
        return fromModel(model, null);
    }
    
    T fromModel(U model, Set<String> includeFields);
    
    void toModel(T rep, U existingModel);
}
