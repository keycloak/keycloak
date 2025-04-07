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

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@DistributionTest(defaultOptions = "--db=dev-file")
@RawDistOnly(reason = "Containers are immutable")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(DistributionTest.SMOKE)
public class ImportDistTest {

    @Test
    void testImport(KeycloakDistribution dist) throws IOException {
        CLIResult cliResult = dist.run("build");

        File dir = new File("target");

        cliResult = dist.run("export", "--realm=master", "--dir=" + dir.getAbsolutePath());
        cliResult.assertMessage("Export of realm 'master' requested.");
        cliResult.assertMessage("Export finished successfully");

        // add a placeholder into the realm
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(dir, "master-realm.json");
        ObjectNode node = (ObjectNode)mapper.readTree(file);
        node.put("enabled", "${REALM_ENABLED}");
        mapper.writer().writeValue(file, node);

        dist.setEnvVar("REALM_ENABLED", "true");
        cliResult = dist.run("import", "--dir=" + dir.getAbsolutePath());
        cliResult.assertMessage("Realm 'master' imported");
        cliResult.assertMessage("Import finished successfully");
        cliResult.assertNoMessage("Changes detected in configuration");
        cliResult.assertNoMessage("Listening on: http");

        cliResult = dist.run("import");
        cliResult.assertError("Must specify either --dir or --file options.");
    }
}
