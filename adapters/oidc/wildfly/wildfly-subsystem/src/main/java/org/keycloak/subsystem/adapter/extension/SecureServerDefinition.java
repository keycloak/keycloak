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
import static org.keycloak.subsystem.adapter.extension.KeycloakHttpServerAuthenticationMechanismFactoryDefinition.KeycloakHttpServerAuthenticationMechanismFactoryAddHandler.HTTP_SERVER_AUTHENTICATION_CAPABILITY;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.util.ETag;
import io.undertow.util.MimeMappings;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.server.mgmt.domain.ExtensibleHttpManagement;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.security.http.HttpServerAuthenticationMechanismFactory;

/**
 * Defines attributes and operations for a secure-deployment.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
final class SecureServerDefinition extends AbstractAdapterConfigurationDefinition {

    public static final String TAG_NAME = "secure-server";

    SecureServerDefinition() {
        super(TAG_NAME, ALL_ATTRIBUTES, new SecureServerAddHandler(), new SecureServerRemoveHandler(), new SecureServerWriteHandler());
    }

    /**
     * A {@link AbstractAdapterConfigurationAddHandler} that exposes a {@link SecureServerDefinition}
     * as a capability through the installation of a {@link KeycloakHttpAuthenticationFactoryService}.
     *
     * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
     */
    static final class SecureServerAddHandler extends AbstractAdapterConfigurationAddHandler {

        static final String HTTP_SERVER_AUTHENTICATION_CAPABILITY = "org.wildfly.security.http-server-mechanism-factory";
        static final String HTTP_MANAGEMENT_HTTP_EXTENSIBLE_CAPABILITY = "org.wildfly.management.http.extensible";
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

        SecureServerAddHandler() {
            super(HTTP_SERVER_AUTHENTICATION_RUNTIME_CAPABILITY, ALL_ATTRIBUTES);
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            super.performRuntime(context, operation, model);
            if (HTTP_SERVER_AUTHENTICATION_RUNTIME_CAPABILITY != null) {
                installCapability(context, operation);
            }
        }

        static void installCapability(OperationContext context, ModelNode operation) throws OperationFailedException {
            PathAddress pathAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
            String factoryName = pathAddress.getLastElement().getValue();
            ServiceName serviceName = context.getCapabilityServiceName(HTTP_SERVER_AUTHENTICATION_CAPABILITY, factoryName, HttpServerAuthenticationMechanismFactory.class);
            boolean publicClient = SecureServerDefinition.PUBLIC_CLIENT.resolveModelAttribute(context, operation).asBoolean(false);

            if (!publicClient) {
                throw new OperationFailedException("Only public clients are allowed to have their configuration exposed through the management interface");
            }

            KeycloakHttpAuthenticationFactoryService service = new KeycloakHttpAuthenticationFactoryService(factoryName);
            ServiceTarget serviceTarget = context.getServiceTarget();
            InjectedValue<ExtensibleHttpManagement> injectedValue = new InjectedValue<>();
            serviceTarget.addService(serviceName.append("http-management-context"), createHttpManagementConfigContextService(factoryName, injectedValue))
                    .addDependency(context.getCapabilityServiceName(HTTP_MANAGEMENT_HTTP_EXTENSIBLE_CAPABILITY, ExtensibleHttpManagement.class), ExtensibleHttpManagement.class, injectedValue).setInitialMode(Mode.ACTIVE).install();
            serviceTarget.addService(serviceName, service).setInitialMode(Mode.ACTIVE).install();
        }
    }

    /**
     * A {@link AbstractAdapterConfigurationRemoveHandler} that handles the removal of {@link SecureServerDefinition}.
     *
     * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
     */
    static final class SecureServerRemoveHandler extends AbstractAdapterConfigurationRemoveHandler {
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
            SecureServerDefinition.SecureServerAddHandler.installCapability(context, operation);
        }
    }

    /**
     * A {@link AbstractAdapterConfigurationWriteAttributeHandler} that updates attributes on a {@link SecureServerDefinition}.
     *
     * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
     */
    static final class SecureServerWriteHandler extends AbstractAdapterConfigurationWriteAttributeHandler {
        SecureServerWriteHandler() {
            super(ALL_ATTRIBUTES);
        }
    }

    private static Service<Void> createHttpManagementConfigContextService(final String factoryName, final InjectedValue<ExtensibleHttpManagement> httpConfigContext) {
        final String contextName = "/keycloak/adapter/" + factoryName + "/";
        return new Service<Void>() {
            public void start(StartContext startContext) throws StartException {
                ExtensibleHttpManagement extensibleHttpManagement = (ExtensibleHttpManagement)httpConfigContext.getValue();
                extensibleHttpManagement.addStaticContext(contextName, new ResourceManager() {
                    public Resource getResource(final String path) throws IOException {
                        KeycloakAdapterConfigService adapterConfigService = KeycloakAdapterConfigService.getInstance();
                        final String config = adapterConfigService.getJSON(factoryName);

                        if (config == null) {
                            return null;
                        }

                        return new Resource() {
                            public String getPath() {
                                return null;
                            }

                            public Date getLastModified() {
                                return null;
                            }

                            public String getLastModifiedString() {
                                return null;
                            }

                            public ETag getETag() {
                                return null;
                            }

                            public String getName() {
                                return null;
                            }

                            public boolean isDirectory() {
                                return false;
                            }

                            public List<Resource> list() {
                                return Collections.emptyList();
                            }

                            public String getContentType(MimeMappings mimeMappings) {
                                return "application/json";
                            }

                            public void serve(Sender sender, HttpServerExchange exchange, IoCallback completionCallback) {
                                sender.send(config);
                            }

                            public Long getContentLength() {
                                return Long.valueOf((long)config.length());
                            }

                            public String getCacheKey() {
                                return null;
                            }

                            public File getFile() {
                                return null;
                            }

                            public Path getFilePath() {
                                return null;
                            }

                            public File getResourceManagerRoot() {
                                return null;
                            }

                            public Path getResourceManagerRootPath() {
                                return null;
                            }

                            public URL getUrl() {
                                return null;
                            }
                        };
                    }

                    public boolean isResourceChangeListenerSupported() {
                        return false;
                    }

                    public void registerResourceChangeListener(ResourceChangeListener listener) {
                    }

                    public void removeResourceChangeListener(ResourceChangeListener listener) {
                    }

                    public void close() throws IOException {
                    }
                });
            }

            public void stop(StopContext stopContext) {
                ((ExtensibleHttpManagement)httpConfigContext.getValue()).removeContext(contextName);
            }

            public Void getValue() throws IllegalStateException, IllegalArgumentException {
                return null;
            }
        };
    }
}
