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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.KeycloakRunner;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.TestProvider;

import com.acme.provider.legacy.jpa.entity.CustomJpaEntityProvider;
import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
@Tag(DistributionTest.SMOKE)
@TestProvider(CustomJpaEntityProvider.class)
public class CustomJpaEntityProviderDistTest {

    private static final String MULTIPLE_DATASOURCES_MSG = "Multiple datasources are specified: explicit-custom-store, <default>, client-store, new-user-store, pu-without-dialect-store";

    @Test
    void dbKindSpecifiedInBuildTime(KeycloakRunner runner) {
        var result = runner.run("build", "--db=dev-file", "--db-kind-new-user-store=dev-mem", "--db-kind-pu-without-dialect-store=dev-mem", "--db-kind-explicit-custom-store=dev-mem");
        result.assertMessage(MULTIPLE_DATASOURCES_MSG);
        result.assertMessage("You have set DB kind for 'client-store' datasource via a Quarkus property. This approach is deprecated and you should use the Keycloak 'db-kind-client-store' property.");
        result.assertBuild();

        result = runner.run("start", "--optimized", "--http-enabled=true", "--hostname-strict=false");
        result.assertNoError("Detected additional named datasources. You need to explicitly set the DB kind for the datasource(s) to properly work as: db-kind-user-store");

        result.assertMessage("Datasource 'client-store' is not active, so the 'client-store' persistence unit is skipped");
        result.assertNoMessage("Datasource 'new-user-store' is not active, so the 'new-user-store' persistence unit is skipped");
        result.assertNoMessage("Datasource 'pu-without-dialect-store' is not active, so the 'pu-without-dialect-store' persistence unit is skipped");
        result.assertNoMessage("Persistence-unit [<default>] sets unsupported properties");
        result.assertStarted();
    }

    @Test
    @Launch({"start-dev", "--db=dev-file", "--db-kind-new-user-store=dev-mem", "--db-kind-client-store=dev-file", "--db-kind-pu-without-dialect-store=dev-mem", "--db-kind-explicit-custom-store=dev-mem", "--db-enabled-client-store=false"})
    void disabledDatasourceSkippedWithoutWarning(CLIResult cliResult) {
        cliResult.assertNoMessage("Datasource 'client-store' is not active");
        cliResult.assertStartedDevMode();
    }

    @Test
    @Launch({"start-dev", "--db=dev-file"})
    void notSpecifiedDbKind(CLIResult cliResult) {
        // it is printed at build time and the check done at runtime
        cliResult.assertNoMessage(MULTIPLE_DATASOURCES_MSG);
        cliResult.assertError("Detected additional named datasources without a DB kind set, please specify: kc.db-kind-explicit-custom-store,kc.db-kind-new-user-store,kc.db-kind-pu-without-dialect-store");
    }

    @Test
    @Launch({"start-dev", "--db=dev-file", "--log-level=org.hibernate.orm.jpa:debug,org.keycloak.quarkus.deployment.KeycloakProcessor:debug", "--db-kind-new-user-store=dev-mem", "--db-kind-client-store=dev-file", "--db-kind-pu-without-dialect-store=dev-mem", "--db-kind-explicit-custom-store=dev-mem"})
    void testUserManagedEntityNotAddedToDefaultPU(CLIResult cliResult) {
        cliResult.assertMessage(MULTIPLE_DATASOURCES_MSG);
        cliResult.assertMessage("Datasource name 'client-store' is obtained from the 'Persistence unit name' configuration property in persistence.xml file. Use 'client-store' name for datasource options like 'db-kind-client-store'.");
        cliResult.assertMessage("Datasource name 'pu-without-dialect-store' is obtained from the 'Persistence unit name' configuration property in persistence.xml file. Use 'pu-without-dialect-store' name for datasource options like 'db-kind-pu-without-dialect-store'.");
        cliResult.assertMessage("Datasource name 'new-user-store' is obtained from the 'jakarta.persistence.jtaDataSource' configuration property in persistence.xml file. Use 'new-user-store' name for datasource options like 'db-kind-new-user-store'.");

        // tests for https://github.com/keycloak/keycloak/issues/41641
        cliResult.assertNoMessage("Error while creating file");

        cliResult.assertMessageWasShownExactlyNumberOfTimes("name: new-user-store", 1);
        cliResult.assertMessageWasShownExactlyNumberOfTimes("name: client-store", 1);
        cliResult.assertMessageWasShownExactlyNumberOfTimes("name: explicit-custom-store", 1);
        cliResult.assertMessageWasShownExactlyNumberOfTimes("name: pu-without-dialect-store", 1);
        assertEntityAssignedOnlyTo(cliResult.getOutput(), "com.acme.provider.legacy.jpa.entity.Realm", "new-user-store");
        assertEntityAssignedOnlyTo(cliResult.getOutput(), "com.acme.provider.legacy.jpa.entity.UnlistedEntity", "explicit-custom-store");
        assertEntityAssignedOnlyTo(cliResult.getOutput(), "com.acme.provider.legacy.jpa.entity.OrmMappedEntity", "new-user-store");

        cliResult.assertMessageWasShownExactlyNumberOfTimes("hibernate.dialect: com.acme.provider.legacy.jpa.entity.KeycloakItH2Dialect", 1);
        cliResult.assertMessageWasShownExactlyNumberOfTimes("hibernate.dialect: org.keycloak.connections.jpa.dialect.KeycloakH2Dialect", 4);

        cliResult.assertMessageWasShownExactlyNumberOfTimes("jakarta.persistence.sharedCache.mode: ENABLE_SELECTIVE", 1);
        cliResult.assertMessage("Persistence unit 'client-store' declares <jar-file>");
        cliResult.assertMessage("META-INF/ignored.jar");
        cliResult.assertMessage("which is not supported; entities from a referenced jar are not added to this unit.");
        assertFalse(cliResult.getOutput().contains("com.acme.provider.legacy.jpa.unused.IgnoredJarEntity"),
                "The entity packaged only in the unsupported referenced jar must not be added to a persistence unit");
        cliResult.assertMessageWasShownExactlyNumberOfTimes("Custom persistence unit validation mode: NONE", 1);

        cliResult.assertStartedDevMode();
    }

    private static void assertEntityAssignedOnlyTo(String output, String entityClass, String persistenceUnit) {
        var blocks = Pattern.compile("HHH008541: PersistenceUnitInfo \\[\\s+name: ([^\\r\\n]+).*?Managed classes names \\[(.*?)\\]", Pattern.DOTALL).matcher(output);
        Set<String> assignedUnits = new HashSet<>();
        while (blocks.find()) {
            if (blocks.group(2).lines().map(String::trim).anyMatch(entityClass::equals)) {
                assignedUnits.add(blocks.group(1).trim());
            }
        }
        assertEquals(Set.of(persistenceUnit), assignedUnits, "Unexpected persistence unit ownership for " + entityClass);
    }
}
