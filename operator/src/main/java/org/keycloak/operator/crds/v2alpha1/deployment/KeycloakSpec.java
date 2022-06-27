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
package org.keycloak.operator.crds.v2alpha1.deployment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import org.keycloak.operator.Constants;

import javax.validation.constraints.NotNull;
import java.util.List;

public class KeycloakSpec {

    @JsonPropertyDescription("Number of Keycloak instances in HA mode. Default is 1.")
    private int instances = 1;
    @JsonPropertyDescription("Custom Keycloak image to be used.")
    private String image;
    @JsonPropertyDescription("Configuration of the Keycloak server.\n" +
            "expressed as a keys (reference: https://www.keycloak.org/server/all-config) and values that can be either direct values or references to secrets.")
    private List<ValueOrSecret> serverConfiguration; // can't use Set due to a bug in Sundrio https://github.com/sundrio/sundrio/issues/316

    // TODO: switch to this serverConfig when all the options are ported
    // private ServerConfig serverConfig;

    @NotNull
    @JsonPropertyDescription("Hostname for the Keycloak server.\n" +
            "The special value `" + Constants.INSECURE_DISABLE + "` disables the hostname strict resolution.")
    private String hostname;
    @NotNull
    @JsonPropertyDescription("A secret containing the TLS configuration for HTTPS. Reference: https://kubernetes.io/docs/concepts/configuration/secret/#tls-secrets.\n" +
            "The special value `" + Constants.INSECURE_DISABLE + "` disables https.")
    private String tlsSecret;
    @JsonPropertyDescription("Disable the default ingress.")
    private boolean disableDefaultIngress;
    @JsonPropertyDescription(
        "In this section you can configure podTemplate advanced features, not production-ready, and not supported settings.\n" +
        "Use at your own risk and open an issue with your use-case if you don't find an alternative way.")
    private KeycloakSpecUnsupported unsupported;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    @JsonIgnore
    public boolean isHostnameDisabled() {
        return this.hostname.equals(Constants.INSECURE_DISABLE);
    }

    public void setDisableDefaultIngress(boolean value) {
        this.disableDefaultIngress = value;
    }

    public boolean isDisableDefaultIngress() {
        return this.disableDefaultIngress;
    }

    public String getTlsSecret() {
        return tlsSecret;
    }

    public void setTlsSecret(String tlsSecret) {
        this.tlsSecret = tlsSecret;
    }

    @JsonIgnore
    public boolean isHttp() {
        return this.tlsSecret.equals(Constants.INSECURE_DISABLE);
    }

    public KeycloakSpecUnsupported getUnsupported() {
        return unsupported;
    }

    public void setUnsupported(KeycloakSpecUnsupported unsupported) {
        this.unsupported = unsupported;
    }

    public int getInstances() {
        return instances;
    }

    public void setInstances(int instances) {
        this.instances = instances;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<ValueOrSecret> getServerConfiguration() {
        return serverConfiguration;
    }

    public void setServerConfiguration(List<ValueOrSecret> serverConfiguration) {
        this.serverConfiguration = serverConfiguration;
    }
}
