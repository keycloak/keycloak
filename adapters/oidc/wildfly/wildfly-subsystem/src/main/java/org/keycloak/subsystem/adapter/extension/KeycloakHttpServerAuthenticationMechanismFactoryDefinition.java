/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.subsystem.adapter.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.keycloak.subsystem.adapter.extension.KeycloakHttpServerAuthenticationMechanismFactoryDefinition.KeycloakHttpServerAuthenticationMechanismFactoryAddHandler.HTTP_SERVER_AUTHENTICATION_CAPABILITY;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.security.http.HttpServerAuthenticationMechanismFactory;

/**
 * A {@link SimpleResourceDefinition} that can be used to configure a {@link org.keycloak.adapters.elytron.KeycloakHttpServerAuthenticationMechanismFactory}
 * and expose it as a capability for other subsystems.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
class KeycloakHttpServerAuthenticationMechanismFactoryDefinition extends AbstractAdapterConfigurationDefinition {

    static final String TAG_NAME = "http-server-mechanism-factory";

    KeycloakHttpServerAuthenticationMechanismFactoryDefinition() {
        this(TAG_NAME);
    }

    KeycloakHttpServerAuthenticationMechanismFactoryDefinition(String tagName) {
        super(tagName, ALL_ATTRIBUTES, new KeycloakHttpServerAuthenticationMechanismFactoryAddHandler(), new KeycloakHttpServerAuthenticationMechanismFactoryRemoveHandler(), new KeycloakHttpServerAuthenticationMechanismFactoryWriteHandler());
    }

    /**
     * A {@link AbstractAdapterConfigurationAddHandler} that exposes a {@link KeycloakHttpServerAuthenticationMechanismFactoryDefinition}
     * as a capability through the installation of a {@link KeycloakHttpAuthenticationFactoryService}.
     *
     * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
     */
    static final class KeycloakHttpServerAuthenticationMechanismFactoryAddHandler extends AbstractAdapterConfigurationAddHandler {

        static final String HTTP_SERVER_AUTHENTICATION_CAPABILITY = "org.wildfly.security.http-server-mechanism-factory";
        static final RuntimeCapability<Void> HTTP_SERVER_AUTHENTICATION_RUNTIME_CAPABILITY = RuntimeCapability
                .Builder.of(HTTP_SERVER_AUTHENTICATION_CAPABILITY, true, HttpServerAuthenticationMechanismFactory.class)
                .build();

        KeycloakHttpServerAuthenticationMechanismFactoryAddHandler() {
            super(HTTP_SERVER_AUTHENTICATION_RUNTIME_CAPABILITY, ALL_ATTRIBUTES);
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            super.performRuntime(context, operation, model);
            installCapability(context, operation);
        }

        static void installCapability(OperationContext context, ModelNode operation) {
            PathAddress pathAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
            String factoryName = pathAddress.getLastElement().getValue();
            ServiceName serviceName = context.getCapabilityServiceName(HTTP_SERVER_AUTHENTICATION_CAPABILITY, factoryName, HttpServerAuthenticationMechanismFactory.class);
            KeycloakHttpAuthenticationFactoryService service = new KeycloakHttpAuthenticationFactoryService(factoryName);
            context.getServiceTarget().addService(serviceName, service).setInitialMode(ServiceController.Mode.ACTIVE).install();
        }
    }

    /**
     * A {@link AbstractAdapterConfigurationRemoveHandler} that handles the removal of {@link KeycloakHttpServerAuthenticationMechanismFactoryDefinition}.
     *
     * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
     */
    static final class KeycloakHttpServerAuthenticationMechanismFactoryRemoveHandler extends AbstractAdapterConfigurationRemoveHandler {
        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            super.performRuntime(context, operation, model);
            PathAddress pathAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
            String factoryName = pathAddress.getLastElement().getValue();
            ServiceName serviceName = context.getCapabilityServiceName(HTTP_SERVER_AUTHENTICATION_CAPABILITY, factoryName, HttpServerAuthenticationMechanismFactory.class);

            context.removeService(serviceName);
        }

        @Override
        protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            super.recoverServices(context, operation, model);
            KeycloakHttpServerAuthenticationMechanismFactoryAddHandler.installCapability(context, operation);
        }
    }

    /**
     * A {@link AbstractAdapterConfigurationWriteAttributeHandler} that updates attributes on a {@link KeycloakHttpServerAuthenticationMechanismFactoryDefinition}.
     *
     * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
     */
    static final class KeycloakHttpServerAuthenticationMechanismFactoryWriteHandler extends AbstractAdapterConfigurationWriteAttributeHandler {
        KeycloakHttpServerAuthenticationMechanismFactoryWriteHandler() {
            super(ALL_ATTRIBUTES);
        }
    }
}
