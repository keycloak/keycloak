package org.keycloak.models.mongo.keycloak.entities;

import org.keycloak.models.mongo.api.MongoCollection;
import org.keycloak.models.mongo.api.MongoIndex;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "oauthClients")
@MongoIndex(fields = { "realmId", "name" }, unique = true)
public class OAuthClientEntity extends ClientEntity {

}
