/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.client.registration.cli;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.keycloak.client.cli.util.HttpUtil;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public enum EndpointType {
    DEFAULT("default", "default"),
    OIDC("openid-connect", "oidc", "oidc"),
    INSTALL("install", "install", "adapter"),
    SAML2("saml2-entity-descriptor", "saml2", "saml2");

    private String endpoint;
    private String preferredName;
    private Set<String> alternativeNames;

    private EndpointType(String endpoint, String preferredName, String ... alternativeNames) {
        this.endpoint = endpoint;
        this.preferredName = preferredName;
        this.alternativeNames = new HashSet(Arrays.asList(alternativeNames));
    }

    public static EndpointType of(String name) {
        if (DEFAULT.endpoint.equals(name) || DEFAULT.alternativeNames.contains(name)) {
            return DEFAULT;
        } else if (OIDC.endpoint.equals(name) || OIDC.alternativeNames.contains(name)) {
            return OIDC;
        } else if (INSTALL.endpoint.equals(name) || INSTALL.alternativeNames.contains(name)) {
            return INSTALL;
        } else if (SAML2.endpoint.equals(name) || SAML2.alternativeNames.contains(name)) {
            return SAML2;
        }
        throw new IllegalArgumentException("Endpoint not supported: " + name);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getName() {
        return preferredName;
    }

    public static String getExpectedContentType(EndpointType type) {
        switch (type) {
            case DEFAULT:
            case OIDC:
                return HttpUtil.APPLICATION_JSON;
            case SAML2:
                return HttpUtil.APPLICATION_XML;
            default:
                throw new RuntimeException("Unsupported endpoint type: " + type);
        }
    }
}
