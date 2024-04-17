package org.keycloak.infinispan.util;

import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;

import static org.keycloak.common.Profile.Feature.MULTI_SITE;
import static org.keycloak.common.Profile.Feature.REMOTE_CACHE;

public final class InfinispanUtils {

    private InfinispanUtils() {
    }

    // all providers have the same order
    public static final int PROVIDER_ORDER = 1;

    // provider id for embedded cache providers
    public static final String EMBEDDED_PROVIDER_ID = "infinispan";

    // provider id for remote cache providers
    public static final String REMOTE_PROVIDER_ID = "remote";

    // true if running with external infinispan mode only
    public static boolean isRemoteInfinispan() {
        return Profile.isFeatureEnabled(Feature.MULTI_SITE) && Profile.isFeatureEnabled(REMOTE_CACHE);
    }

    // true if running with embedded caches.
    public static boolean isEmbeddedInfinispan() {
        return !Profile.isFeatureEnabled(MULTI_SITE) || !Profile.isFeatureEnabled(REMOTE_CACHE);
    }
}
