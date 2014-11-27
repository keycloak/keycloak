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

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import org.jboss.as.controller.registry.Resource;
import org.keycloak.subsystem.extension.KeycloakAdapterConfigService;

/**
 * Add an auth server.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public final class AuthServerAddHandler extends AbstractAddStepHandler {

    public static AuthServerAddHandler INSTANCE = new AuthServerAddHandler();

    private AuthServerAddHandler() {
    }

    @Override
    protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        // TODO: localize exception. get id number
        if (!operation.get(OP).asString().equals(ADD)) {
            throw new OperationFailedException("Unexpected operation for add Auth Server. operation=" + operation.toString());
        }

        ModelNode model = resource.getModel();
        for (AttributeDefinition attr : AuthServerDefinition.ALL_ATTRIBUTES) {
            attr.validateAndSet(operation, model);
        }

        // returns early if on domain controller
        if (!requiresRuntime(context)) return;

        // don't want to try to start server on host controller
        if (!context.isNormalServer()) return;


        ModelNode webContextNode = model.get(AuthServerDefinition.WEB_CONTEXT.getName());
        if (!webContextNode.isDefined()) webContextNode = AuthServerDefinition.WEB_CONTEXT.getDefaultValue();
        String webContext = webContextNode.asString();

        ModelNode isEnabled = model.get("enabled");
        boolean enabled = isEnabled.isDefined() && isEnabled.asBoolean();

        AuthServerUtil authServerUtil = new AuthServerUtil(operation);
        authServerUtil.addStepToUploadAuthServer(context, enabled);
        KeycloakAdapterConfigService.INSTANCE.addServerDeployment(authServerUtil.getDeploymentName(), webContext);
    }

    @Override
    protected boolean requiresRuntimeVerification() {
        return false;
    }
}
