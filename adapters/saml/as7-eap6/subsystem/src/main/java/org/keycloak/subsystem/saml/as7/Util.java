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

package org.keycloak.subsystem.saml.as7;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Util {
    public static ModelNode createAddOperation(final PathAddress address) {
        return createOperation(ModelDescriptionConstants.ADD, address);
    }

    public static ModelNode createAddOperation() {
        return createEmptyOperation(ModelDescriptionConstants.ADD, null);
    }

    public static ModelNode createRemoveOperation(final PathAddress address) {
        return createOperation(ModelDescriptionConstants.REMOVE, address);
    }

    public static ModelNode createOperation(final String operationName, final PathAddress address) {
        return createEmptyOperation(operationName, address);
    }

    public static ModelNode createEmptyOperation(String operationName, final PathAddress address) {
        ModelNode op = new ModelNode();
        op.get(OP).set(operationName);
        if (address != null) {
            op.get(OP_ADDR).set(address.toModelNode());
        } else {
            // Just establish the standard structure; caller can fill in address later
            op.get(OP_ADDR);
        }
        return op;
    }
}
