package org.keycloak.connections.infinispan;

import org.infinispan.Cache;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface InfinispanConnectionProvider extends Provider {

    <K, V> Cache<K, V> getCache(String name);

}
