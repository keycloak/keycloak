package org.keycloak.models.utils;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public final class KeycloakSessionUtils {

    private KeycloakSessionUtils() {
    }

    private static AtomicLong counter = new AtomicLong(1);

    public static String generateId() {
        return counter.getAndIncrement() + "-" + System.currentTimeMillis();
    }
}
