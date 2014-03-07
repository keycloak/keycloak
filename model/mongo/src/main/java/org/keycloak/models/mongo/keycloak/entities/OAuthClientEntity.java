package org.keycloak.models.mongo.keycloak.entities;

import org.keycloak.models.mongo.api.MongoCollection;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "oauthClients")
public class OAuthClientEntity extends ClientEntity {

}
