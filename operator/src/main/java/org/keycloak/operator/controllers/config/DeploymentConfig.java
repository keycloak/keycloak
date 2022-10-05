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

package org.keycloak.operator.controllers.config;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import org.keycloak.operator.controllers.KeycloakDeployment;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusBuilder;

/**
 * An interface to represent a configuration of the particular deployment
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public interface DeploymentConfig {

    /**
     * Update status of the Keycloak Deployment
     * Mainly used for the validation of the deployment
     *
     * @param keycloakDeployment Keycloak deployment
     * @param status             Status of the deployment
     */
    void validateConfiguration(KeycloakDeployment keycloakDeployment, KeycloakStatusBuilder status);

    /**
     * Configure the particular deployment
     *
     * @param keycloakDeployment Keycloak deployment
     * @param deployment         Real deployment model for K8s
     */
    void configure(KeycloakDeployment keycloakDeployment, StatefulSet deployment);
}
