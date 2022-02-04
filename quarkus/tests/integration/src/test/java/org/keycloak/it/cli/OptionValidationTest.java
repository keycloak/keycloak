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

package org.keycloak.it.cli;

import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.CLITest;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CLITest
public class OptionValidationTest {

    @Test
    @Launch({"build", "--db"})
    public void failMissingOptionValue(LaunchResult result) {
        assertTrue(result.getErrorOutput().contains("Missing required value for option '--db' (vendor). Expected values are: dev-file, dev-mem, mariadb, mssql, mysql, oracle, postgres"));
    }

    @Test
    @Launch({"build", "--db", "foo", "bar"})
    public void failMultipleOptionValue(LaunchResult result) {
        assertTrue(result.getErrorOutput().contains("Option '--db' expects a single value (vendor) Expected values are: dev-file, dev-mem, mariadb, mssql, mysql, oracle, postgres"));
    }

    @Test
    @Launch({"build", "--nosuch"})
    public void failUnknownOption(LaunchResult result) {
        assertEquals("Unknown option: '--nosuch'\n" +
                "Try 'kc.sh build --help' for more information on the available options.", result.getErrorOutput());
    }
}
