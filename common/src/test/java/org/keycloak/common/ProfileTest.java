package org.keycloak.common;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;
import java.util.Set;

public class ProfileTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void checkDefaults() {
        Assert.assertEquals("community", Profile.getName());
        assertEquals(Profile.getDisabledFeatures(), Profile.Feature.ACCOUNT2, Profile.Feature.ACCOUNT_API, Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ, Profile.Feature.DOCKER, Profile.Feature.SCRIPTS, Profile.Feature.TOKEN_EXCHANGE, Profile.Feature.AUTHZ_DROOLS_POLICY, Profile.Feature.OPENSHIFT_INTEGRATION, Profile.Feature.DEVICE_ACTIVITY);
        assertEquals(Profile.getPreviewFeatures(), Profile.Feature.ACCOUNT_API, Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ, Profile.Feature.SCRIPTS, Profile.Feature.TOKEN_EXCHANGE, Profile.Feature.AUTHZ_DROOLS_POLICY, Profile.Feature.OPENSHIFT_INTEGRATION);
        assertEquals(Profile.getExperimentalFeatures(), Profile.Feature.ACCOUNT2, Profile.Feature.DEVICE_ACTIVITY);
    }

    @Test
    public void configWithSystemProperties() {
        Assert.assertEquals("community", Profile.getName());
        Assert.assertFalse(Profile.isFeatureEnabled(Profile.Feature.DOCKER));
        Assert.assertFalse(Profile.isFeatureEnabled(Profile.Feature.OPENSHIFT_INTEGRATION));
        Assert.assertTrue(Profile.isFeatureEnabled(Profile.Feature.IMPERSONATION));

        System.setProperty("keycloak.profile", "preview");
        System.setProperty("keycloak.profile.feature.docker", "enabled");
        System.setProperty("keycloak.profile.feature.impersonation", "disabled");

        Profile.init();

        Assert.assertEquals("preview", Profile.getName());
        Assert.assertTrue(Profile.isFeatureEnabled(Profile.Feature.DOCKER));
        Assert.assertTrue(Profile.isFeatureEnabled(Profile.Feature.OPENSHIFT_INTEGRATION));
        Assert.assertFalse(Profile.isFeatureEnabled(Profile.Feature.IMPERSONATION));

        System.getProperties().remove("keycloak.profile");
        System.getProperties().remove("keycloak.profile.feature.docker");
        System.getProperties().remove("keycloak.profile.feature.impersonation");

        Profile.init();
    }

    @Test
    public void configWithPropertiesFile() throws IOException {
        Assert.assertEquals("community", Profile.getName());
        Assert.assertFalse(Profile.isFeatureEnabled(Profile.Feature.DOCKER));
        Assert.assertTrue(Profile.isFeatureEnabled(Profile.Feature.IMPERSONATION));

        File d = temporaryFolder.newFolder();
        File f = new File(d, "profile.properties");

        Properties p = new Properties();
        p.setProperty("profile", "preview");
        p.setProperty("feature.docker", "enabled");
        p.setProperty("feature.impersonation", "disabled");
        PrintWriter pw = new PrintWriter(f);
        p.list(pw);
        pw.close();

        System.setProperty("jboss.server.config.dir", d.getAbsolutePath());

        Profile.init();

        Assert.assertEquals("preview", Profile.getName());
        Assert.assertTrue(Profile.isFeatureEnabled(Profile.Feature.DOCKER));
        Assert.assertTrue(Profile.isFeatureEnabled(Profile.Feature.OPENSHIFT_INTEGRATION));
        Assert.assertFalse(Profile.isFeatureEnabled(Profile.Feature.IMPERSONATION));

        System.getProperties().remove("jboss.server.config.dir");

        Profile.init();
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
