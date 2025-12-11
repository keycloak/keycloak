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

import java.nio.file.Path;
import java.util.function.Consumer;

import org.keycloak.it.junit5.extension.BeforeStartDistribution;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.Storage;
import org.keycloak.it.utils.KeycloakDistribution;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DistributionTest(reInstall = DistributionTest.ReInstall.BEFORE_TEST)
@RawDistOnly(reason = "Not possible to mount files using docker.")
@Storage(defaultLocalCache = false)
@Tag(DistributionTest.SMOKE)
@Tag(DistributionTest.SLOW)
public class ClusterConfigDistTest {

    private static final String WARN_DEFAULT_CACHE_MUTATIONS = "Modifying the default cache configuration in the config file";

    @Test
    @Launch({ "start-dev", "--cache=ispn" })
    void changeClusterSetting(CLIResult result) {
        result.assertClusteredCache();
        result.assertMessage("ISPN000078: Starting JGroups channel `ISPN` with stack `jdbc-ping`");
    }

    @Test
    @Launch({ "start-dev", "--cache=ispn", "--cache-embedded-network-bind-address=127.0.0.1","--cache-embedded-network-bind-port=7801", "--cache-embedded-network-external-address=127.0.0.2", "--cache-embedded-network-external-port=7802"})
    void changeBindAndExternalAddress(CLIResult result) {
        result.assertClusteredCache();
        result.assertMessage("physical addresses are `[127.0.0.2:7802]`");
        result.assertMessage("ISPN000078: Starting JGroups channel `ISPN` with stack `jdbc-ping`");
    }

    @Test
    @Launch({ "start-dev", "--cache=ispn", "--cache-embedded-network-bind-address=127.0.0.1", "-Djgroups.bind.address=127.0.0.2", "-Djgroups.bind_addr=127.0.0.3"})
    void testJGroupsBindAddressPropertyAlsoExists(CLIResult result) {
        result.assertClusteredCache();
        result.assertMessage("Conflicting system property 'jgroups.bind.address' and CLI arg 'cache-embedded-network-bind-address' set, utilising CLI value '127.0.0.1'");
        result.assertMessage("Conflicting system property 'jgroups.bind_addr' and CLI arg 'cache-embedded-network-bind-address' set, utilising CLI value '127.0.0.1'");
        result.assertMessage("physical addresses are `[127.0.0.1:7800]`");
        result.assertMessage("ISPN000078: Starting JGroups channel `ISPN` with stack `jdbc-ping`");
    }

    @Test
    @Launch({ "start-dev", "--cache=ispn", "-Djgroups.bind.address=127.0.0.2", "-Djgroups.bind.port=7801"})
    void testJGroupsBindAddressProperty(CLIResult result) {
        result.assertClusteredCache();
        result.assertMessage("physical addresses are `[127.0.0.2:7801]`");
        result.assertMessage("ISPN000078: Starting JGroups channel `ISPN` with stack `jdbc-ping`");
    }

    @Test
    @Launch({ "start-dev", "--cache=ispn", "--cache-embedded-network-bind-address=match-address:127.0.0.*"})
    void testBindSiteMatches(CLIResult result) {
        result.assertClusteredCache();
        result.assertMessage("physical addresses are `[127.0.0.");
        result.assertMessage("ISPN000078: Starting JGroups channel `ISPN` with stack `jdbc-ping`");
    }

    @Test
    @Launch({ "start-dev", "--cache=ispn", "--cache-embedded-network-bind-address=SITE_LOCAL"})
    void testBindSiteLocal(CLIResult result) {
        result.assertClusteredCache();
        result.assertMessage("ISPN000078: Starting JGroups channel `ISPN` with stack `jdbc-ping`");
    }

    @Test
    @Launch({ "start-dev", "--cache=ispn", "--cache-stack=jdbc-ping-udp"})
    void testJdbcPingTCP(CLIResult result) {
        result.assertClusteredCache();
        result.assertMessage("ISPN000078: Starting JGroups channel `ISPN` with stack `jdbc-ping-udp`");
    }

    @Test
    @Launch({ "start-dev", "--cache=ispn", "--cache-stack=azure" })
    void warnDeprecatedCloudStack(CLIResult result) {
        result.assertMessage("Stack 'azure' is deprecated. We recommend to use 'jdbc-ping' instead");
    }

    @Test
    @Launch({ "start-dev", "--cache-config-file=invalid" })
    void failInvalidClusterConfig(CLIResult result) {
        result.assertError("Cache config file 'invalid' does not exist in the conf directory");
    }

    @Test
    @Launch({ "start-dev", "--cache=ispn", "--cache-stack=kubernetes" })
    void failMisConfiguredClusterStack(CLIResult result) {
        result.assertMessage("ERROR: dns_query can not be null or empty");
    }

    @Test
    @Launch({ "start-dev", "--cache=ispn", "--cache-stack=invalid" })
    void failInvalidClusterStack(CLIResult result) {
        result.assertMessage("No such JGroups stack 'invalid'");
    }

    @Test
    @Launch({ "start-dev", "--cache-config-file=cache-ispn.xml" })
    void testExplicitCacheConfigFile(CLIResult result) {
        result.assertStartedDevMode();
        result.assertClusteredCache();
    }

    @Test
    @Launch({ "start", "--cache=ispn", "--log-level=info,org.keycloak.connections.infinispan:debug", "--http-enabled=true", "--hostname-strict=false"})
    void testPrintCacheConfigurationsDebug(CLIResult result) {
        result.assertStarted();
        result.assertMessage("Infinispan configuration");
    }

