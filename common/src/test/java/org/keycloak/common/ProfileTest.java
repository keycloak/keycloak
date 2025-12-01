package org.keycloak.common;

import java.security.Provider;
import java.security.Security;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.keycloak.common.profile.CommaSeparatedListProfileConfigResolver;
import org.keycloak.common.profile.ProfileException;
import org.keycloak.common.profile.PropertiesProfileConfigResolver;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertThrows;

public class ProfileTest {

    private static final Profile.Feature DEFAULT_FEATURE = Profile.Feature.CLIENT_POLICIES;
    private static final Profile.Feature DISABLED_BY_DEFAULT_FEATURE = Profile.Feature.DOCKER;
    private static final Profile.Feature PREVIEW_FEATURE = Profile.Feature.TOKEN_EXCHANGE;
    private static final Profile.Feature EXPERIMENTAL_FEATURE = Profile.Feature.DYNAMIC_SCOPES;
    private static Profile.Feature DEPRECATED_FEATURE = Profile.Feature.LOGIN_V1;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeClass
    public static void beforeClass() {
        Assert.assertEquals(Profile.Feature.Type.DEFAULT, DEFAULT_FEATURE.getType());
        Assert.assertEquals(Profile.Feature.Type.DISABLED_BY_DEFAULT, DISABLED_BY_DEFAULT_FEATURE.getType());
        Assert.assertEquals(Profile.Feature.Type.PREVIEW, PREVIEW_FEATURE.getType());
        Assert.assertEquals(Profile.Feature.Type.EXPERIMENTAL, EXPERIMENTAL_FEATURE.getType());

        for (Profile.Feature feature : Profile.Feature.values()) {
            if (feature.getType().equals(Profile.Feature.Type.DEPRECATED)) {
                DEPRECATED_FEATURE = feature;
                break;
            }
        }

        if (DEPRECATED_FEATURE != null) {
            Assert.assertEquals(Profile.Feature.Type.DEPRECATED, DEPRECATED_FEATURE.getType());
        }
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
        if (DEPRECATED_FEATURE != null) {
            Assert.assertFalse(Profile.isFeatureEnabled(DEPRECATED_FEATURE));
        } else {
            MatcherAssert.assertThat(profile.getDeprecatedFeatures(), Matchers.empty());
        }

        Assert.assertEquals(Profile.ProfileName.DEFAULT, profile.getName());

        MatcherAssert.assertThat(profile.getDisabledFeatures(), Matchers.hasItem(DISABLED_BY_DEFAULT_FEATURE));
        MatcherAssert.assertThat(profile.getPreviewFeatures(), Matchers.hasItem(PREVIEW_FEATURE));
    }

    @Test
    public void checkFailureIfDependencyDisabled() {
        Properties properties = new Properties();
        properties.setProperty("keycloak.profile.feature.account_api", "disabled");

        Assert.assertEquals("Feature account-v3 depends on disabled feature account-api",
                assertThrows(ProfileException.class,
                        () -> Profile.configure(new PropertiesProfileConfigResolver(properties))).getMessage());
    }

    @Test
    public void checkSuccessIfFeatureDisabledWithDisabledDependencies() {
        Properties properties = new Properties();
        properties.setProperty("keycloak.profile.feature.account", "disabled");
        properties.setProperty("keycloak.profile.feature.account_api", "disabled");
        Profile.configure(new PropertiesProfileConfigResolver(properties));
                Assert.assertFalse(Profile.isFeatureEnabled(Profile.Feature.ACCOUNT_V3));
        Assert.assertFalse(Profile.isFeatureEnabled(Profile.Feature.ACCOUNT_API));
    }

    @Test
    public void checkErrorOnBadConfig() {
        Properties properties = new Properties();
        properties.setProperty("keycloak.profile.feature.account_api", "invalid");

        Assert.assertEquals("Invalid config value 'invalid' for feature key keycloak.profile.feature.account_api",
                assertThrows(ProfileException.class,
                        () -> Profile.configure(new PropertiesProfileConfigResolver(properties))).getMessage());
    }

