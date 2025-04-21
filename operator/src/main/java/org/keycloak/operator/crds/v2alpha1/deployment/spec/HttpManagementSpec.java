/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.operator.crds.v2alpha1.deployment.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.sundr.builder.annotations.Buildable;
import org.keycloak.operator.Constants;
import org.keycloak.operator.crds.v2alpha1.CRDUtils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakSpec;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class HttpManagementSpec {

    @JsonPropertyDescription("Port of the management interface.")
    private Integer port = Constants.KEYCLOAK_MANAGEMENT_PORT;

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public static int managementPort(Keycloak keycloak) {
        return CRDUtils.keycloakSpecOf(keycloak)
                .map(KeycloakSpec::getHttpManagementSpec)
                .map(HttpManagementSpec::getPort)
                .orElse(Constants.KEYCLOAK_MANAGEMENT_PORT);
    }
}
