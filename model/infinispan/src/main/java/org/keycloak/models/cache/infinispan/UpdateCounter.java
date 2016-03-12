package org.keycloak.models.cache.infinispan;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Used to track cache revisions
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UpdateCounter {

    private final AtomicLong counter = new AtomicLong();

    public long current() {
        return counter.get();
    }

    public long next() {
        return counter.incrementAndGet();
    }

}