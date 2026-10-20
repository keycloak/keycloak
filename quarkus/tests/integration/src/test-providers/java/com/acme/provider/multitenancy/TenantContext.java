package com.acme.provider.multitenancy;

/**
 * Carries the tenant identifier selected by the current request to the per-persistence-unit
 * {@code TenantResolver} implementations. Used only by this test provider to exercise multi-tenant
 * persistence units defined in {@code META-INF/persistence.xml}.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(String tenant) {
        CURRENT.set(tenant);
    }

    public static String get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
