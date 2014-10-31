package org.keycloak.connections.infinispan;

import org.infinispan.Cache;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface InfinispanConnectionProvider extends Provider {

    static final String REALM_CACHE_NAME = "realms";
    static final String USER_CACHE_NAME = "users";
    static final String SESSION_CACHE_NAME = "sessions";
    static final String LOGIN_FAILURE_CACHE_NAME = "loginFailures";

    <K, V> Cache<K, V> getCache(String name);

}
