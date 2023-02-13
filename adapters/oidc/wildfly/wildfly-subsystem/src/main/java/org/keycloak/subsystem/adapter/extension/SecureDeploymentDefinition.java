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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.security.http.HttpServerAuthenticationMechanismFactory;

/**
 * Defines attributes and operations for a secure-deployment.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
final class SecureDeploymentDefinition extends AbstractAdapterConfigurationDefinition {

    static final String TAG_NAME = "secure-deployment";

    public SecureDeploymentDefinition() {
        super(TAG_NAME, ALL_ATTRIBUTES, new SecureDeploymentAddHandler(), new SecureDeploymentRemoveHandler(), new SecureDeploymentWriteAttributeHandler());
    }

    /**
     * Add a deployment to a realm.
     *
     * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
     */
    static final class SecureDeploymentAddHandler extends AbstractAdapterConfigurationAddHandler {

        static final String HTTP_SERVER_AUTHENTICATION_CAPABILITY = "org.wildfly.security.http-server-mechanism-factory";
        static RuntimeCapability<Void> HTTP_SERVER_AUTHENTICATION_RUNTIME_CAPABILITY;

        static {
            try {
                HTTP_SERVER_AUTHENTICATION_RUNTIME_CAPABILITY = RuntimeCapability
                        .Builder.of(HTTP_SERVER_AUTHENTICATION_CAPABILITY, true, HttpServerAuthenticationMechanismFactory.class)
                        .build();
            } catch (NoClassDefFoundError ncfe) {
                // ignore, Elytron not present thus no capability will be published by this resource definition
            }
        }

        SecureDeploymentAddHandler() {
            super(HTTP_SERVER_AUTHENTICATION_RUNTIME_CAPABILITY, ALL_ATTRIBUTES);
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            super.performRuntime(context, operation, model);
            if (HTTP_SERVER_AUTHENTICATION_RUNTIME_CAPABILITY != null) {
                installCapability(context, operation);
            }
        }

        static void installCapability(OperationContext context, ModelNode operation) {
            PathAddress pathAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
            String factoryName = pathAddress.getLastElement().getValue();
            ServiceName serviceName = context.getCapabilityServiceName(HTTP_SERVER_AUTHENTICATION_CAPABILITY, factoryName, HttpServerAuthenticationMechanismFactory.class);
            KeycloakHttpAuthenticationFactoryService service = new KeycloakHttpAuthenticationFactoryService(factoryName);
            ServiceTarget serviceTarget = context.getServiceTarget();
            serviceTarget.addService(serviceName, service).setInitialMode(ServiceController.Mode.ACTIVE).install();
        }
    }

    /**
     * Remove a secure-deployment from a realm.
     *
     * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
     */
    static final class SecureDeploymentRemoveHandler extends AbstractAdapterConfigurationRemoveHandler {}

    /**
     * Update an attribute on a secure-deployment.
     *
     * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
     */
    static final class SecureDeploymentWriteAttributeHandler extends AbstractAdapterConfigurationWriteAttributeHandler {
        SecureDeploymentWriteAttributeHandler() {
            super(ALL_ATTRIBUTES);
        }
    }
}
