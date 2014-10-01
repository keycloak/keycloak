/*
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.subsystem.extension.authserver;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.keycloak.subsystem.extension.KeycloakAdapterConfigService;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;

/**
 * Remove an auth-server from a realm.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public final class AuthServerRemoveHandler extends AbstractRemoveStepHandler {

    public static AuthServerRemoveHandler INSTANCE = new AuthServerRemoveHandler();

    private AuthServerRemoveHandler() {}

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        //KeycloakAdapterConfigService.INSTANCE.removeAuthServer()
        System.out.println("*** performRuntime ** operation");
        System.out.println(operation.toString());
        System.out.println("*** performRuntime ** model");
        System.out.println(model.toString());
        String deploymentName = Util.getNameFromAddress(operation.get(ADDRESS));
        System.out.println("*** authServerName=" + deploymentName);
        if (!deploymentName.toLowerCase().endsWith(".war")) {
            deploymentName += ".war";
        }
        KeycloakAdapterConfigService.INSTANCE.removeServerDeployment(deploymentName);
    }
}
