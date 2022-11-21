package org.keycloak.common;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.keycloak.common.profile.CommaSeparatedListProfileConfigResolver;
import org.keycloak.common.profile.ProfileException;
import org.keycloak.common.profile.PropertiesFileProfileConfigResolver;
import org.keycloak.common.profile.PropertiesProfileConfigResolver;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;
import java.util.Set;

public class ProfileTest {

    private static final Profile.Feature DEFAULT_FEATURE = Profile.Feature.AUTHORIZATION;
    private static final Profile.Feature DISABLED_BY_DEFAULT_FEATURE = Profile.Feature.DOCKER;
    private static final Profile.Feature PREVIEW_FEATURE = Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ;
    private static final Profile.Feature EXPERIMENTAL_FEATURE = Profile.Feature.DYNAMIC_SCOPES;
    private static final Profile.Feature DEPRECATED_FEATURE = Profile.Feature.ADMIN;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeClass
    public static void beforeClass() {
        Assert.assertEquals(Profile.Feature.Type.DEFAULT, DEFAULT_FEATURE.getType());
        Assert.assertEquals(Profile.Feature.Type.DISABLED_BY_DEFAULT, DISABLED_BY_DEFAULT_FEATURE.getType());
        Assert.assertEquals(Profile.Feature.Type.PREVIEW, PREVIEW_FEATURE.getType());
        Assert.assertEquals(Profile.Feature.Type.EXPERIMENTAL, EXPERIMENTAL_FEATURE.getType());
        Assert.assertEquals(Profile.Feature.Type.DEPRECATED, DEPRECATED_FEATURE.getType());
    }

    @After
    public void afterTest() {
        Profile.defaults();
    }

    @Test
    public void checkDefaults() {
        Profile profile = Profile.defaults();

        Assert.assertTrue(Profile.isFeatureEnabled(DEFAULT_FEATURE));
        Assert.assertFalse(Profile.isFeatureEnabled(DISABLED_BY_DEFAULT_FEATURE));
        Assert.assertFalse(Profile.isFeatureEnabled(PREVIEW_FEATURE));
        Assert.assertFalse(Profile.isFeatureEnabled(EXPERIMENTAL_FEATURE));
        Assert.assertFalse(Profile.isFeatureEnabled(DEPRECATED_FEATURE));

        Assert.assertEquals(Profile.ProfileName.DEFAULT, profile.getName());
        assertEquals(profile.getDisabledFeatures(), Profile.Feature.ADMIN, Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ, Profile.Feature.DYNAMIC_SCOPES, Profile.Feature.DOCKER, Profile.Feature.RECOVERY_CODES, Profile.Feature.SCRIPTS, Profile.Feature.TOKEN_EXCHANGE, Profile.Feature.OPENSHIFT_INTEGRATION, Profile.Feature.MAP_STORAGE, Profile.Feature.DECLARATIVE_USER_PROFILE, Profile.Feature.CLIENT_SECRET_ROTATION, Profile.Feature.UPDATE_EMAIL);
        assertEquals(profile.getPreviewFeatures(), Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ, Profile.Feature.RECOVERY_CODES, Profile.Feature.SCRIPTS, Profile.Feature.TOKEN_EXCHANGE, Profile.Feature.OPENSHIFT_INTEGRATION, Profile.Feature.DECLARATIVE_USER_PROFILE, Profile.Feature.CLIENT_SECRET_ROTATION, Profile.Feature.UPDATE_EMAIL);
    }

    @Test
    public void checkFailureIfDependencyDisabled() {
        Properties properties = new Properties();
        properties.setProperty("keycloak.profile.feature.account_api", "disabled");

        try {
            Profile.configure(new PropertiesProfileConfigResolver(properties));
        } catch (ProfileException e) {
            Assert.assertEquals("Feature account2 depends on disabled feature account-api", e.getMessage());
        }
    }

    @Test
    public void checkErrorOnBadConfig() {
        Properties properties = new Properties();
        properties.setProperty("keycloak.profile.feature.account_api", "invalid");

        try {
            Profile.configure(new PropertiesProfileConfigResolver(properties));
        } catch (ProfileException e) {
            Assert.assertEquals("Invalid config value 'invalid' for feature account-api", e.getMessage());
        }
    }

    @Test
    public void enablePreviewWithProperties() {
        Properties properties = new Properties();
        properties.setProperty("keycloak.profile", "preview");
        Profile.configure(new PropertiesProfileConfigResolver(properties));

        Assert.assertEquals(Profile.ProfileName.PREVIEW, Profile.getInstance().getName());
        Assert.assertTrue(Profile.isFeatureEnabled(PREVIEW_FEATURE));
    }

