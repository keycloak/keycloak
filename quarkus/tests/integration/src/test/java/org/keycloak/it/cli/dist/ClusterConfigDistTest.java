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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.keycloak.it.junit5.extension.BeforeStartDistribution;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;

@DistributionTest(reInstall = DistributionTest.ReInstall.BEFORE_TEST)
@RawDistOnly(reason = "Not possible to mount files using docker.")
public class ClusterConfigDistTest {

    @Test
    @Launch({ "start-dev", "--cache=ispn" })
    void changeClusterSetting(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertClusteredCache();
    }

    @Test
    @Launch({ "build", "--cache-config-file=invalid" })
    void failInvalidClusterConfig(LaunchResult result) {
        assertTrue(result.getErrorOutput().contains("ERROR: Could not load cluster configuration file"));
    }

    @Test
    @Launch({ "start-dev", "--cache=ispn", "--cache-stack=kubernetes" })
    void failMisConfiguredClusterStack(LaunchResult result) {
        assertTrue(result.getOutput().contains("ERROR: dns_query can not be null or empty"));
    }

    @Test
    @Launch({ "build", "--cache-stack=invalid" })
    void failInvalidClusterStack(LaunchResult result) {
        assertTrue(result.getErrorOutput().contains("Invalid value for option '--cache-stack': invalid. Expected values are: tcp, udp, kubernetes, ec2, azure, google"));
    }

    @Test
    @Launch({ "start-dev", "--cache-config-file=cache-ispn.xml" })
    void testExplicitCacheConfigFile(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStartedDevMode();
        cliResult.assertClusteredCache();
    }

    @Test
    @EnabledOnOs(value = { OS.LINUX, OS.MAC }, disabledReason = "different shell escaping behaviour on Windows.")
    @Launch({ "start", "--log-level=info,org.infinispan.remoting.transport.jgroups.JGroupsTransport:debug","--http-enabled=true", "--hostname-strict=false" })
    void testStartDefaultsToClustering(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStarted();
        cliResult.assertClusteredCache();
        assertTrue(cliResult.getOutput().contains("JGroups protocol stack: UDP"));
    }

    @Test
    @EnabledOnOs(value = { OS.WINDOWS }, disabledReason = "different shell behaviour on Windows.")
    @Launch({ "start", "--log-level=\"info,org.infinispan.remoting.transport.jgroups.JGroupsTransport:debug","--http-enabled=true\"", "--hostname-strict=false" })
    void testWinStartDefaultsToClustering(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStarted();
        cliResult.assertClusteredCache();
        assertTrue(cliResult.getOutput().contains("JGroups protocol stack: UDP"));
    }

    @Test
    @Launch({ "start-dev" })
    void testStartDevDefaultsToLocalCaches(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertStartedDevMode();
        cliResult.assertLocalCache();
    }

    @Test
    @BeforeStartDistribution(ConfigureCacheUsingAsyncEncryption.class)
    @Launch({ "start-dev", "--cache-config-file=cache-ispn-asym-enc.xml" })
    void testCustomCacheStackInConfigFile(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertTrue(cliResult.getOutput().contains("ERROR: server.jks"));
    }

    public static class ConfigureCacheUsingAsyncEncryption implements Consumer<KeycloakDistribution> {

        @Override
        public void accept(KeycloakDistribution distribution) {
            distribution.copyOrReplaceFileFromClasspath("/cache-ispn-asym-enc.xml", Path.of("conf", "cache-ispn-asym-enc.xml"));
        }
    }
}
