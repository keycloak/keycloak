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

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPeer;
import io.sundr.builder.annotations.Buildable;
import org.keycloak.operator.Constants;
import org.keycloak.operator.crds.v2alpha1.CRDUtils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakSpec;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
public class NetworkPolicySpec {

    @JsonProperty("enabled")
    @JsonPropertyDescription("Enables or disable the ingress traffic control.")
    private boolean networkPolicyEnabled = false;

    @JsonProperty("http")
    private List<NetworkPolicyPeer> httpRules;

    @JsonProperty("https")
    private List<NetworkPolicyPeer> httpsRules;

    @JsonProperty("management")
    private List<NetworkPolicyPeer> managementRules;

    public boolean isNetworkPolicyEnabled() {
        return networkPolicyEnabled;
    }

    public void setNetworkPolicyEnabled(boolean networkPolicyEnabled) {
        this.networkPolicyEnabled = networkPolicyEnabled;
    }

    public List<NetworkPolicyPeer> getHttpRules() {
        return httpRules;
    }

    public void setHttpRules(List<NetworkPolicyPeer> httpRules) {
        this.httpRules = httpRules;
    }

    public List<NetworkPolicyPeer> getHttpsRules() {
        return httpsRules;
    }

    public void setHttpsRules(List<NetworkPolicyPeer> httpsRules) {
        this.httpsRules = httpsRules;
    }

    public List<NetworkPolicyPeer> getManagementRules() {
        return managementRules;
    }

    public void setManagementRules(List<NetworkPolicyPeer> managementRules) {
        this.managementRules = managementRules;
    }

    public static Optional<NetworkPolicySpec> networkPolicySpecOf(Keycloak keycloak) {
        return CRDUtils.keycloakSpecOf(keycloak)
                .map(KeycloakSpec::getNetworkPolicySpec);
    }

    public static boolean isNetworkPolicyEnabled(Keycloak keycloak) {
        return networkPolicySpecOf(keycloak)
                .map(NetworkPolicySpec::isNetworkPolicyEnabled)
                .orElse(false);
    }

    public static String networkPolicyName(Keycloak keycloak) {
        return keycloak.getMetadata().getName() + Constants.KEYCLOAK_NETWORK_POLICY_SUFFIX;
    }

    public static Optional<List<NetworkPolicyPeer>> httpRules(Keycloak keycloak) {
        return networkPolicySpecOf(keycloak)
                .map(NetworkPolicySpec::getHttpRules);
    }

    public static Optional<List<NetworkPolicyPeer>> httpsRules(Keycloak keycloak) {
        return networkPolicySpecOf(keycloak)
                .map(NetworkPolicySpec::getHttpsRules);
    }

    public static Optional<List<NetworkPolicyPeer>> managementRules(Keycloak keycloak) {
        return networkPolicySpecOf(keycloak)
                .map(NetworkPolicySpec::getManagementRules);
    }

}
