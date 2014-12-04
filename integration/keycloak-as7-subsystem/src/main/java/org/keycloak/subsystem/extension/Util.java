package org.keycloak.subsystem.extension;

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
