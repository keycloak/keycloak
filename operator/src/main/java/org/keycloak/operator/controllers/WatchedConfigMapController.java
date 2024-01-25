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

package org.keycloak.operator.controllers;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;

import org.keycloak.operator.Constants;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ControllerConfiguration(labelSelector = Constants.KEYCLOAK_COMPONENT_LABEL + "=" + WatchedResources.WATCHED_LABEL_VALUE_PREFIX + "configmap")
public class WatchedConfigMapController extends WatchedResourceController<ConfigMap> {

    public WatchedConfigMapController() {
        super(ConfigMap.class);
    }

    @Override
    Map<String, String> getData(ConfigMap resource) {
        return resource.getData();
    }

}
