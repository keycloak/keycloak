/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.operator;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class Utils {
    private static final String MEMORY = "memory";

    public static boolean isOpenShift(KubernetesClient client) {
        return client.supports("operator.openshift.io/v1", "OpenShiftAPIServer");
    }

    /**
     * Returns the current timestamp in ISO 8601 format, for example "2019-07-23T09:08:12.356Z".
     * @return the current timestamp in ISO 8601 format, for example "2019-07-23T09:08:12.356Z".
     */
    public static String iso8601Now() {
        return ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    }

    public static String asBase64(String toEncode) {
        return Base64.getEncoder().encodeToString(toEncode.getBytes(StandardCharsets.UTF_8));
    }

    public static String toSelectorString(Map<String, String> labels) {
        if (labels == null || labels.isEmpty()) {
            return null;
        }
        return labels.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(","));
    }

    public static Map<String, String> allInstanceLabels(HasMetadata primary) {
        var labels = new LinkedHashMap<>(Constants.DEFAULT_LABELS);
        labels.put(Constants.INSTANCE_LABEL, primary.getMetadata().getName());
        return labels;
    }

    /**
     * Set resources requests/limits for Keycloak container
     * </p>
     * If not specified in the Keycloak CR, set default values from operator config
     */
    public static void addResources(ResourceRequirements resource, Config config, Container kcContainer) {
        final ResourceRequirementsBuilder resourcesBuilder = new ResourceRequirementsBuilder(resource);

        final var defaultMemoryRequest = config.keycloak().resources().requests().memory();
        final var defaultMemoryLimit = config.keycloak().resources().limits().memory();

        if (resourcesBuilder.getRequests() == null || resourcesBuilder.getRequests().get(MEMORY) == null) {
            resourcesBuilder.addToRequests(MEMORY, defaultMemoryRequest);
        }
        if (resourcesBuilder.getLimits() == null || resourcesBuilder.getLimits().get(MEMORY) == null) {
            resourcesBuilder.addToLimits(MEMORY, defaultMemoryLimit);
        }

        kcContainer.setResources(resourcesBuilder.build());
    }

    public static <T> String hash(List<T> current) {
        var messageDigest = getMessageDigest();

        current.stream()
                .map(Utils::getData)
                .map(Serialization::asYaml)
                .map(Utils::utf8Bytes)
                .forEachOrdered(messageDigest::update);

        return new BigInteger(1, messageDigest.digest()).toString(16);
    }

    public static String hash(String value) {
        var messageDigest = getMessageDigest();
        messageDigest.update(utf8Bytes(value));
        return new BigInteger(1, messageDigest.digest()).toString(16);
    }

    private static MessageDigest getMessageDigest() {
        // Uses a fips compliant hash
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object getData(Object object) {
        if (object instanceof Secret) {
            return ((Secret) object).getData();
        }
        if (object instanceof ConfigMap) {
            return ((ConfigMap) object).getData();
        }
        return object;
    }

    private static byte[] utf8Bytes(String string) {
        return string.getBytes(StandardCharsets.UTF_8);
    }

}
