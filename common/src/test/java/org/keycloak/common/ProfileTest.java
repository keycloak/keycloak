package org.keycloak.common;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;
import java.util.Set;

public class ProfileTest {

    private static final Feature DEFAULT_FEATURE = Feature.AUTHORIZATION;
    private static final Feature DISABLED_BY_DEFAULT_FEATURE = Feature.DOCKER;
    private static final Feature PREVIEW_FEATURE = Feature.ADMIN_FINE_GRAINED_AUTHZ;
    private static final Feature EXPERIMENTAL_FEATURE = Feature.DYNAMIC_SCOPES;
    private static final Feature DEPRECATED_FEATURE = Feature.ADMIN;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    @BeforeClass
    public static void beforeClass() {
        Assert.assertEquals(Feature.Type.DEFAULT, DEFAULT_FEATURE.getType());
        Assert.assertEquals(Feature.Type.DISABLED_BY_DEFAULT, DISABLED_BY_DEFAULT_FEATURE.getType());
        Assert.assertEquals(Feature.Type.PREVIEW, PREVIEW_FEATURE.getType());
        Assert.assertEquals(Feature.Type.EXPERIMENTAL, EXPERIMENTAL_FEATURE.getType());
        Assert.assertEquals(Feature.Type.DEPRECATED, DEPRECATED_FEATURE.getType());
    }

    @Test
    public void checkDefaults() {
        Profile.init(null);

        Assert.assertTrue(Profile.isFeatureEnabled(DEFAULT_FEATURE));
        Assert.assertFalse(Profile.isFeatureEnabled(DISABLED_BY_DEFAULT_FEATURE));
        Assert.assertFalse(Profile.isFeatureEnabled(PREVIEW_FEATURE));
        Assert.assertFalse(Profile.isFeatureEnabled(EXPERIMENTAL_FEATURE));
        Assert.assertFalse(Profile.isFeatureEnabled(DEPRECATED_FEATURE));

        Assert.assertEquals("default", Profile.getName());
        assertEquals(Profile.getDisabledFeatures(), Feature.ADMIN, Feature.ADMIN_FINE_GRAINED_AUTHZ, Feature.DYNAMIC_SCOPES, Feature.DOCKER, Feature.RECOVERY_CODES, Feature.SCRIPTS, Feature.TOKEN_EXCHANGE, Feature.OPENSHIFT_INTEGRATION, Feature.MAP_STORAGE, Feature.DECLARATIVE_USER_PROFILE, Feature.CLIENT_SECRET_ROTATION, Feature.UPDATE_EMAIL);
        assertEquals(Profile.getPreviewFeatures(), Feature.ADMIN_FINE_GRAINED_AUTHZ, Feature.RECOVERY_CODES, Feature.SCRIPTS, Feature.TOKEN_EXCHANGE, Feature.OPENSHIFT_INTEGRATION, Feature.DECLARATIVE_USER_PROFILE, Feature.CLIENT_SECRET_ROTATION, Feature.UPDATE_EMAIL);
    }

    @Test
    public void checkFailureIfDependencyDisabled() {
        Properties properties = new Properties();
        properties.setProperty("keycloak.profile.feature.account_api", "disabled");

        try {
            Profile.init(new PropertiesProfileConfigResolver(properties));
        } catch (ProfileException e) {
            Assert.assertEquals("Feature account2 depends on disabled feature account-api", e.getMessage());
        }
    }

    @Test
    public void checkErrorOnBadConfig() {
        Properties properties = new Properties();
        properties.setProperty("keycloak.profile.feature.account_api", "invalid");

        try {
            Profile.init(new PropertiesProfileConfigResolver(properties));
        } catch (ProfileException e) {
            Assert.assertEquals("Invalid config value 'invalid' for feature account-api", e.getMessage());
        }
    }

    @Test
    public void enablePreviewWithProperties() {
        Properties properties = new Properties();
        properties.setProperty("keycloak.profile", "preview");
        Profile.init(new PropertiesProfileConfigResolver(properties));

        Assert.assertEquals("preview", Profile.getName());
        Assert.assertTrue(Profile.isFeatureEnabled(PREVIEW_FEATURE));
    }

    @Test
    public void enablePreviewWithCommaSeparatedList() {
        Profile.init(new CommaSeparatedListProfileConfigResolver("preview", null));

        Assert.assertEquals("preview", Profile.getName());
        Assert.assertTrue(Profile.isFeatureEnabled(PREVIEW_FEATURE));
    }

    @Test
    public void configWithCommaSeparatedList() {
        String enabledFeatures = DISABLED_BY_DEFAULT_FEATURE.getKey() + "," + PREVIEW_FEATURE.getKey() + "," + EXPERIMENTAL_FEATURE.getKey() + "," + DEPRECATED_FEATURE.getKey();
        String disabledFeatures = DEFAULT_FEATURE.getKey();
        Profile.init(new CommaSeparatedListProfileConfigResolver(enabledFeatures, disabledFeatures));

        Assert.assertFalse(Profile.isFeatureEnabled(DEFAULT_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(DISABLED_BY_DEFAULT_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(PREVIEW_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(EXPERIMENTAL_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(DEPRECATED_FEATURE));
    }
    @Test
    public void configWithProperties() {
        Properties properties = new Properties();
        properties.setProperty("keycloak.profile.feature." + DEFAULT_FEATURE.name().toLowerCase(), "disabled");
        properties.setProperty("keycloak.profile.feature." + DISABLED_BY_DEFAULT_FEATURE.name().toLowerCase(), "enabled");
        properties.setProperty("keycloak.profile.feature." + PREVIEW_FEATURE.name().toLowerCase(), "enabled");
        properties.setProperty("keycloak.profile.feature." + EXPERIMENTAL_FEATURE.name().toLowerCase(), "enabled");
        properties.setProperty("keycloak.profile.feature." + DEPRECATED_FEATURE.name().toLowerCase(), "enabled");

        Profile.init(new PropertiesProfileConfigResolver(properties));

        Assert.assertFalse(Profile.isFeatureEnabled(DEFAULT_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(DISABLED_BY_DEFAULT_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(PREVIEW_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(EXPERIMENTAL_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(DEPRECATED_FEATURE));
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
