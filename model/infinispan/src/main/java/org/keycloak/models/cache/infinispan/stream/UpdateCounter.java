package org.keycloak.models.cache.infinispan.stream;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UpdateCounter {

    private static final AtomicLong counter = new AtomicLong();

    public static long current() {
        return counter.get();
    }

    public static long next() {
        return counter.incrementAndGet();
    }

}