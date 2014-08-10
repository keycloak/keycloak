package org.keycloak.models.cache.entities;

import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.cache.RealmCache;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CachedOAuthClient extends CachedClient {
    public CachedOAuthClient(RealmCache cache, RealmProvider delegate, RealmModel realm, OAuthClientModel model) {
        super(cache, delegate, realm, model);

    }
}
