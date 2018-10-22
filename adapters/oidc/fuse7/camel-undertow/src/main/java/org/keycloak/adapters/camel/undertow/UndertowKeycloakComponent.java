/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.adapters.camel.undertow;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.camel.CamelContext;
import org.apache.camel.component.undertow.UndertowComponent;
import org.apache.camel.component.undertow.UndertowEndpoint;

/**
 *
 * @author hmlnarik
 */
public class UndertowKeycloakComponent extends UndertowComponent {

    public UndertowKeycloakComponent() {
    }

    public UndertowKeycloakComponent(CamelContext context) {
        super(context);
    }

    @Override
    protected UndertowEndpoint createEndpointInstance(URI endpointUri, UndertowComponent component) throws URISyntaxException {
        return new UndertowKeycloakEndpoint(endpointUri.toString(), component);
    }

    @Override
    protected String getComponentName() {
        return "undertow-keycloak";
    }
}
