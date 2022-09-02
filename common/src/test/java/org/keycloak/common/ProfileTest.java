package org.keycloak.common;

import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;
import java.util.Set;
import org.keycloak.common.Profile.Feature;

public class ProfileTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void checkDefaultsKeycloak() {
        Assert.assertEquals("community", Profile.getName());
        assertEquals(Profile.getDisabledFeatures(), Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ, Profile.Feature.DYNAMIC_SCOPES, Profile.Feature.DOCKER, Profile.Feature.RECOVERY_CODES, Profile.Feature.SCRIPTS, Profile.Feature.TOKEN_EXCHANGE, Profile.Feature.OPENSHIFT_INTEGRATION, Profile.Feature.MAP_STORAGE, Profile.Feature.DECLARATIVE_USER_PROFILE, Feature.CLIENT_SECRET_ROTATION, Feature.UPDATE_EMAIL);
        assertEquals(Profile.getPreviewFeatures(), Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ, Profile.Feature.RECOVERY_CODES, Profile.Feature.SCRIPTS, Profile.Feature.TOKEN_EXCHANGE, Profile.Feature.OPENSHIFT_INTEGRATION, Profile.Feature.DECLARATIVE_USER_PROFILE, Feature.CLIENT_SECRET_ROTATION, Feature.UPDATE_EMAIL);
    }

    @Test
    public void checkDefaultsRH_SSO() {
        System.setProperty("keycloak.profile", "product");
        String backUpName = Version.NAME;
        Version.NAME = Profile.PRODUCT_NAME.toLowerCase();
        Profile.init();

        Assert.assertEquals("product", Profile.getName());
        assertEquals(Profile.getDisabledFeatures(), Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ, Profile.Feature.DYNAMIC_SCOPES, Profile.Feature.DOCKER,  Profile.Feature.RECOVERY_CODES, Profile.Feature.SCRIPTS, Profile.Feature.TOKEN_EXCHANGE, Profile.Feature.OPENSHIFT_INTEGRATION, Profile.Feature.MAP_STORAGE, Profile.Feature.DECLARATIVE_USER_PROFILE, Feature.CLIENT_SECRET_ROTATION, Feature.UPDATE_EMAIL);
        assertEquals(Profile.getPreviewFeatures(), Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ, Profile.Feature.RECOVERY_CODES, Profile.Feature.SCRIPTS, Profile.Feature.TOKEN_EXCHANGE, Profile.Feature.OPENSHIFT_INTEGRATION, Profile.Feature.DECLARATIVE_USER_PROFILE, Feature.CLIENT_SECRET_ROTATION, Feature.UPDATE_EMAIL);

        System.setProperty("keycloak.profile", "community");
        Version.NAME = backUpName;
        Profile.init();
    }

    @Test
    public void configWithSystemProperties() {
        Assert.assertEquals("community", Profile.getName());
        Assert.assertFalse(Profile.isFeatureEnabled(Profile.Feature.DOCKER));
        Assert.assertFalse(Profile.isFeatureEnabled(Profile.Feature.OPENSHIFT_INTEGRATION));
        assertTrue(Profile.isFeatureEnabled(Profile.Feature.IMPERSONATION));

        System.setProperty("keycloak.profile", "preview");
        System.setProperty("keycloak.profile.feature.docker", "enabled");
        System.setProperty("keycloak.profile.feature.impersonation", "disabled");
        System.setProperty("keycloak.profile.feature.upload_scripts", "enabled");

        Profile.init();

        Assert.assertEquals("preview", Profile.getName());
        assertTrue(Profile.isFeatureEnabled(Profile.Feature.DOCKER));
        assertTrue(Profile.isFeatureEnabled(Profile.Feature.OPENSHIFT_INTEGRATION));
        Assert.assertFalse(Profile.isFeatureEnabled(Profile.Feature.IMPERSONATION));

        System.getProperties().remove("keycloak.profile");
        System.getProperties().remove("keycloak.profile.feature.docker");
        System.getProperties().remove("keycloak.profile.feature.impersonation");
        System.getProperties().remove("keycloak.profile.feature.upload_scripts");

        Profile.init();
    }

    @Test
    public void configWithPropertiesFile() throws IOException {
        Assert.assertEquals("community", Profile.getName());
        Assert.assertFalse(Profile.isFeatureEnabled(Profile.Feature.DOCKER));
        assertTrue(Profile.isFeatureEnabled(Profile.Feature.IMPERSONATION));

        File d = temporaryFolder.newFolder();
        File f = new File(d, "profile.properties");

        Properties p = new Properties();
        p.setProperty("profile", "preview");
        p.setProperty("feature.docker", "enabled");
        p.setProperty("feature.impersonation", "disabled");
        p.setProperty("feature.upload_scripts", "enabled");
        PrintWriter pw = new PrintWriter(f);
        p.list(pw);
        pw.close();

        System.setProperty("jboss.server.config.dir", d.getAbsolutePath());

        Profile.init();

        Assert.assertEquals("preview", Profile.getName());
        assertTrue(Profile.isFeatureEnabled(Profile.Feature.DOCKER));
        assertTrue(Profile.isFeatureEnabled(Profile.Feature.OPENSHIFT_INTEGRATION));
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
