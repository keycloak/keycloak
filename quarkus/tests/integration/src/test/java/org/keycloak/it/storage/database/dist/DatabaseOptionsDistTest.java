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

package org.keycloak.it.storage.database.dist;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.WithDatabase;
import org.keycloak.it.junit5.extension.WithEnvVars;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@DistributionTest
@WithDatabase(alias = "postgres")
@Tag(DistributionTest.STORAGE)
public class DatabaseOptionsDistTest {

    @Test
    @Launch({ "start-dev", "--db-schema=foo" })
    void testSetSchema(CLIResult cliResult) {
        cliResult.assertStartedDevMode();
    }

    @Test
    @Launch({ "start-dev" })
    @WithEnvVars({ "KC_DB_USERNAME", "bad" })
    void testEnvVarPrecedenceOverConfFile(CLIResult cliResult) {
        cliResult.assertMessage("FATAL: password authentication failed for user \"bad\"");
    }

}