    @Test
    @EnabledOnOs(value = { OS.LINUX, OS.MAC }, disabledReason = "different shell escaping behaviour on Windows.")
    @Launch({ "start", "--db=dev-file", "--log-level=info,org.infinispan.remoting.transport.jgroups.JGroupsTransport:debug","--http-enabled=true", "--hostname-strict=false" })
    void testStartDefaultsToClustering(CLIResult result) {
        result.assertStarted();
        result.assertClusteredCache();
        result.assertMessage("JGroups protocol stack: TCP");
    }

    @Test
    @EnabledOnOs(value = { OS.WINDOWS }, disabledReason = "different shell behaviour on Windows.")
    @Launch({ "start", "--db=dev-file", "--log-level=\"info,org.infinispan.remoting.transport.jgroups.JGroupsTransport:debug\"","--http-enabled=true", "--hostname-strict=false" })
    void testWinStartDefaultsToClustering(CLIResult result) {
        result.assertStarted();
        result.assertClusteredCache();
        result.assertMessage("JGroups protocol stack: TCP");
    }

    @Test
    @Launch({ "start-dev" })
    void testStartDevDefaultsToLocalCaches(CLIResult result) {
        result.assertStartedDevMode();
        result.assertLocalCache();
        result.assertNoMessage("JGroups JDBC_PING discovery enabled");
        result.assertNoMessage("JGroups Encryption enabled.");
        result.assertNoMessage("Starting JGroups certificate reload manager");
    }

    @Test
    @BeforeStartDistribution(ConfigureCacheUsingAsyncEncryption.class)
    @Launch({ "start-dev", "--cache-config-file=cache-ispn-asym-enc.xml" })
    void testCustomCacheStackInConfigFile(CLIResult result) {
        result.assertMessage("ISPN000078: Starting JGroups channel `ISPN` with stack `encrypt-udp`");
    }

    @Test
    @BeforeStartDistribution(ConfigureCacheUsingAsyncEncryption.class)
    @Launch({"start", "--cache-config-file=cache-ispn-asym-enc.xml", "--http-enabled=true", "--hostname-strict=false", "--cache-embedded-mtls-enabled=false"})
    void testCustomCacheStackInConfigFileNotDev(CLIResult result) {
        result.assertMessage("ISPN000078: Starting JGroups channel `ISPN` with stack `encrypt-udp`");
    }

    @Test
    @BeforeStartDistribution(ConfigureCustomCache.class)
    @Launch({ "start-dev", "--cache-config-file=cache-ispn-custom-cache.xml" })
    void testCustomCacheConfigurationWarning(CLIResult result) {
        result.assertMessage(WARN_DEFAULT_CACHE_MUTATIONS);
    }

    @Test
    @BeforeStartDistribution(ConfigureCustomCache.class)
    @Launch({ "start-dev", "--cache-config-file=cache-ispn-custom-cache.xml", "--cache-config-mutate=true" })
    void testCustomCacheConfigurationNoWarning(CLIResult result) {
        result.assertNoMessage(WARN_DEFAULT_CACHE_MUTATIONS);
    }

    @Test
    @BeforeStartDistribution(ConfigureCustomCache.class)
    @Launch({ "start-dev", "--cache-config-file=cache-ispn-custom-user-cache.xml"})
    void testCustomUserCacheConfigurationNoWarning(CLIResult result) {
        result.assertNoMessage(WARN_DEFAULT_CACHE_MUTATIONS);
    }

    @Test
    @Launch({ "start", "--cache=local", "--http-enabled=true", "--hostname-strict=false"})
    void testNotClustered(CLIResult result) {
        result.assertStarted();
        result.assertLocalCache();
        result.assertNoMessage("JGroups JDBC_PING discovery enabled");
        result.assertNoMessage("JGroups Encryption enabled.");
        result.assertNoMessage("Starting JGroups certificate reload manager");
        result.assertNoMessage("Modifying the default cache configuration in the config file without setting cache-config-mutate=true is deprecated.");
    }

    @Test
    @Launch({ "start-dev", "--cache-embedded-users-max-count=-1" })
    void testNegativeMaxCountIgnoredForBoundedCache(CLIResult result) {
        result.assertMessage("Ignoring unbounded max-count for cache 'users'");
    }

    @Test
    @Launch({ "start-dev", "--cache-embedded-sessions-max-count=-1", "--features-disabled=persistent-user-sessions" })
    void testNegativeMaxCountAllowedForVolatileCache(CLIResult result) {
        result.assertNoMessage("Ignoring unbounded max-count for cache 'sessions'");
    }

    public static class ConfigureCacheUsingAsyncEncryption implements Consumer<KeycloakDistribution> {

        @Override
        public void accept(KeycloakDistribution distribution) {
            distribution.copyOrReplaceFileFromClasspath("/cache-ispn-asym-enc.xml", Path.of("conf", "cache-ispn-asym-enc.xml"));
        }
    }

    public static class ConfigureCustomCache implements Consumer<KeycloakDistribution> {

        @Override
        public void accept(KeycloakDistribution distribution) {
            distribution.copyOrReplaceFileFromClasspath("/cache-ispn-custom-cache.xml", Path.of("conf", "cache-ispn-custom-cache.xml"));
        }
    }
}
