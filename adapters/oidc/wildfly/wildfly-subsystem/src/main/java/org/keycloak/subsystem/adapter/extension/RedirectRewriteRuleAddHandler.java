/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.subsystem.adapter.extension;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author sblanc
 */
public class RedirectRewriteRuleAddHandler extends AbstractAddStepHandler {

    public RedirectRewriteRuleAddHandler(AttributeDefinition... attributes) {
        super(attributes);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        KeycloakAdapterConfigService ckService = KeycloakAdapterConfigService.getInstance();
        ckService.addRedirectRewriteRule(operation, context.resolveExpressions(model));
    }

}
