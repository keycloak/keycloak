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

package org.keycloak.operator.crds.v2alpha1.client;

import org.keycloak.operator.crds.v2alpha1.StatusCondition;

// TODO: we may want to simply eliminate this until a specialization is needed
public class KeycloakClientStatusCondition extends StatusCondition {
    public static final String HAS_ERRORS = "HasErrors";

    public KeycloakClientStatusCondition() {

    }

    public KeycloakClientStatusCondition(String type, Boolean status, String message, String lastTransitionTime,
            Long observedGeneration) {
        super(type, status, message, lastTransitionTime, observedGeneration);
    }

}
