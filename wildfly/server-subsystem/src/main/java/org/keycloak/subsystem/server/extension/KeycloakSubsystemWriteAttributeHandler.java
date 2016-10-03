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
package org.keycloak.subsystem.server.extension;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

import java.util.List;

/**
 * Update an attribute on an Auth Server.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public class KeycloakSubsystemWriteAttributeHandler extends ModelOnlyWriteAttributeHandler { //extends ReloadRequiredWriteAttributeHandler {

    public KeycloakSubsystemWriteAttributeHandler(List<AttributeDefinition> definitions) {
        this(definitions.toArray(new AttributeDefinition[definitions.size()]));
    }

    public KeycloakSubsystemWriteAttributeHandler(AttributeDefinition... definitions) {
        super(definitions);
    }

    @Override
    protected void finishModelStage(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue, ModelNode oldValue, Resource model) throws OperationFailedException {
        if (!context.isNormalServer() || attribNotChanging(attributeName, newValue, oldValue)) {
            super.finishModelStage(context, operation, attributeName, newValue, oldValue, model);
            return;
        }

        String deploymentName = ServerUtil.getDeploymentName(operation);

        if (attributeName.equals(KeycloakSubsystemDefinition.WEB_CONTEXT.getName())) {
            KeycloakAdapterConfigService.INSTANCE.setWebContext(newValue.asString());
            ServerUtil.addStepToRedeployServerWar(context, deploymentName);
        }

        super.finishModelStage(context, operation, attributeName, newValue, oldValue, model);
    }

    private boolean attribNotChanging(String attributeName, ModelNode newValue, ModelNode oldValue) {
        AttributeDefinition attribDef = KeycloakSubsystemDefinition.lookup(attributeName);
        if (!oldValue.isDefined()) {
            oldValue = attribDef.getDefaultValue();
        }
        if (!newValue.isDefined()) {
            newValue = attribDef.getDefaultValue();
        }
        return newValue.equals(oldValue);
    }
}
