package org.keycloak.models.mongo.keycloak.entities;

import org.keycloak.connections.mongo.api.MongoCollection;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "userSessions")
public class MongoOnlineUserSessionEntity extends MongoUserSessionEntity {
}
