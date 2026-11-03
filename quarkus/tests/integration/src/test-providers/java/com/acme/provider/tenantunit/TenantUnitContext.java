package com.acme.provider.tenantunit;

/**
 * Carries the tenant selected by the current request to {@link TenantUnitTenantResolver}.
 */
public final class TenantUnitContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantUnitContext() {
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
