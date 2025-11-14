/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
import java.util.Arrays;

import org.keycloak.config.CachingOptions;
import org.keycloak.config.Option;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.DryRun;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;

import org.junit.jupiter.api.Test;

@DistributionTest
public class CacheEmbeddedMtlsDistTest {

    @DryRun
    @Test
    @RawDistOnly(reason = "Containers are immutable")
    public void testCacheEmbeddedMtlsDisabled(KeycloakDistribution dist) {
        for (var option : Arrays.asList(
                CachingOptions.CACHE_EMBEDDED_MTLS_TRUSTSTORE,
                CachingOptions.CACHE_EMBEDDED_MTLS_KEYSTORE,
                CachingOptions.CACHE_EMBEDDED_MTLS_KEYSTORE_PASSWORD,
                CachingOptions.CACHE_EMBEDDED_MTLS_TRUSTSTORE_PASSWORD,
                CachingOptions.CACHE_EMBEDDED_MTLS_ROTATION
        )) {
            var result = dist.run("start-dev", "--cache=ispn", "--cache-embedded-mtls-enabled=false", "--%s=1".formatted(option.getKey()));
            result.assertError("Disabled option: '--%s'. Available only when property 'cache-embedded-mtls-enabled' is enabled".formatted(option.getKey()));
        }
    }

    @DryRun
    @Test
    @RawDistOnly(reason = "Containers are immutable")
    public void testCacheEmbeddedMtlsFileValidation(KeycloakDistribution dist) {
        doFileAndPasswordValidation(dist, CachingOptions.CACHE_EMBEDDED_MTLS_KEYSTORE, CachingOptions.CACHE_EMBEDDED_MTLS_KEYSTORE_PASSWORD);
        doFileAndPasswordValidation(dist, CachingOptions.CACHE_EMBEDDED_MTLS_TRUSTSTORE, CachingOptions.CACHE_EMBEDDED_MTLS_TRUSTSTORE_PASSWORD);
    }

    @DryRun
    @Test
    @RawDistOnly(reason = "Containers are immutable")
    public void testCacheEmbeddedMtlsFileExistsValidation(KeycloakDistribution dist) throws IOException {
        var result = dist.run(
              "start-dev",
              "--cache=ispn",
              "--cache-embedded-mtls-enabled=true",
              "--cache-embedded-mtls-key-store-file=keystore.p12",
              "--cache-embedded-mtls-key-store-password=password",
              "--cache-embedded-mtls-trust-store-file=truststore.p12",
              "--cache-embedded-mtls-trust-store-password=password"
              );
        result.assertError("The 'cache-embedded-mtls-key-store-file' file 'keystore.p12' does not exist");

        File keystore = Util.createTempFile("key", ".p12");
        result = dist.run(
              "start-dev",
              "--cache=ispn",
              "--cache-embedded-mtls-enabled=true",
              "--cache-embedded-mtls-key-store-file=%s".formatted(keystore.getAbsolutePath()),
              "--cache-embedded-mtls-key-store-password=password",
              "--cache-embedded-mtls-trust-store-file=truststore.p12",
              "--cache-embedded-mtls-trust-store-password=password"
        );
        result.assertError("The 'cache-embedded-mtls-trust-store-file' file 'truststore.p12' does not exist");
    }

    @DryRun
    @Test
    @RawDistOnly(reason = "Containers are immutable")
    public void testCacheEmbeddedMtlsValidation(KeycloakDistribution dist) {
        var key = CachingOptions.CACHE_EMBEDDED_MTLS_ROTATION.getKey();
        // test zero
        var result = dist.run("start-dev", "--cache=ispn", "--cache-embedded-mtls-enabled=true", "--%s=0".formatted(key));
        result.assertError("JGroups MTLS certificate rotation in '%s' option must positive.".formatted(key));

        // test negative
        result = dist.run("start-dev", "--cache=ispn", "--cache-embedded-mtls-enabled=true", "--%s=-1".formatted(key));
        result.assertError("JGroups MTLS certificate rotation in '%s' option must positive.".formatted(key));

        // test blank
        result = dist.run("start-dev", "--cache=ispn", "--cache-embedded-mtls-enabled=true", "--%s=".formatted(key));
        result.assertError("Invalid empty value for option '--%s'".formatted(key));
    }

    @Test
    @RawDistOnly(reason = "Containers are immutable")
    public void testCacheEmbeddedMtlsEnabled(KeycloakDistribution dist) {
        var result = dist.run("start-dev", "--cache=ispn", "--cache-embedded-mtls-enabled=true");
        result.assertMessage("JGroups JDBC_PING discovery enabled.");
        result.assertMessage("JGroups Encryption enabled (mTLS).");
    }

    private void doFileAndPasswordValidation(KeycloakDistribution dist, Option<String> fileOption, Option<String> passwordOption) {
        var result = dist.run("start-dev", "--cache=ispn", "--cache-embedded-mtls-enabled=true", "--%s=file".formatted(fileOption.getKey()));
        result.assertError("The option '%s' requires '%s' to be enabled.".formatted(fileOption.getKey(), passwordOption.getKey()));

        result = dist.run("start-dev", "--cache=ispn", "--cache-embedded-mtls-enabled=true", "--%s=secret".formatted(passwordOption.getKey()));
        result.assertError("The option '%s' requires '%s' to be enabled.".formatted(passwordOption.getKey(), fileOption.getKey()));
    }
}
