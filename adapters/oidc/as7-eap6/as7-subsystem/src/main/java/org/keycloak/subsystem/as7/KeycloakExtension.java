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
package org.keycloak.subsystem.as7;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.keycloak.subsystem.as7.logging.KeycloakLogger;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;


/**
 * Main Extension class for the subsystem.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
public class KeycloakExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "keycloak";
    public static final String NAMESPACE_1_1 = "urn:jboss:domain:keycloak:1.1";
    public static final String NAMESPACE_1_2 = "urn:jboss:domain:keycloak:1.2";
    public static final String CURRENT_NAMESPACE = NAMESPACE_1_2;
    private static final KeycloakSubsystemParser PARSER = new KeycloakSubsystemParser();
    static final PathElement PATH_SUBSYSTEM = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    private static final String RESOURCE_NAME = KeycloakExtension.class.getPackage().getName() + ".LocalDescriptions";
    private static final int MGMT_API_VERSION_MAJOR = 1;
    private static final int MGMT_API_VERSION_MINOR = 1;

    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    private static final ResourceDefinition KEYCLOAK_SUBSYSTEM_RESOURCE = new KeycloakSubsystemDefinition();
    static final RealmDefinition REALM_DEFINITION = new RealmDefinition();
    static final SecureDeploymentDefinition SECURE_DEPLOYMENT_DEFINITION = new SecureDeploymentDefinition();
    static final CredentialDefinition CREDENTIAL_DEFINITION = new CredentialDefinition();

    public static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, KeycloakExtension.class.getClassLoader(), true, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, KeycloakExtension.NAMESPACE_1_1, PARSER);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, KeycloakExtension.NAMESPACE_1_2, PARSER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(final ExtensionContext context) {
        KeycloakLogger.ROOT_LOGGER.debug("Activating Keycloak Extension");
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MGMT_API_VERSION_MAJOR, MGMT_API_VERSION_MINOR);

        ManagementResourceRegistration registration = subsystem.registerSubsystemModel(KEYCLOAK_SUBSYSTEM_RESOURCE);
        registration.registerSubModel(REALM_DEFINITION);
        ManagementResourceRegistration secureDeploymentRegistration = registration.registerSubModel(SECURE_DEPLOYMENT_DEFINITION);
        secureDeploymentRegistration.registerSubModel(CREDENTIAL_DEFINITION);

        subsystem.registerXMLElementWriter(PARSER);
    }
}