    @Test
    public void wrongProfileInProperties() {
        Properties properties = new Properties();
        properties.setProperty("keycloak.profile", "experimental");

        Assert.assertEquals("Invalid profile 'experimental' specified via 'keycloak.profile' property",
                assertThrows(ProfileException.class,
                        () -> Profile.configure(new PropertiesProfileConfigResolver(properties))).getMessage());
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
    public void configWithCommaSeparatedList() {
        String enabledFeatures = DISABLED_BY_DEFAULT_FEATURE.getKey() + "," + PREVIEW_FEATURE.getKey() + "," + EXPERIMENTAL_FEATURE.getKey();
        if (DEPRECATED_FEATURE != null) {
            enabledFeatures += "," + DEPRECATED_FEATURE.getVersionedKey();
        }

        String disabledFeatures = DEFAULT_FEATURE.getKey();
        Profile.configure(new CommaSeparatedListProfileConfigResolver(enabledFeatures, disabledFeatures));

        Assert.assertFalse(Profile.isFeatureEnabled(DEFAULT_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(DISABLED_BY_DEFAULT_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(PREVIEW_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(EXPERIMENTAL_FEATURE));
        if (DEPRECATED_FEATURE != null) {
            Assert.assertTrue(Profile.isFeatureEnabled(DEPRECATED_FEATURE));
        }
    }

    @Test
    public void testKeys() {
        Assert.assertEquals("account-v3", Profile.Feature.ACCOUNT_V3.getKey());
        Assert.assertEquals("account", Profile.Feature.ACCOUNT_V3.getUnversionedKey());
        Assert.assertEquals("account:v3", Profile.Feature.ACCOUNT_V3.getVersionedKey());
    }

    @Test
    public void configWithCommaSeparatedVersionedList() {
        String enabledFeatures = DISABLED_BY_DEFAULT_FEATURE.getVersionedKey() + "," + PREVIEW_FEATURE.getVersionedKey() + "," + EXPERIMENTAL_FEATURE.getVersionedKey();
        if (DEPRECATED_FEATURE != null) {
            enabledFeatures += "," + DEPRECATED_FEATURE.getVersionedKey();
        }

        String disabledFeatures = DEFAULT_FEATURE.getUnversionedKey();
        Profile.configure(new CommaSeparatedListProfileConfigResolver(enabledFeatures, disabledFeatures));

        Assert.assertFalse(Profile.isFeatureEnabled(DEFAULT_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(DISABLED_BY_DEFAULT_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(PREVIEW_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(EXPERIMENTAL_FEATURE));
        if (DEPRECATED_FEATURE != null) {
            Assert.assertTrue(Profile.isFeatureEnabled(DEPRECATED_FEATURE));
        }
    }

    @Test
    public void configWithCommaSeparatedInvalidDisabled() {
        String disabledFeatures = DEFAULT_FEATURE.getVersionedKey();
        CommaSeparatedListProfileConfigResolver resolver = new CommaSeparatedListProfileConfigResolver(null, disabledFeatures);
        assertThrows(ProfileException.class, () -> Profile.configure(resolver));
    }

    @Test
    public void commaSeparatedVersionedConflict() {
        String enabledFeatures = DEFAULT_FEATURE.getVersionedKey();
        String disabledFeatures = DEFAULT_FEATURE.getVersionedKey();
        CommaSeparatedListProfileConfigResolver resolver = new CommaSeparatedListProfileConfigResolver(enabledFeatures, disabledFeatures);
        assertThrows(ProfileException.class, () -> Profile.configure(resolver));
    }

    @Test
    public void commaSeparatedDuplicateEnabled() {
        String enabledFeatures = DEFAULT_FEATURE.getVersionedKey() + "," + DEFAULT_FEATURE.getUnversionedKey();
        CommaSeparatedListProfileConfigResolver resolver = new CommaSeparatedListProfileConfigResolver(enabledFeatures, null);
        assertThrows(ProfileException.class, () -> Profile.configure(resolver));
    }

    @Test
    public void configWithProperties() {
        Properties properties = new Properties();
        properties.setProperty(PropertiesProfileConfigResolver.getPropertyKey(DEFAULT_FEATURE), "disabled");
        properties.setProperty(PropertiesProfileConfigResolver.getPropertyKey(DISABLED_BY_DEFAULT_FEATURE), "enabled");
        properties.setProperty(PropertiesProfileConfigResolver.getPropertyKey(PREVIEW_FEATURE), "enabled");
        properties.setProperty(PropertiesProfileConfigResolver.getPropertyKey(EXPERIMENTAL_FEATURE), "enabled");
        if (DEPRECATED_FEATURE != null) {
            properties.setProperty(PropertiesProfileConfigResolver.getPropertyKey(DEPRECATED_FEATURE), "enabled");
        }

        Profile.configure(new PropertiesProfileConfigResolver(properties));

        Assert.assertFalse(Profile.isFeatureEnabled(DEFAULT_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(DISABLED_BY_DEFAULT_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(PREVIEW_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(EXPERIMENTAL_FEATURE));
        if (DEPRECATED_FEATURE != null) {
            Assert.assertTrue(Profile.isFeatureEnabled(DEPRECATED_FEATURE));
        }
    }

    @Test
    public void configWithMultipleResolvers() {
        Properties properties = new Properties();
        properties.setProperty(PropertiesProfileConfigResolver.getPropertyKey(PREVIEW_FEATURE), "enabled");

        Profile.configure(new CommaSeparatedListProfileConfigResolver(DISABLED_BY_DEFAULT_FEATURE.getKey(), ""), new PropertiesProfileConfigResolver(properties));

        Assert.assertTrue(Profile.isFeatureEnabled(DISABLED_BY_DEFAULT_FEATURE));
        Assert.assertTrue(Profile.isFeatureEnabled(PREVIEW_FEATURE));
    }

    @Test
    public void kerberosConfigAvailability() {
        // remove SunJGSS to remove kerberos availability
        Map.Entry<Integer, Provider> removed = removeSecurityProvider("SunJGSS");
        try {
            Properties properties = new Properties();
            properties.setProperty(PropertiesProfileConfigResolver.getPropertyKey(Profile.Feature.KERBEROS), "enabled");
            ProfileException e = Assert.assertThrows(ProfileException.class, () -> Profile.configure(new PropertiesProfileConfigResolver(properties)));
            Assert.assertEquals("Feature kerberos cannot be enabled as it is not available.", e.getMessage());

            Profile.defaults();
            properties.setProperty(PropertiesProfileConfigResolver.getPropertyKey(Profile.Feature.KERBEROS), "disabled");
            Profile.configure(new PropertiesProfileConfigResolver(properties));
            Assert.assertFalse(Profile.isFeatureEnabled(Profile.Feature.KERBEROS));

            Profile.defaults();
            properties.clear();
            Profile.configure(new PropertiesProfileConfigResolver(properties));
            Assert.assertFalse(Profile.isFeatureEnabled(Profile.Feature.KERBEROS));
        } finally {
            if (removed != null) {
                Security.insertProviderAt(removed.getValue(), removed.getKey());
            }
        }
    }

    public static void assertEquals(Set<Profile.Feature> actual, Collection<Profile.Feature> expected) {
        MatcherAssert.assertThat(actual, Matchers.equalTo(expected));
    }

    public static void assertEquals(Set<Profile.Feature> actual, Profile.Feature... expected) {
        assertEquals(actual, new HashSet<>(Arrays.asList(expected)));
    }

    private static class FeatureComparator implements Comparator<Profile.Feature> {
        @Override
        public int compare(Profile.Feature o1, Profile.Feature o2) {
            return o1.name().compareTo(o2.name());
        }
    }

    private Map.Entry<Integer, Provider> removeSecurityProvider(String name) {
        int position = 1;
        for (Provider p : Security.getProviders()) {
            if (name.equals(p.getName())) {
                Security.removeProvider(name);
                return new AbstractMap.SimpleEntry<>(position, p);
            }
            position++;
        }
        return null;
    }
}
