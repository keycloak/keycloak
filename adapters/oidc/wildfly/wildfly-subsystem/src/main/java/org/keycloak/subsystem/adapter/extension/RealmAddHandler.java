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

package org.keycloak.subsystem.adapter.extension;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;

/**
 * Add a new realm.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
public final class RealmAddHandler extends AbstractAddStepHandler {

    public static RealmAddHandler INSTANCE = new RealmAddHandler();

    private RealmAddHandler() {}

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        // TODO: localize exception. get id number
        if (!operation.get(OP).asString().equals(ADD)) {
            throw new OperationFailedException("Unexpected operation for add realm. operation=" + operation.toString());
        }

        for (AttributeDefinition attrib : RealmDefinition.ALL_ATTRIBUTES) {
            attrib.validateAndSet(operation, model);
        }

    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        KeycloakAdapterConfigService ckService = KeycloakAdapterConfigService.getInstance();
        ckService.addRealm(operation, context.resolveExpressions(model));
    }
}
