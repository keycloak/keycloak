/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.test.distribution;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.Feature;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 *
 * This test makes sure that server-dist, and server-overlay contain proper feature configuration
 * which has different default values for community project, and for product.
 *
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class DistributionFeaturesTest {

    static final Object [] COMMUNITY_FEATURES = {
            "authorization-preview", true,
            "impersonation", true,
            "scripts-preview", true,
            "docker", false,
            "account2-preview", false,
            "token-exchange", true};

    static final Object [] PRODUCT_FEATURES = {
            "authorization-preview", false,
            "impersonation", true,
            "scripts-preview", false,
            "docker", false,
            "account2-preview", false,
            "token-exchange", false};

    static final Object [] FEATURES = isProductBuild() ? PRODUCT_FEATURES : COMMUNITY_FEATURES;

    static final String SERVER_DIST_PREFIX = isProductBuild() ? "rh-sso" : "keycloak";

    @Test
    public void ensureProductFeatureDefaults() throws IOException {
        testAllFeaturesAccountedFor();
        testServerDist();
        testServerOverlay();
    }

    private void testAllFeaturesAccountedFor() {
        for (Feature f: Feature.values()) {
            checkFeatureCovered(f.caption());
        }
    }

    private void checkFeatureCovered(String caption) {
        for (int i=0; i < FEATURES.length; i+=2) {
            if (FEATURES[i].equals(caption))
                return;
        }
        Assert.fail("Feature not covered in test: " + caption);
    }

    public void testServerDist() throws IOException {

        File zip = getServerZip();

        String dirname = zip.getName().substring(0, zip.getName().length()-4);

        // load individual config files and check their content
        ClassLoader cl = new URLClassLoader(new URL[] {zip.toURI().toURL()}, null);
        URL file = cl.getResource(dirname + "/standalone/configuration/standalone.xml");
        try {
            checkStandalone(readFully(file));

            file = cl.getResource(dirname + "/standalone/configuration/standalone-ha.xml");
            checkStandalone(readFully(file));

            file = cl.getResource(dirname + "/domain/configuration/domain.xml");
            checkDomain(readFully(file));

            file = cl.getResource(dirname + "/bin/migrate-standalone.cli");
            checkMigrateStandalone(readFully(file));

            file = cl.getResource(dirname + "/bin/migrate-standalone-ha.cli");
            checkMigrateStandalone(readFully(file));

            file = cl.getResource(dirname + "/bin/migrate-domain-standalone.cli");
            checkMigrateDomainStandalone(readFully(file));

            file = cl.getResource(dirname + "/bin/migrate-domain-clustered.cli");
            checkMigrateDomainClustered(readFully(file));
        } catch (Throwable t) {
            throw new AssertionError("Invalid content detected in " + zip.getName() + ": " + file + " :: " + t.getMessage(), t);
        }
    }

    public void testServerOverlay() throws IOException {
        File zip = getOverlayZip();

        // load individual config files and check their content
        ClassLoader cl = new URLClassLoader(new URL[] {zip.toURI().toURL()}, null);

        URL file = cl.getResource("bin/migrate-standalone.cli");
        try {
            checkMigrateStandalone(readFully(file));

            file = cl.getResource("bin/migrate-standalone-ha.cli");
            checkMigrateStandalone(readFully(file));

            file = cl.getResource("bin/migrate-domain-standalone.cli");
            checkMigrateDomainStandalone(readFully(file));

            file = cl.getResource("bin/migrate-domain-clustered.cli");
            checkMigrateDomainClustered(readFully(file));

            file = cl.getResource("bin/keycloak-install.cli");
            checkMigrateStandalone(readFully(file));

            file = cl.getResource("bin/keycloak-install-ha.cli");
            checkMigrateStandalone(readFully(file));

        } catch (Throwable t) {
            throw new AssertionError("Invalid content detected in " + zip.getName() + ": " + file + " :: " + t.getMessage(), t);
        }
    }









    private static File getServerZip() throws IOException {
        return getSingleZip("server-dist", SERVER_DIST_PREFIX + "-");
    }

    private static File getOverlayZip() throws IOException {
        return getSingleZip("server-overlay", "keycloak-overlay-");
    }

    private static File getSingleZip(String module, String prefix) throws IOException {
        String path = "../" + module + "/target";
        File dir = new File(path);
        List<String> matching = Arrays.asList(dir.list()).stream().filter(n -> n.startsWith(prefix) && n.endsWith(".zip")).collect(Collectors.toList());

        Assert.assertEquals("One .zip expected", 1, matching.size());
        return new File(path, matching.get(0));
    }

    private static void checkThatExistsStandalone(String content, String feature, boolean enabled) {
        checkThatExists(content, feature, "<feature name=\"" + feature + "\" enabled=\"${feature." + feature + ":" + enabled + "}\"");
    }

    private static void checkThatExistsStandaloneCli(String content, String feature, boolean enabled) {
        checkThatExists(content, feature, "/subsystem=keycloak-server/feature=" + feature + "/:add(enabled=\"${feature." + feature + ":" + enabled + "}\")");
    }

    private static void checkThatExistsDomain(String content, String feature, boolean enabled) {
        checkThatExistsTwice(content, feature, "<feature name=\"" + feature + "\" enabled=\"${feature." + feature + ":" + enabled + "}\"");
    }

    private static void checkThatExistsDomainCliStandalone(String content, String feature, boolean enabled) {
        checkThatExists(content, feature, "/profile=$standaloneProfile/subsystem=keycloak-server/feature=" + feature + "/:add(enabled=\"${feature." + feature + ":" + enabled + "}\")");
    }

    private static void checkThatExistsDomainCliClustered(String content, String feature, boolean enabled) {
        checkThatExists(content, feature, "/profile=$clusteredProfile/subsystem=keycloak-server/feature=" + feature + "/:add(enabled=\"${feature." + feature + ":" + enabled + "}\")");
    }

    private static void checkThatExists(String content, String feature, String elementText) {
        if (content == null) {
            throw new IllegalArgumentException("content == null");
        }
        boolean found = content.indexOf(elementText) != -1;
        Assert.assertTrue(feature + " not found or mismatched [" + elementText + "]", found);
    }

    private static void checkThatExistsTwice(String content, String feature, String elementText) {
        if (content == null) {
            throw new IllegalArgumentException("content == null");
        }
        int pos = content.indexOf(elementText);
        Assert.assertTrue(feature + " not found or mismatched", pos != -1);
        pos = content.indexOf(elementText, pos+1);
        Assert.assertTrue(feature + " not found second time or mismatched", pos != -1);
    }

    private static void checkStandalone(String content) {
        checkStandalone(content, FEATURES);
    }

    private static void checkStandalone(String content, Object ... args) {
        forEach(
                (feature, enabled) -> checkThatExistsStandalone(content, feature, enabled),
                args);
    }

    private static void checkMigrateStandalone(String content) {
        checkMigrateStandalone(content, FEATURES);
    }

    private static void checkMigrateStandalone(String content, Object ... args) {
        forEach(
                (feature, enabled) -> checkThatExistsStandaloneCli(content, feature, enabled),
                args);
    }

    private static void checkDomain(String content) {
        checkDomain(content, FEATURES);
    }

    private static void checkDomain(String content, Object ... args) {
        forEach(
                (feature, enabled) -> checkThatExistsDomain(content, feature, enabled),
                args);
    }

    private static void checkMigrateDomainStandalone(String content) {
        checkMigrateDomainStandalone(content, FEATURES);
    }

    private static void checkMigrateDomainStandalone(String content, Object ... args) {
        forEach(
                (feature, enabled) -> checkThatExistsDomainCliStandalone(content, feature, enabled),
                args);
    }

    private static void checkMigrateDomainClustered(String content) {
        checkMigrateDomainClustered(content, FEATURES);
    }

    private static void checkMigrateDomainClustered(String content, Object ... args) {
        forEach(
                (feature, enabled) -> checkThatExistsDomainCliClustered(content, feature, enabled),
                args);
    }

    private static void forEach(BiConsumer<String, Boolean> f, Object ... args) {
        for (int i=0; i < args.length; i+=2) {
            String feature = (String) args[i];
            boolean enabled = (Boolean) args[i+1];

            f.accept(feature, enabled);
        }
    }

    private static boolean isProductBuild() {
        return booleanProperty("product.build", false);
    }

    private static boolean booleanProperty(String name, boolean defaultValue) {
        String val = System.getProperty(name);
        if (val == null || "".equals(val)) {
            return defaultValue;
        }
        return Boolean.valueOf(val);
    }

    private static String readFully(URL resource) throws IOException {
        if (resource == null) {
            throw new IllegalArgumentException("Null resource");
        }
        StringWriter out = new StringWriter();
        InputStreamReader in = new InputStreamReader(resource.openStream(), Charset.forName("utf-8"));
        try {
            char[] buffer = new char[8192];
            int rc = -1;
            while ((rc = in.read(buffer)) != -1) {
                out.write(buffer, 0, rc);
            }
        } finally {
            in.close();
        }
        return out.toString();
    }
}