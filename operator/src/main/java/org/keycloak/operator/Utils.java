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

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.quarkus.logging.Log;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class Utils {
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

    public static <T extends HasMetadata> Optional<T> getByName(Class<T> clazz, Function<Keycloak, String> nameFunction, Keycloak primary, Context<Keycloak> context) {
        InformerEventSource<T, Keycloak> ies = (InformerEventSource<T, Keycloak>) context
                .eventSourceRetriever().getResourceEventSourceFor(clazz);
    
        return ies.get(new ResourceID(nameFunction.apply(primary), primary.getMetadata().getNamespace()));
    }

    /**
     * Set resources requests/limits for Keycloak container
     * </p>
     * If not specified in the Keycloak CR, set default values from operator config
     */
    public static void addResources(ResourceRequirements resource, Config config, Container kcContainer) {
        final ResourceRequirements resourcesSpec = Optional.ofNullable(resource).orElseGet(ResourceRequirements::new);

        // sets the min boundary when the spec is not present
        final var requests = Optional.ofNullable(resourcesSpec.getRequests()).orElseGet(HashMap::new);

        final var requestsMemory = requests.get("memory");
        final var defaultRequestsMemory = config.keycloak().resources().requests().memory();

        // Validate 'requests' memory
        if (requestsMemory != null) {
            var specifiedMemoryIsLessThanDefault = requestsMemory.getNumericalAmount().intValue() < defaultRequestsMemory.getNumericalAmount().intValue();
            if (specifiedMemoryIsLessThanDefault) {
                Log.debugf("Provided 'requests' memory ('%s') is less than used default value ('%s'). Use it in your risk, as Keycloak performance might be degraded.", requestsMemory, defaultRequestsMemory);
            }
        } else {
            requests.put("memory", defaultRequestsMemory);
        }

        // sets the max boundary when the spec is not present
        final var limits = Optional.ofNullable(resourcesSpec.getLimits()).orElseGet(HashMap::new);
        limits.putIfAbsent("memory", config.keycloak().resources().limits().memory());

        kcContainer.setResources(resourcesSpec);
    }

}