    @Test
    public void enablePreviewWithCommaSeparatedList() {
        Profile.configure(new CommaSeparatedListProfileConfigResolver("preview", null));

        Assert.assertEquals(Profile.ProfileName.PREVIEW, Profile.getInstance().getName());
        Assert.assertTrue(Profile.isFeatureEnabled(PREVIEW_FEATURE));
    }

    @Test
    public void enablePreviewWithPropertiesFile() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("profile", "preview");

        Path tempDirectory = Files.createTempDirectory("jboss-config");
        System.setProperty("jboss.server.config.dir", tempDirectory.toString());

        Path profileProperties = tempDirectory.resolve("profile.properties");

        properties.store(new FileOutputStream(profileProperties.toFile()), "");

        Profile.configure(new PropertiesFileProfileConfigResolver());

        Assert.assertEquals(Profile.ProfileName.PREVIEW, Profile.getInstance().getName());
        Assert.assertTrue(Profile.isFeatureEnabled(PREVIEW_FEATURE));

        Files.delete(profileProperties);
        Files.delete(tempDirectory);
        System.getProperties().remove("jboss.server.config.dir");
    }

    @Test
    public void configWithCommaSeparatedList() {
        String enabledFeatures = DISABLED_BY_DEFAULT_FEATURE.getKey() + "," + PREVIEW_FEATURE.getKey() + "," + EXPERIMENTAL_FEATURE.getKey() + "," + DEPRECATED_FEATURE.getKey();
        String disabledFeatures = DEFAULT_FEATURE.getKey();
        Profile.configure(new CommaSeparatedListProfileConfigResolver(enabledFeatures, disabledFeatures));

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

        Profile.configure(new PropertiesProfileConfigResolver(properties));

        Assert.assertFalse(Profile.isFeatureEnabled(DEFAULT_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(DISABLED_BY_DEFAULT_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(PREVIEW_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(EXPERIMENTAL_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(DEPRECATED_FEATURE));
    }

    @Test
    public void configWithPropertiesFile() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("feature." + DEFAULT_FEATURE.name().toLowerCase(), "disabled");
        properties.setProperty("feature." + DISABLED_BY_DEFAULT_FEATURE.name().toLowerCase(), "enabled");
        properties.setProperty("feature." + PREVIEW_FEATURE.name().toLowerCase(), "enabled");
        properties.setProperty("feature." + EXPERIMENTAL_FEATURE.name().toLowerCase(), "enabled");
        properties.setProperty("feature." + DEPRECATED_FEATURE.name().toLowerCase(), "enabled");

        Path tempDirectory = Files.createTempDirectory("jboss-config");
        System.setProperty("jboss.server.config.dir", tempDirectory.toString());

        Path profileProperties = tempDirectory.resolve("profile.properties");

        properties.store(new FileOutputStream(profileProperties.toFile()), "");

        Profile.configure(new PropertiesFileProfileConfigResolver());

        Assert.assertFalse(Profile.isFeatureEnabled(DEFAULT_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(DISABLED_BY_DEFAULT_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(PREVIEW_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(EXPERIMENTAL_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(DEPRECATED_FEATURE));

        Files.delete(profileProperties);
        Files.delete(tempDirectory);
        System.getProperties().remove("jboss.server.config.dir");
    }

    @Test
    public void configWithMultipleResolvers() {
        Properties properties = new Properties();
        properties.setProperty("keycloak.profile.feature." + PREVIEW_FEATURE.name().toLowerCase(), "enabled");

        Profile.configure(new CommaSeparatedListProfileConfigResolver(DISABLED_BY_DEFAULT_FEATURE.getKey(), ""), new PropertiesProfileConfigResolver(properties));

        Assert.assertTrue(Profile.isFeatureEnabled(DISABLED_BY_DEFAULT_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(PREVIEW_FEATURE));
    }

    public static void assertEquals(Set<Profile.Feature> actual, Profile.Feature... expected) {
        Profile.Feature[] a = actual.toArray(new Profile.Feature[actual.size()]);
        Arrays.sort(a, new FeatureComparator());
        Arrays.sort(expected, new FeatureComparator());
        Assert.assertArrayEquals(a, expected);
    }

    private static class FeatureComparator implements Comparator<Profile.Feature> {
        @Override
        public int compare(Profile.Feature o1, Profile.Feature o2) {
            return o1.name().compareTo(o2.name());
        }
    }

}
