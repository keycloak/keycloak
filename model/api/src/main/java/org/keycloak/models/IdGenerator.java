package org.keycloak.models;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class IdGenerator {
    private static AtomicLong counter = new AtomicLong(1);
    public static String generateId() {
        return counter.getAndIncrement() + "-" + System.currentTimeMillis();
    }

}
