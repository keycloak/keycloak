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
package org.keycloak.subsystem.server.extension;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.Feature;

import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.BiConsumer;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class FeaturesTestCase {

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


    @Test
    public void runTests() throws Exception {
        checkFeaturesAccountedFor();
        checkAllFeaturesCovered();
        checkFeaturesInSubsystemTemplate();
        checkFeaturesInOverlayCli();
    }


    private static void checkFeaturesAccountedFor() throws Exception {
        for (Feature f: Feature.values()) {
            Feature.fromCaption(f.caption());
            f.isEnabledByDefault();
        }
    }

    private static void checkAllFeaturesCovered() {
        for (Feature f: Feature.values()) {
            checkFeatureCovered(f.caption());
        }
    }

    private static void checkFeatureCovered(String caption) {
        for (int i=0; i < FEATURES.length; i+=2) {
            if (FEATURES[i].equals(caption))
                return;
        }
        Assert.fail("Feature not covered in test: " + caption);
    }

    private void checkFeaturesInSubsystemTemplate() throws Exception {

        URL file = getClass().getClassLoader().getResource("subsystem-templates/keycloak-server.xml");
        Assert.assertTrue("Expected file url", "file".equals(file.getProtocol()));

        String content = new String(Files.readAllBytes(Paths.get(file.toURI())), Charset.forName("utf-8"));
        checkStandalone(content);
    }

    private void checkFeaturesInOverlayCli() throws Exception {

        URL file = getClass().getClassLoader().getResource("cli/default-keycloak-subsys-config.cli");
        Assert.assertTrue("Expected file url", "file".equals(file.getProtocol()));

        String content = new String(Files.readAllBytes(Paths.get(file.toURI())), Charset.forName("utf-8"));
        checkOverlayCli(content);
    }

    private static void checkStandalone(String content) {
        checkStandalone(content, FEATURES);
    }

    private static void checkStandalone(String content, Object ... args) {
        forEach(
                (feature, enabled) -> checkFeatureInSubsystem(content, feature, enabled),
                args);
    }

    private static void checkOverlayCli(String content, Object ... args) {
        forEach(
                (feature, enabled) -> checkFeatureInCli(content, feature, enabled),
                args);
    }

    private static void forEach(BiConsumer<String, Boolean> f, Object ... args) {
        for (int i=0; i < args.length; i+=2) {
            String feature = (String) args[i];
            boolean enabled = (Boolean) args[i+1];

            f.accept(feature, enabled);
        }
    }

    private static void checkFeatureInSubsystem(String content, String feature, boolean enabled) {
        checkThatExists(content, feature, "<feature name=\"" + feature + "\" enabled=\"${feature." + feature + ":" + enabled + "}\"");
    }

    private static void checkFeatureInCli(String content, String feature, boolean enabled) {
        checkThatExists(content, feature, "/subsystem=keycloak-server/feature=" + feature + "/:add(enabled=\"${feature." + feature + ":" + enabled + "}\")");
    }

    private static void checkThatExists(String content, String feature, String elementText) {
        boolean found = content.indexOf(elementText) != -1;
        Assert.assertTrue(feature + " not found or mismatched", found);
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
}
