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
import java.io.FileOutputStream;
import java.io.IOException;

import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.it.utils.RawKeycloakDistribution;
import org.keycloak.representations.idm.RealmRepresentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        cliResult.assertNoMessage("local_addr");

        // add a placeholder into the realm
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(dir, "master-realm.json");
        ObjectNode node = (ObjectNode)mapper.readTree(file);
        node.put("enabled", "${REALM_ENABLED}");
        mapper.writer().writeValue(file, node);

        dist.setEnvVar("REALM_ENABLED", "true");
        dist.setEnvVar("KC_HOSTNAME_STRICT", "false");
        dist.setEnvVar("KC_CACHE", "ispn");
        cliResult = dist.run("import", "--dir=" + dir.getAbsolutePath());
        cliResult.assertMessage("Realm 'master' imported");
        cliResult.assertMessage("Import finished successfully");
        cliResult.assertNoMessage("Changes detected in configuration");
        cliResult.assertNoMessage("Listening on: http");
        cliResult.assertNoMessage("local_addr");

        cliResult = dist.run("import");
        cliResult.assertError("Must specify either --dir or --file options.");
    }

    @Test
    void testImportNewRealm(KeycloakDistribution dist) throws IOException {
        File file = new File("target/realm.json");

        RealmRepresentation newRealm=new RealmRepresentation();
        newRealm.setRealm("anotherRealm");
        newRealm.setId("anotherRealm");
        newRealm.setEnabled(true);

        ObjectMapper mapper = new ObjectMapper();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            mapper.writeValue(fos, newRealm);
        }

        var cliResult = dist.run("import", "--file=" + file.getAbsolutePath());
        cliResult.assertMessage("Realm 'anotherRealm' imported");

        dist.setEnvVar("MY_SECRET", "admin123");

        RawKeycloakDistribution rawDist = dist.unwrap(RawKeycloakDistribution.class);
        CLIResult result = rawDist.run("bootstrap-admin", "service", "--db=dev-file", "--client-id=admin", "--client-secret:env=MY_SECRET");

        assertTrue(result.getErrorOutput().isEmpty(), result.getErrorOutput());

        rawDist.setManualStop(true);
        rawDist.run("start-dev");

        CLIResult adminResult = rawDist.kcadm("get", "realms", "--server", "http://localhost:8080", "--realm", "master", "--client", "admin", "--secret", "admin123");

        assertEquals(0, adminResult.exitCode());
        assertTrue(adminResult.getOutput().contains("anotherRealm"));
    }
}
