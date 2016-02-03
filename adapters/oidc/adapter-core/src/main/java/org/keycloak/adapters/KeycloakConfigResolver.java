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
package org.keycloak.adapters;

import org.keycloak.adapters.spi.HttpFacade.Request;

/**
 * On multi-tenant scenarios, Keycloak will defer the resolution of a
 * KeycloakDeployment to the target application at the request-phase.
 *
 * A Request object is passed to the resolver and callers expect a complete
 * KeycloakDeployment. Based on this KeycloakDeployment, Keycloak will resume
 * authenticating and authorizing the request.
 *
 * The easiest way to build a KeycloakDeployment is to use
 * KeycloakDeploymentBuilder , passing the InputStream of an existing
 * keycloak.json to the build() method. 
 *
 * @see KeycloakDeploymentBuilder
 * @author Juraci Paixão Kröhling <juraci at kroehling.de>
 */
public interface KeycloakConfigResolver {

    public KeycloakDeployment resolve(Request facade);

}
