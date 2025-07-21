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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.TestProvider;
import com.acme.provider.legacy.jpa.entity.CustomJpaEntityProvider;

import io.quarkus.test.junit.main.Launch;

@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
@Tag(DistributionTest.SMOKE)
public class CustomJpaEntityProviderDistTest {

    @Test
    @TestProvider(CustomJpaEntityProvider.class)
    @Launch({ "start-dev", "--log-level=org.hibernate.jpa.internal.util.LogHelper:debug,org.keycloak.quarkus.deployment.KeycloakProcessor:debug" })
    void testUserManagedEntityNotAddedToDefaultPU(CLIResult cliResult) {
        cliResult.assertMessage("Multiple datasources are specified: <default>, user-store");
        cliResult.assertMessage("Datasource name 'user-store' is obtained from the 'jakarta.persistence.jtaDataSource' configuration property in persistence.xml file. Use 'user-store' name for datasource options like 'db-kind-user-store'.");
        cliResult.assertStringCount("name: user-store", 1);
        cliResult.assertStringCount("com.acme.provider.legacy.jpa.entity.Realm", 1);
        cliResult.assertStartedDevMode();
    }
}
