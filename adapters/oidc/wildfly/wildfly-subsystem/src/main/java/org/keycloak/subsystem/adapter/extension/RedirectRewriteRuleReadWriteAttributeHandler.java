/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.subsystem.adapter.extension;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author sblanc
 */
public class RedirectRewriteRuleReadWriteAttributeHandler extends AbstractWriteAttributeHandler<KeycloakAdapterConfigService> {

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                           ModelNode resolvedValue, ModelNode currentValue, AbstractWriteAttributeHandler.HandbackHolder<KeycloakAdapterConfigService> hh) throws OperationFailedException {

        KeycloakAdapterConfigService ckService = KeycloakAdapterConfigService.getInstance();
        ckService.updateRedirectRewriteRule(operation, attributeName, resolvedValue);

        hh.setHandback(ckService);

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                         ModelNode valueToRestore, ModelNode valueToRevert, KeycloakAdapterConfigService ckService) throws OperationFailedException {
        ckService.updateRedirectRewriteRule(operation, attributeName, valueToRestore);
    }

}
