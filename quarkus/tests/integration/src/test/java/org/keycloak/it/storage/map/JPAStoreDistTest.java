/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.it.storage.map;

import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.WithDatabase;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;

@DistributionTest(removeBuildOptionsAfterBuild = true)
@WithDatabase(alias = "postgres", buildOptions={"storage=jpa"})
public class JPAStoreDistTest {

    @Test
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false" })
    void testSuccessful(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertMessage("Experimental feature enabled: map_storage");
        cliResult.assertMessage("[org.keycloak.models.map.storage.jpa.liquibase.updater.MapJpaLiquibaseUpdaterProvider] (main) Initializing database schema. Using changelog META-INF/jpa-realms-changelog.xml");
        cliResult.assertStarted();
    }
}
