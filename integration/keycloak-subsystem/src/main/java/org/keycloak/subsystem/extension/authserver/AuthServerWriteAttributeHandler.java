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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;

import java.util.List;
import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.keycloak.subsystem.extension.KeycloakAdapterConfigService;

/**
 * Update an attribute on an Auth Server.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public class AuthServerWriteAttributeHandler extends ModelOnlyWriteAttributeHandler { //extends ReloadRequiredWriteAttributeHandler {

    public AuthServerWriteAttributeHandler(List<SimpleAttributeDefinition> definitions) {
        this(definitions.toArray(new AttributeDefinition[definitions.size()]));
    }

    public AuthServerWriteAttributeHandler(AttributeDefinition... definitions) {
        super(definitions);
    }

    @Override
    protected void finishModelStage(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue, ModelNode oldValue, Resource model) throws OperationFailedException {
        if (!context.isNormalServer() || attribNotChanging(attributeName, newValue, oldValue)) {
            super.finishModelStage(context, operation, attributeName, newValue, oldValue, model);
            return;
        }

        boolean isEnabled = isEnabled(model); // is server currently enabled?
        String deploymentName = AuthServerUtil.getDeploymentName(operation);

        if (attributeName.equals(AuthServerDefinition.WEB_CONTEXT.getName())) {

            KeycloakAdapterConfigService.INSTANCE.removeServerDeployment(deploymentName);
            KeycloakAdapterConfigService.INSTANCE.addServerDeployment(deploymentName, newValue.asString());
            if (isEnabled) {
                AuthServerUtil.addStepToRedeployAuthServer(context, deploymentName);
            }
        }

        if (attributeName.equals(AuthServerDefinition.ENABLED.getName())) {
            if (!isEnabled) { // we are disabling
                AuthServerUtil.addStepToUndeployAuthServer(context, deploymentName);
            } else { // we are enabling
                AuthServerUtil.addStepToDeployAuthServer(context, deploymentName);
            }
        }

        super.finishModelStage(context, operation, attributeName, newValue, oldValue, model);
    }

    // Is auth server currently enabled?
    private boolean isEnabled(Resource model) {
        ModelNode authServer = model.getModel();
        ModelNode isEnabled = authServer.get(AuthServerDefinition.ENABLED.getName());
        if (!isEnabled.isDefined()) isEnabled = AuthServerDefinition.ENABLED.getDefaultValue();
        return isEnabled.asBoolean();
    }

    private boolean attribNotChanging(String attributeName, ModelNode newValue, ModelNode oldValue) {
        SimpleAttributeDefinition attribDef = AuthServerDefinition.lookup(attributeName);
        if (!oldValue.isDefined()) oldValue = attribDef.getDefaultValue();
        if (!newValue.isDefined()) newValue = attribDef.getDefaultValue();
        return newValue.equals(oldValue);
    }

}
