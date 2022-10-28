package org.keycloak.common;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class ProfileTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void checkDefaults() {
        // Default feature enabled
        Assert.assertTrue(Profile.isFeatureEnabled(Feature.AUTHORIZATION));

        // Disabled by default feature enabled
        Assert.assertFalse(Profile.isFeatureEnabled(Feature.DOCKER));

        // Preview feature disabled
        Assert.assertFalse(Profile.isFeatureEnabled(Feature.ADMIN_FINE_GRAINED_AUTHZ));

        // Experimental feature disabled
        Assert.assertFalse(Profile.isFeatureEnabled(Feature.DYNAMIC_SCOPES));

        // Deprecated feature disabled
        Assert.assertFalse(Profile.isFeatureEnabled(Feature.ADMIN));

        Assert.assertEquals("default", Profile.getName());
        assertEquals(Profile.getDisabledFeatures(), Feature.ADMIN, Feature.ADMIN_FINE_GRAINED_AUTHZ, Feature.DYNAMIC_SCOPES, Feature.DOCKER, Feature.RECOVERY_CODES, Feature.SCRIPTS, Feature.TOKEN_EXCHANGE, Feature.OPENSHIFT_INTEGRATION, Feature.MAP_STORAGE, Feature.DECLARATIVE_USER_PROFILE, Feature.CLIENT_SECRET_ROTATION, Feature.UPDATE_EMAIL);
        assertEquals(Profile.getPreviewFeatures(), Feature.ADMIN_FINE_GRAINED_AUTHZ, Feature.RECOVERY_CODES, Feature.SCRIPTS, Feature.TOKEN_EXCHANGE, Feature.OPENSHIFT_INTEGRATION, Feature.DECLARATIVE_USER_PROFILE, Feature.CLIENT_SECRET_ROTATION, Feature.UPDATE_EMAIL);
    }

    @Test
    public void checkPreviewFeatureEnabledWithPreviewProfile() {
        System.setProperty("keycloak.profile", "preview");
        Profile.init();

        Assert.assertTrue(Profile.isFeatureEnabled(Feature.ADMIN_FINE_GRAINED_AUTHZ));
    }

    @Test
    public void checkFailureIfDependencyDisabled() {
        System.setProperty("keycloak.profile.feature.account_api", "disabled");

        try {
            Profile.init();
        } catch (ProfileException e) {
            Assert.assertEquals("Feature account2 depends on disabled feature account-api", e.getMessage());
        }

        System.getProperties().remove("keycloak.profile.feature.account_api");
    }

    @Test
    public void checkErrorOnBadConfig() {
        System.setProperty("keycloak.profile.feature.account_api", "invalid");

        try {
            Profile.init();
        } catch (ProfileException e) {
            Assert.assertEquals("Invalid config value 'invalid' for feature account-api", e.getMessage());
        }

        System.getProperties().remove("keycloak.profile.feature.account_api");
    }

    @Test
    public void configWithSystemProperties() {
        Assert.assertEquals("default", Profile.getName());
        Assert.assertFalse(Profile.isFeatureEnabled(Feature.DOCKER));
        Assert.assertFalse(Profile.isFeatureEnabled(Feature.OPENSHIFT_INTEGRATION));
        assertTrue(Profile.isFeatureEnabled(Feature.IMPERSONATION));

        System.setProperty("keycloak.profile", "preview");
        System.setProperty("keycloak.profile.feature.docker", "enabled");
        System.setProperty("keycloak.profile.feature.impersonation", "disabled");
        System.setProperty("keycloak.profile.feature.upload_scripts", "enabled");

        Profile.init();

        Assert.assertEquals("preview", Profile.getName());
        assertTrue(Profile.isFeatureEnabled(Feature.DOCKER));
        assertTrue(Profile.isFeatureEnabled(Feature.OPENSHIFT_INTEGRATION));
        Assert.assertFalse(Profile.isFeatureEnabled(Feature.IMPERSONATION));

        System.getProperties().remove("keycloak.profile");
        System.getProperties().remove("keycloak.profile.feature.docker");
        System.getProperties().remove("keycloak.profile.feature.impersonation");
        System.getProperties().remove("keycloak.profile.feature.upload_scripts");

        Profile.init();
    }

    public static void assertEquals(Set<Feature> actual, Feature... expected) {
        Feature[] a = actual.toArray(new Feature[actual.size()]);
        Arrays.sort(a, new FeatureComparator());
        Arrays.sort(expected, new FeatureComparator());
        Assert.assertArrayEquals(a, expected);
    }

    private static class FeatureComparator implements Comparator<Feature> {
        @Override
        public int compare(Feature o1, Feature o2) {
            return o1.name().compareTo(o2.name());
        }
    }

}
