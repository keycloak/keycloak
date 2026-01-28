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

import java.util.function.Consumer;

import org.keycloak.it.junit5.extension.BeforeStartDistribution;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.WithEnvVars;
import org.keycloak.it.utils.KeycloakDistribution;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@DistributionTest(defaultOptions = {"--db=dev-file", "--http-enabled=true", "--hostname-strict=false"})
@RawDistOnly(reason = "Containers are immutable")
@TestMethodOrder(OrderAnnotation.class)
public class QuarkusPropertiesAutoBuildDistTest {

    @Test
    @Launch({ "start" })
    @Order(1)
    void reAugOnFirstRun(CLIResult cliResult) {
        cliResult.assertBuild();
    }

    @Test
    @BeforeStartDistribution(EnableAdditionalConsoleHandler.class)
    @Launch({ "start" })
    @Order(2)
    @Disabled(value = "We don't properly differentiate between quarkus runtime and build time properties")
    void testQuarkusRuntimePropDoesNotTriggerReAug(CLIResult cliResult) {
        cliResult.assertNoBuild();
        cliResult.assertMessage("Keycloak is the best");
    }

    @Test
    @BeforeStartDistribution(DisableAdditionalConsoleHandler.class)
    @Launch({ "start" })
    @Order(3)
    @Disabled(value = "We don't properly differentiate between quarkus runtime and build time properties")
    void testNoReAugAfterChangingRuntimeProperty(CLIResult cliResult) {
        cliResult.assertNoBuild();
        cliResult.assertNoMessage("Keycloak is the best");
    }

    @Test
    @BeforeStartDistribution(AddAdditionalDatasource.class)
    @Launch({ "start" })
    @Order(4)
    void testReAugForAdditionalDatasource(CLIResult cliResult) {
        cliResult.assertBuild();
    }

    @Test
    @BeforeStartDistribution(ChangeAdditionalDatasourceUsername.class)
    @Launch({ "start" })
    @Order(5)
    @Disabled(value = "We don't properly differentiate between quarkus runtime and build time properties")
    void testNoReAugForAdditionalDatasourceRuntimeProperty(CLIResult cliResult) {
        cliResult.assertNoBuild();
    }

    @Test
    @BeforeStartDistribution(ChangeAdditionalDatasourceDbKind.class)
    @Launch({ "start" })
    @Order(6)
    void testNoReAugWhenBuildTimePropertiesAreTheSame(CLIResult cliResult) {
        cliResult.assertNoBuild();
    }

    @Test
    @BeforeStartDistribution(AddAdditionalDatasource2.class)
    @Launch({ "start" })
    @Order(7)
    void testReAugWhenAnotherDatasourceAdded(CLIResult cliResult) {
        cliResult.assertBuild();
    }

    @Test
    @BeforeStartDistribution(SetDatabaseKind.class)
    @Launch({ "start" })
    @Order(8)
    void testWrappedBuildPropertyTriggersBuildButGetsIgnoredWhenSetByQuarkus(CLIResult cliResult) {
        cliResult.assertBuild();
        cliResult.assertStarted();
    }

    @Test
    @BeforeStartDistribution(AddNonXADatasource.class)
    @Launch({ "start" })
    @Order(9)
    void nonXADatasourceFailsToStart(CLIResult cliResult) {
        cliResult.assertError("Multiple datasources are configured but more than 1 (user-store3, <default>) is using non-XA transactions.");
    }

    @Test
    @BeforeStartDistribution(AddNonXADatasource.class)
    @WithEnvVars({"QUARKUS_TRANSACTION_MANAGER_UNSAFE_MULTIPLE_LAST_RESOURCES", "ALLOW"})
    @Launch({ "start" })
    @Order(10)
    void nonXADatasourceStart(CLIResult cliResult) {
        cliResult.assertStarted();
    }

    public static class EnableAdditionalConsoleHandler implements Consumer<KeycloakDistribution> {
        @Override
        public void accept(KeycloakDistribution distribution) {
            distribution.setQuarkusProperty("quarkus.log.handler.console.\"console-2\".enable", "true");
            distribution.setQuarkusProperty("quarkus.log.handler.console.\"console-2\".format", "Keycloak is the best");
            distribution.setQuarkusProperty("quarkus.log.handlers", "console-2");
        }
    }

    public static class DisableAdditionalConsoleHandler implements Consumer<KeycloakDistribution> {

        @Override
        public void accept(KeycloakDistribution distribution) {
            distribution.setQuarkusProperty("quarkus.log.handler.console.\"console-2\".enable", "false");
        }
    }

    public static class AddAdditionalDatasource implements Consumer<KeycloakDistribution> {
        @Override
        public void accept(KeycloakDistribution distribution) {
            distribution.setProperty("db-kind-user-store", "dev-mem");
            distribution.setProperty("db-username-user-store", "sa");
            distribution.setProperty("db-url-full-user-store", "jdbc:h2:mem:user-store;DB_CLOSE_DELAY=-1");
        }
    }

    public static class AddAdditionalDatasource2 implements Consumer<KeycloakDistribution> {
        @Override
        public void accept(KeycloakDistribution distribution) {
            distribution.setProperty("db-kind-user-store2", "dev-mem");
            distribution.setProperty("transaction-xa-enabled-user-store2", "true");
            distribution.setProperty("db-username-user-store2", "sa");
            distribution.setProperty("db-url-full-user-store2", "jdbc:h2:mem:user-store2;DB_CLOSE_DELAY=-1");
        }
    }

    public static class AddNonXADatasource implements Consumer<KeycloakDistribution> {
        @Override
        public void accept(KeycloakDistribution distribution) {
            distribution.setProperty("db-kind-user-store3", "dev-mem");
            distribution.setProperty("transaction-xa-enabled-user-store3", "false");
            distribution.setProperty("db-username-user-store3", "sa");
            distribution.setProperty("db-url-full-user-store3", "jdbc:h2:mem:user-store2;DB_CLOSE_DELAY=-1");
        }
    }

    public static class ChangeAdditionalDatasourceUsername implements Consumer<KeycloakDistribution> {
        @Override
        public void accept(KeycloakDistribution distribution) {
            distribution.setProperty("db-username-user-store", "foo");
        }
    }

    public static class ChangeAdditionalDatasourceDbKind implements Consumer<KeycloakDistribution> {
        @Override
        public void accept(KeycloakDistribution distribution) {
            distribution.setProperty("db-kind-user-store", "dev-mem");
        }
    }

    public static class SetDatabaseKind implements Consumer<KeycloakDistribution> {
        @Override
        public void accept(KeycloakDistribution distribution) {
            distribution.setManualStop(true);
            distribution.setQuarkusProperty("quarkus.datasource.db-kind", "postgres");
        }
    }
}
