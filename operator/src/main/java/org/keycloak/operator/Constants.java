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
package org.keycloak.operator;

import java.util.Map;

public final class Constants {
    public static final String CRDS_GROUP = "keycloak.org";
    public static final String CRDS_VERSION = "v2alpha1";
    public static final String SHORT_NAME = "kc";
    public static final String NAME = "keycloak";
    public static final String PLURAL_NAME = "keycloaks";
    public static final String MANAGED_BY_LABEL = "app.kubernetes.io/managed-by";
    public static final String MANAGED_BY_VALUE = "keycloak-operator";

    public static final Map<String, String> DEFAULT_LABELS = Map.of(
            "app", NAME,
            MANAGED_BY_LABEL, MANAGED_BY_VALUE
    );

    public static final Map<String, String> DEFAULT_DIST_CONFIG = Map.of(
        "KC_METRICS_ENABLED", "true"
    );
}
