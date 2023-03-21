package org.keycloak.quarkus.runtime;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class EnvironmentTest {
    private static final String LAZY_LOADING_EXPECTED_PROPERTY_NAME = "kc.config.classloading.lazy-load-db-drivers";

    @After
    public void onAfter() {
        System.getProperties().remove(LAZY_LOADING_EXPECTED_PROPERTY_NAME);
    }
    @Test
    public void testIsDbDriverLazyLoadingEnabledWithPropertyNotSet() {
        Assert.assertTrue(Environment.isDbDriverLazyLoadingEnabled(true));
        Assert.assertFalse(Environment.isDbDriverLazyLoadingEnabled(false));
    }

    @Test
    public void testIsDbDriverLazyLoadingEnabledWithPropertySet() {
        System.setProperty(LAZY_LOADING_EXPECTED_PROPERTY_NAME, "true");
        Assert.assertTrue(Environment.isDbDriverLazyLoadingEnabled(false));

        System.setProperty(LAZY_LOADING_EXPECTED_PROPERTY_NAME, "false");
        Assert.assertFalse(Environment.isDbDriverLazyLoadingEnabled(true));
    }
}
