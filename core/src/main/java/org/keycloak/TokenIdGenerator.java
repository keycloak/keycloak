package org.keycloak;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class TokenIdGenerator {
    private static final AtomicLong counter = new AtomicLong();

    public static String generateId() {
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
    }
}
