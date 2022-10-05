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

package org.keycloak.utils;

import org.keycloak.common.util.CollectionUtil;
import org.keycloak.operator.controllers.KeycloakDeployment;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusBuilder;

import java.util.List;

/**
 * Util class for managing status of the deployment
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class DeploymentStatusUtils {

    /**
     * Assume the specified first-class citizens are not included in the general server configuration
     *
     * @param keycloakDeployment Keycloak deployment
     * @param status             Status of the deployment
     * @param firstClassCitizens First-class citizens in the CR
     */
    public static void assumeFirstClassCitizens(KeycloakDeployment keycloakDeployment, KeycloakStatusBuilder status, String... firstClassCitizens) {
        var configNames = keycloakDeployment.getServerConfigNames();

        if (CollectionUtil.elementsArePresent(configNames, firstClassCitizens)) {
            status.addNotReadyMessage("You need to specify these fields as the first-class citizen of the CR: "
                    + CollectionUtil.join(List.of(firstClassCitizens), ","));
        }
    }

}
