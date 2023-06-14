/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.operator.crds.v2alpha1;

import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HttpSpec;

import java.util.Optional;
import java.util.function.Function;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class CRDUtils {
    public static boolean isTlsConfigured(Keycloak keycloakCR) {
        var tlsSecret = getValueFromSubSpec(keycloakCR.getSpec().getHttpSpec(), HttpSpec::getTlsSecret);
        return tlsSecret.isPresent() && !tlsSecret.get().trim().isEmpty();
    }

    public static <T, R> Optional<R> getValueFromSubSpec(T subSpec, Function<T, R> valueSupplier) {
        if (subSpec != null) {
            return Optional.ofNullable(valueSupplier.apply(subSpec));
        } else {
            return Optional.empty();
        }
    }
}
