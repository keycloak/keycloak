/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.infinispan.util;

import java.util.Map;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.provider.ProviderConfigurationBuilder;

import static org.keycloak.common.Profile.Feature.CLUSTERLESS;

public final class InfinispanUtils {

    private InfinispanUtils() {
    }

    private static final String INFINISPAN_VIRTUAL_THREADS_PROP = "org.infinispan.threads.virtual";

    private static final int MIN_VT_POOL_SIZE = 2;

    // all providers have the same order
    public static final int PROVIDER_ORDER = 1;

    // provider id for embedded cache providers
    public static final String EMBEDDED_PROVIDER_ID = "infinispan";

    // provider id for remote cache providers
    public static final String REMOTE_PROVIDER_ID = "remote";

    // true if running with external infinispan mode only
    public static boolean isRemoteInfinispan() {
        return MultiSiteUtils.isMultiSiteEnabled() || Profile.isFeatureEnabled(CLUSTERLESS);
    }

    // true if running with embedded caches.
    public static boolean isEmbeddedInfinispan() {
        return !isRemoteInfinispan();
    }

    // ---- Retries on Error - Exponential Back Off ----

    // max number of retries on error.
    public static final int DEFAULT_MAX_RETRIES = 10;
    private static final String CONFIG_MAX_RETRIES = "maxRetries";

    // the base back-off time in milliseconds
    public static final int DEFAULT_RETRIES_BASE_TIME_MILLIS = 10;
    private static final String CONFIG_RETRIES_BASE_TIME_MILLIS = "retryBaseTime";

    public static void configureMaxRetries(ProviderConfigurationBuilder builder) {
        builder.property()
                .name(CONFIG_MAX_RETRIES)
                .type("int")
                .helpText("The maximum number of retries if an error occurs. A value of zero or less disable any retries.")
                .defaultValue(DEFAULT_MAX_RETRIES)
                .add();
    }

    public static void configureRetryBaseTime(ProviderConfigurationBuilder builder) {
        builder.property()
                .name(CONFIG_RETRIES_BASE_TIME_MILLIS)
                .type("int")
                .helpText("The base back-off time in milliseconds.")
                .defaultValue(DEFAULT_RETRIES_BASE_TIME_MILLIS)
                .add();
    }

    public static int getMaxRetries(Config.Scope config) {
        return Math.max(0, config.getInt(CONFIG_MAX_RETRIES, DEFAULT_MAX_RETRIES));
    }

    public static int getRetryBaseTimeMillis(Config.Scope config) {
        return Math.max(1, config.getInt(CONFIG_RETRIES_BASE_TIME_MILLIS, DEFAULT_RETRIES_BASE_TIME_MILLIS));
    }

    public static void maxRetriesToOperationalInfo(Map<String, String> map, int value) {
        map.put(CONFIG_MAX_RETRIES, Integer.toString(value));
    }

    public static void retryBaseTimeMillisToOperationalInfo(Map<String, String> map, int value) {
        map.put(CONFIG_RETRIES_BASE_TIME_MILLIS, Integer.toString(value));
    }

    public static boolean isVirtualThreadsEnabled() {
        return Boolean.parseBoolean(System.getProperty(INFINISPAN_VIRTUAL_THREADS_PROP));
    }

    public static void configureVirtualThreads() {
        // enable Infinispan and JGroups virtual threads by default
        if (System.getProperty(INFINISPAN_VIRTUAL_THREADS_PROP) == null && getParallelism() >= MIN_VT_POOL_SIZE)
            System.setProperty(INFINISPAN_VIRTUAL_THREADS_PROP, "true");
    }

    public static void ensureVirtualThreadsParallelism() {
        if (isVirtualThreadsEnabled()) {
            if (getParallelism() < MIN_VT_POOL_SIZE) {
                throw new RuntimeException("To be able to use Infinispan/JGroups virtual threads, you need to set the Java system property jdk.virtualThreadScheduler.parallelism to at least " + MIN_VT_POOL_SIZE);
            }
        }
    }

    private static int getParallelism() {
        int parallelism;
        String parallelismValue = System.getProperty("jdk.virtualThreadScheduler.parallelism");
        if (parallelismValue != null) {
            parallelism = Integer.parseInt(parallelismValue);
        } else {
            parallelism = Runtime.getRuntime().availableProcessors();
        }
        return parallelism;
    }
}
