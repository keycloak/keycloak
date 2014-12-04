package org.keycloak.subsystem.extension;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AbstractAddStepHandlerWithAttributes extends AbstractAddStepHandler {
    protected Collection<? extends AttributeDefinition> attributes;

    public AbstractAddStepHandlerWithAttributes(){ //default constructor to preserve backward compatibility

    }

    public AbstractAddStepHandlerWithAttributes(Collection<? extends AttributeDefinition> attributes) {
        this.attributes = attributes;
    }

    /**
     * Constructs add handler
     *
     * @param attributes for which model will be populated
     */
    public AbstractAddStepHandlerWithAttributes(AttributeDefinition... attributes) {
        if (attributes.length > 0) {
            this.attributes = Arrays.asList(attributes);
        } else {
            this.attributes = Collections.emptySet();
        }
    }

    /**
     * Populate the given node in the persistent configuration model based on the values in the given operation.
     *
     * @param operation the operation
     * @param model persistent configuration model node that corresponds to the address of {@code operation}
     *
     * @throws org.jboss.as.controller.OperationFailedException if {@code operation} is invalid or populating the model otherwise fails
     */
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        if (attributes != null) {
            for (AttributeDefinition attr : attributes) {
                attr.validateAndSet(operation, model);
            }
        }
    }


}
