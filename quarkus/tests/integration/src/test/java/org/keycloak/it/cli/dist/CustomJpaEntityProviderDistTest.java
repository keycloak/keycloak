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

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.TestProvider;
import org.keycloak.it.utils.KeycloakDistribution;

import com.acme.provider.legacy.jpa.entity.CustomJpaEntityProvider;
import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
@Tag(DistributionTest.SMOKE)
@TestProvider(CustomJpaEntityProvider.class)
public class CustomJpaEntityProviderDistTest {

    private static final String MULTIPLE_DATASOURCES_MSG = "Multiple datasources are specified: <default>, client-store, new-user-store, pu-without-dialect-store";

    @Test
    void dbKindSpecifiedInBuildTime(KeycloakDistribution dist) {
        var result = dist.run("build", "--db=dev-file", "--db-kind-new-user-store=dev-mem", "--db-kind-pu-without-dialect-store=dev-mem");
        result.assertMessage(MULTIPLE_DATASOURCES_MSG);
        result.assertMessage("You have set DB kind for 'client-store' datasource via a Quarkus property. This approach is deprecated and you should use the Keycloak 'db-kind-client-store' property.");
        result.assertBuild();

        result = dist.run("start", "--optimized", "--http-enabled=true", "--hostname-strict=false");
        result.assertNoError("Detected additional named datasources. You need to explicitly set the DB kind for the datasource(s) to properly work as: db-kind-user-store");

        result.assertMessage("Datasource 'client-store' was deactivated automatically because its URL is not set");
        result.assertNoMessage("Datasource 'new-user-store' was deactivated automatically because its URL is not set");
        result.assertNoMessage("Datasource 'pu-without-dialect-store' was deactivated automatically because its URL is not set");
        result.assertStarted();
    }

    @Test
    @Launch({"start-dev", "--db=dev-file"})
    void notSpecifiedDbKind(CLIResult cliResult) {
        // it is printed at build time and the check done at runtime
        cliResult.assertNoMessage(MULTIPLE_DATASOURCES_MSG);
        cliResult.assertError("Detected additional named datasources without a DB kind set, please specify: db-kind-new-user-store");
    }

    @Test
    @Launch({"start-dev", "--db=dev-file", "--log-level=org.hibernate.jpa.internal.util.LogHelper:debug,org.keycloak.quarkus.deployment.KeycloakProcessor:debug", "--db-kind-new-user-store=dev-mem", "--db-kind-client-store=dev-file", "--db-kind-pu-without-dialect-store=dev-mem"})
    void testUserManagedEntityNotAddedToDefaultPU(CLIResult cliResult) {
        cliResult.assertMessage(MULTIPLE_DATASOURCES_MSG);
        cliResult.assertMessage("Datasource name 'client-store' is obtained from the 'Persistence unit name' configuration property in persistence.xml file. Use 'client-store' name for datasource options like 'db-kind-client-store'.");
        cliResult.assertMessage("Datasource name 'pu-without-dialect-store' is obtained from the 'Persistence unit name' configuration property in persistence.xml file. Use 'pu-without-dialect-store' name for datasource options like 'db-kind-pu-without-dialect-store'.");
        cliResult.assertMessage("Datasource name 'new-user-store' is obtained from the 'jakarta.persistence.jtaDataSource' configuration property in persistence.xml file. Use 'new-user-store' name for datasource options like 'db-kind-new-user-store'.");

        // tests for https://github.com/keycloak/keycloak/issues/41641
        cliResult.assertNoMessage("(JPA Startup Thread: client-store) Error while creating file");
        cliResult.assertNoMessage("(JPA Startup Thread: keycloak-default) Error while creating file");

        cliResult.assertStringCount("name: new-user-store", 1);
        cliResult.assertStringCount("name: client-store", 1);
        cliResult.assertStringCount("name: pu-without-dialect-store", 1);
        cliResult.assertStringCount("com.acme.provider.legacy.jpa.entity.Realm", 1);

        cliResult.assertMessage("jakarta.persistence.jtaDataSource: client-store");
        cliResult.assertMessage("jakarta.persistence.jtaDataSource: new-user-store");
        cliResult.assertMessage("jakarta.persistence.jtaDataSource: pu-without-dialect-store");
        cliResult.assertStringCount("hibernate.dialect: org.hibernate.dialect.H2Dialect", 4);

        cliResult.assertStartedDevMode();
    }
}
