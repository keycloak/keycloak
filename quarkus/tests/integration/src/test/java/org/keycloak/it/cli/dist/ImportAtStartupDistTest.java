/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.it.cli.dist;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.keycloak.it.junit5.extension.BeforeStartDistribution;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.it.utils.RawKeycloakDistribution;

import io.quarkus.deployment.util.FileUtil;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
public class ImportAtStartupDistTest {

    @Test
    @BeforeStartDistribution(CreateRealmConfigurationFile.class)
    @Launch({"start-dev", "--import-realm"})
    void testImport(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("Realm 'quickstart-realm' imported");
    }

    @Test
    @BeforeStartDistribution(CreateRealmConfigurationFileAndDir.class)
    @Launch({"start-dev", "--import-realm", "--log-level=org.keycloak.exportimport.ExportImportManager:debug"})
    void testImportAndIgnoreDirectory(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("Realm 'quickstart-realm' imported");
        cliResult.assertMessage("Ignoring import file because it is not a valid file");
    }

    @Test
    @BeforeStartDistribution(CreateRealmConfigurationFileWithUnsupportedExtension.class)
    @Launch({"start-dev", "--import-realm", "--log-level=org.keycloak.exportimport.ExportImportManager:debug"})
    void testIgnoreFileWithUnsupportedExtension(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("Ignoring import file because it is not a valid file");
    }

    @Test
    @EnabledOnOs(value = { OS.LINUX, OS.MAC }, disabledReason = "different shell escaping behaviour on Windows.")
    @BeforeStartDistribution(CreateRealmConfigurationFile.class)
    @Launch({"start-dev", "--import-realm=some-file"})
    void failSetValueToImportRealmOption(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertError("option '--import-realm' should be specified without 'some-file' parameter");
    }

    @Test
    @BeforeStartDistribution(CreateRealmConfigurationFile.class)
    void testImportFromFileCreatedByExportAllRealms(KeycloakDistribution dist) throws IOException {
        dist.run("start-dev", "--import-realm");
        dist.run("export", "--file=../data/import/realm.json");

        RawKeycloakDistribution rawDist = dist.unwrap(RawKeycloakDistribution.class);
        FileUtil.deleteDirectory(rawDist.getDistPath().resolve("data").resolve("h2").toAbsolutePath());

        CLIResult result = dist.run("start-dev", "--import-realm");
        result.assertMessage("Realm 'quickstart-realm' imported");
        result.assertMessage("Realm 'master' already exists. Import skipped");
    }

    @Test
    @BeforeStartDistribution(CreateRealmConfigurationFile.class)
    void testImportFromFileCreatedByExportSingleRealm(KeycloakDistribution dist) throws IOException {
        dist.run("start-dev", "--import-realm");
        dist.run("export", "--realm=quickstart-realm", "--file=../data/import/realm.json");

        RawKeycloakDistribution rawDist = dist.unwrap(RawKeycloakDistribution.class);
        FileUtil.deleteDirectory(rawDist.getDistPath().resolve("data").resolve("h2").toAbsolutePath());

        CLIResult result = dist.run("start-dev", "--import-realm");
        result.assertMessage("Realm 'quickstart-realm' imported");
        result.assertNoMessage("Not importing realm master from file");
    }

    @Test
    @BeforeStartDistribution(CreateRealmConfigurationFile.class)
    void testImportFromDirCreatedByExport(KeycloakDistribution dist) throws IOException {
        dist.run("start-dev", "--import-realm");
        RawKeycloakDistribution rawDist = dist.unwrap(RawKeycloakDistribution.class);
        FileUtil.deleteDirectory(rawDist.getDistPath().resolve("data").resolve("import").toAbsolutePath());
        dist.run("export", "--dir=../data/import");

        FileUtil.deleteDirectory(rawDist.getDistPath().resolve("data").resolve("h2").toAbsolutePath());

        CLIResult result = dist.run("start-dev", "--import-realm");
        result.assertMessage("Realm 'quickstart-realm' imported");
        result.assertNoMessage("Not importing realm master from file");
    }

    public static class CreateRealmConfigurationFile implements Consumer<KeycloakDistribution> {

        @Override
        public void accept(KeycloakDistribution distribution) {
            distribution.copyOrReplaceFileFromClasspath("/quickstart-realm.json", Path.of("data", "import", "realm.json"));
        }
    }

    public static class CreateRealmConfigurationFileAndDir implements Consumer<KeycloakDistribution> {

        @Override
        public void accept(KeycloakDistribution distribution) {
            distribution.copyOrReplaceFileFromClasspath("/quickstart-realm.json", Path.of("data", "import", "realm.json"));

            RawKeycloakDistribution rawDist = distribution.unwrap(RawKeycloakDistribution.class);

            rawDist.getDistPath().resolve("data").resolve("import").resolve("sub-dir").toFile().mkdirs();
        }
    }

    public static class CreateRealmConfigurationFileWithUnsupportedExtension implements Consumer<KeycloakDistribution> {

        @Override
        public void accept(KeycloakDistribution distribution) {
            distribution.copyOrReplaceFileFromClasspath("/quickstart-realm.json", Path.of("data", "import", "realm"));
        }
    }
}
