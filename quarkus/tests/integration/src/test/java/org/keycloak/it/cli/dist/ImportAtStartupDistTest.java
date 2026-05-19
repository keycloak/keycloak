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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.keycloak.it.junit5.extension.BeforeStartDistribution;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.KeycloakRunner;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.RawKeycloakDistribution;

import io.quarkus.deployment.util.FileUtil;
import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
@Tag(DistributionTest.WIN)
@Tag(DistributionTest.SMOKE)
@Tag(DistributionTest.SLOW)
public class ImportAtStartupDistTest {

    @Test
    @BeforeStartDistribution(CreateRealmConfigurationFile.class)
    @Launch({"start-dev", "--import-realm"})
    void testImport(CLIResult cliResult) {
        cliResult.assertMessage("Realm 'quickstart-realm' imported");
    }

    @Test
    @BeforeStartDistribution(CreateRealmConfigurationFile.class)
    void testMultipleImport(KeycloakRunner runner) throws IOException {
        RawKeycloakDistribution rawDist = runner.getDistribution(RawKeycloakDistribution.class);
        Path dir = rawDist.getDistPath().resolve("data").resolve("import");

        // add another realm
        Files.write(dir.resolve("realm2.json"), Files.readAllLines(dir.resolve("realm.json")).stream()
                .map(s -> s.replace("quickstart-realm", "other-realm")).toList());

        CLIResult cliResult = runner.run("start-dev", "--import-realm");
        cliResult.assertMessage("Realm 'quickstart-realm' imported");
        cliResult.assertMessage("Realm 'other-realm' imported");
    }

    @Test
    @BeforeStartDistribution(CreateRealmConfigurationFileAndDir.class)
    @Launch({"start-dev", "--import-realm", "--log-level=org.keycloak.exportimport.ExportImportManager:debug"})
    void testImportAndIgnoreDirectory(CLIResult cliResult) {
        cliResult.assertMessage("Realm 'quickstart-realm' imported");
        cliResult.assertMessage("Ignoring import file because it is not a valid file");
    }

    @Test
    @BeforeStartDistribution(CreateRealmConfigurationFileWithUnsupportedExtension.class)
    @Launch({"start-dev", "--import-realm", "--log-level=org.keycloak.exportimport.ExportImportManager:debug"})
    void testIgnoreFileWithUnsupportedExtension(CLIResult cliResult) {
        cliResult.assertMessage("Ignoring import file because it is not a valid file");
    }

    @Test
    @BeforeStartDistribution(CreateRealmConfigurationFile.class)
    void testImportFromFileCreatedByExportAllRealms(KeycloakRunner runner) throws IOException {
        runner.run("start-dev", "--import-realm");
        runner.run("--profile=dev", "export", "--file=../data/import/realm.json", "--verbose");

        runner.getDistribution(RawKeycloakDistribution.class).resetH2Dir();

        CLIResult result = runner.run("start-dev", "--import-realm");
        result.assertMessage("Realm 'quickstart-realm' imported");
        result.assertMessage("Realm 'master' imported");
        result.assertNoMessage("Realm 'master' already exists. Import skipped");
    }

    @Test
    @BeforeStartDistribution(CreateRealmConfigurationFile.class)
    void testImportFromFileCreatedByExportSingleRealm(KeycloakRunner runner) throws IOException {
        runner.run("start-dev", "--import-realm");
        runner.run("--profile=dev", "export", "--realm=quickstart-realm", "--file=../data/import/realm.json");

        runner.getDistribution(RawKeycloakDistribution.class).resetH2Dir();

        CLIResult result = runner.run("start-dev", "--import-realm");
        result.assertMessage("Realm 'quickstart-realm' imported");
        result.assertNoMessage("Not importing realm master from file");
    }

    @Test
    @BeforeStartDistribution(CreateRealmConfigurationFile.class)
    void testImportFromDirCreatedByExport(KeycloakRunner runner) throws IOException {
        runner.run("start-dev", "--import-realm");
        RawKeycloakDistribution rawDist = runner.getDistribution(RawKeycloakDistribution.class);
        FileUtil.deleteDirectory(rawDist.getDistPath().resolve("data").resolve("import").toAbsolutePath());
        runner.run("--profile=dev", "export", "--dir=../data/import");

        runner.getDistribution(RawKeycloakDistribution.class).resetH2Dir();

        CLIResult result = runner.run("start-dev", "--import-realm");
        result.assertMessage("Realm 'quickstart-realm' imported");
        result.assertNoMessage("Not importing realm master from file");
    }

    public static class CreateRealmConfigurationFile implements Consumer<RawKeycloakDistribution> {

        @Override
        public void accept(RawKeycloakDistribution distribution) {
            distribution.copyOrReplaceFileFromClasspath("/quickstart-realm.json", Path.of("data", "import", "realm.json"));
        }
    }

    public static class CreateRealmConfigurationFileAndDir implements Consumer<RawKeycloakDistribution> {
        @Override
        public void accept(RawKeycloakDistribution distribution) {
            distribution.copyOrReplaceFileFromClasspath("/quickstart-realm.json", Path.of("data", "import", "realm.json"));

            distribution.getDistPath().resolve("data").resolve("import").resolve("sub-dir").toFile().mkdirs();
        }
    }

    public static class CreateRealmConfigurationFileWithUnsupportedExtension implements Consumer<RawKeycloakDistribution> {

        @Override
        public void accept(RawKeycloakDistribution distribution) {
            distribution.copyOrReplaceFileFromClasspath("/quickstart-realm.json", Path.of("data", "import", "realm"));
        }
    }
}
