package org.keycloak.models.mongo.keycloak.entities;

import org.keycloak.models.entities.AbstractIdentifiableEntity;
import org.keycloak.models.mongo.api.MongoCollection;
import org.keycloak.models.mongo.api.MongoIdentifiableEntity;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@MongoCollection(collectionName = "session-client-associations")
public class MongoClientUserSessionAssociationEntity extends AbstractIdentifiableEntity implements MongoIdentifiableEntity {
    private String clientId;
    private String sessionId;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void afterRemove(MongoStoreInvocationContext invocationContext) {
    }

}
