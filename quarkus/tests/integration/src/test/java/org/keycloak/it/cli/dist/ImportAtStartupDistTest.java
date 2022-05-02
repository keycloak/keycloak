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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.it.junit5.extension.BeforeStartDistribution;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;

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
        cliResult.assertMessage("Imported realm quickstart-realm from file");
    }

    @Test
    @BeforeStartDistribution(CreateRealmConfigurationFile.class)
    @Launch({"start-dev", "--import-realm", "some-file"})
    void failSetValueToImportRealmOption(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertError("Instead of manually specifying the files to import, just copy them to the 'data/import' directory.");
    }

    public static class CreateRealmConfigurationFile implements Consumer<KeycloakDistribution> {

        @Override
        public void accept(KeycloakDistribution distribution) {
            distribution.copyOrReplaceFileFromClasspath("/quickstart-realm.json", Path.of("data", "import", "realm.json"));
        }
    }
}
