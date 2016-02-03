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

package org.keycloak.adapters.saml.wildfly;

import io.undertow.servlet.api.DeploymentInfo;
import org.keycloak.adapters.saml.SamlDeploymentContext;
import org.keycloak.adapters.saml.undertow.SamlServletExtension;
import org.keycloak.adapters.saml.undertow.ServletSamlAuthMech;
import org.keycloak.adapters.undertow.UndertowUserSessionManagement;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class WildflySamlExtension extends SamlServletExtension {
    @Override
    protected ServletSamlAuthMech createAuthMech(DeploymentInfo deploymentInfo, SamlDeploymentContext deploymentContext, UndertowUserSessionManagement userSessionManagement) {
        return new WildflySamlAuthMech(deploymentContext, userSessionManagement, getErrorPage(deploymentInfo));
    }
}
