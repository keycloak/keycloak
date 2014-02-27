package org.keycloak.models.mongo.keycloak.entities;

import org.keycloak.models.mongo.api.MongoField;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ScopedEntity {
    @MongoField
    List<String> getScopeIds();

    void setScopeIds(List<String> scopeIds);
}
