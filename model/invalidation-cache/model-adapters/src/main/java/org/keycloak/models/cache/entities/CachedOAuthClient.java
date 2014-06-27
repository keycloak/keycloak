package org.keycloak.models.cache.entities;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelProvider;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.KeycloakCache;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CachedOAuthClient extends CachedClient {
    public CachedOAuthClient(KeycloakCache cache, ModelProvider delegate, RealmModel realm, OAuthClientModel model) {
        super(cache, delegate, realm, model);

    }
}
