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
package org.keycloak.operator.v2alpha1.crds;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import org.keycloak.operator.v2alpha1.crds.keycloakspec.Unsupported;

import java.util.List;
import java.util.Map;

public class KeycloakSpec {

    private int instances = 1;
    private String image;
    private Map<String, String> serverConfiguration;
    @JsonPropertyDescription("List of URLs to download Keycloak extensions.")
    private List<String> extensions;
    @JsonPropertyDescription(
        "In this section you can configure podTemplate advanced features, not production-ready, and not supported settings.\n" +
        "Use at your own risk and open an issue with your use-case if you don't find an alternative way.")
    private Unsupported unsupported;

    public List<String> getExtensions() {
        return extensions;
    }

    public void setExtensions(List<String> extensions) {
        this.extensions = extensions;
    }

    public Unsupported getUnsupported() {
        return unsupported;
    }

    public void setUnsupported(Unsupported unsupported) {
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

    public Map<String, String> getServerConfiguration() {
        return serverConfiguration;
    }

    public void setServerConfiguration(Map<String, String> serverConfiguration) {
        this.serverConfiguration = serverConfiguration;
    }
}
