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

package org.keycloak.operator.crds.v2alpha1.deployment;

import org.keycloak.operator.crds.v2alpha1.StatusCondition;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class KeycloakStatusCondition extends StatusCondition {
    public static final String READY = "Ready";
    public static final String HAS_ERRORS = "HasErrors";
    public static final String ROLLING_UPDATE = "RollingUpdate";
    public static final String UPDATE_TYPE = "RecreateUpdateUsed";
}
