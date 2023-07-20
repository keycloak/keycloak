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

package org.keycloak.operator;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

import java.util.Optional;

import jakarta.annotation.Priority;

import static io.smallrye.config.SecretKeys.doLocked;

/**
 * A workaround until the operator sdk supports a generate option that only affects the manifests
 */
@Priority(275)
public class ControllerConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = 367246512037404779L;

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        ConfigValue configValue = doLocked(() -> context.proceed(name));
        if (name.equals("quarkus.operator-sdk.namespaces") && Optional.ofNullable(configValue)
                .filter(cv -> cv.getValue() == null || cv.getValue().isEmpty()).isPresent()) {
            configValue = ConfigValue.builder().withName("quarkus.operator-sdk.namespaces")
                    .withValue("JOSDK_ALL_NAMESPACES").build();
        }
        return configValue;
    }
}