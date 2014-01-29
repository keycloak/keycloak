/*
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.subsystem.extension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Defines attributes and operations for the Realm
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
public class RealmDefinition extends SimpleResourceDefinition {

    public static final String TAG_NAME = "realm";

    protected static final SimpleAttributeDefinition REALM_PUBLIC_KEY =
            new SimpleAttributeDefinitionBuilder("realm-public-key", ModelType.STRING, false)
            .setXmlName("realm-public-key")
            .setAllowExpression(true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true))
            .build();
    protected static final SimpleAttributeDefinition AUTH_SERVER_URL =
            new SimpleAttributeDefinitionBuilder("auth-server-url", ModelType.STRING, false)
            .setXmlName("auth-server-url")
            .setAllowExpression(true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true))
            .build();
    protected static final SimpleAttributeDefinition SSL_NOT_REQUIRED =
            new SimpleAttributeDefinitionBuilder("ssl-not-required", ModelType.BOOLEAN, true)
            .setXmlName("ssl-not-required")
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .build();
    protected static final SimpleAttributeDefinition ALLOW_ANY_HOSTNAME =
            new SimpleAttributeDefinitionBuilder("allow-any-hostname", ModelType.BOOLEAN, true)
            .setXmlName("allow-any-hostname")
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .build();
    protected static final SimpleAttributeDefinition DISABLE_TRUST_MANAGER =
            new SimpleAttributeDefinitionBuilder("disable-trust-manager", ModelType.BOOLEAN, true)
            .setXmlName("disable-trust-manager")
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .build();
    protected static final SimpleAttributeDefinition TRUSTSTORE =
            new SimpleAttributeDefinitionBuilder("truststore", ModelType.STRING, true)
            .setXmlName("truststore")
            .setAllowExpression(true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
            .build();
    protected static final SimpleAttributeDefinition TRUSTSTORE_PASSWORD =
            new SimpleAttributeDefinitionBuilder("truststore-password", ModelType.STRING, true)
            .setXmlName("truststore-password")
            .setAllowExpression(true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
            .build();
    protected static final SimpleAttributeDefinition CONNECTION_POOL_SIZE =
            new SimpleAttributeDefinitionBuilder("connection-pool-size", ModelType.INT, true)
            .setXmlName("connection-pool-size")
            .setAllowExpression(true)
            .setValidator(new IntRangeValidator(0))
            .build();

    protected static final List<SimpleAttributeDefinition> REALM_ONLY_ATTRIBUTES = new ArrayList<SimpleAttributeDefinition>();
    static {
        REALM_ONLY_ATTRIBUTES.add(REALM_PUBLIC_KEY);
        REALM_ONLY_ATTRIBUTES.add(AUTH_SERVER_URL);
        REALM_ONLY_ATTRIBUTES.add(TRUSTSTORE);
        REALM_ONLY_ATTRIBUTES.add(TRUSTSTORE_PASSWORD);
        REALM_ONLY_ATTRIBUTES.add(SSL_NOT_REQUIRED);
        REALM_ONLY_ATTRIBUTES.add(ALLOW_ANY_HOSTNAME);
        REALM_ONLY_ATTRIBUTES.add(DISABLE_TRUST_MANAGER);
        REALM_ONLY_ATTRIBUTES.add(CONNECTION_POOL_SIZE);
    }

    protected static final List<SimpleAttributeDefinition> ALL_ATTRIBUTES = new ArrayList<SimpleAttributeDefinition>();
    static {
        ALL_ATTRIBUTES.addAll(REALM_ONLY_ATTRIBUTES);
        ALL_ATTRIBUTES.addAll(SharedAttributeDefinitons.ATTRIBUTES);
    }

    private static final Map<String, SimpleAttributeDefinition> DEFINITION_LOOKUP = new HashMap<String, SimpleAttributeDefinition>();
    static {
        for (SimpleAttributeDefinition def : ALL_ATTRIBUTES) {
            DEFINITION_LOOKUP.put(def.getXmlName(), def);
        }
    }

    private static final RealmWriteAttributeHandler realmAttrHandler = new RealmWriteAttributeHandler(ALL_ATTRIBUTES.toArray(new SimpleAttributeDefinition[0]));

    public RealmDefinition() {
        super(PathElement.pathElement("realm"),
                KeycloakExtension.getResourceDescriptionResolver("realm"),
                RealmAddHandler.INSTANCE,
                RealmRemoveHandler.INSTANCE);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        for (AttributeDefinition attrDef : ALL_ATTRIBUTES) {
            //TODO: use subclass of realmAttrHandler that can call RealmDefinition.validateTruststoreSetIfRequired
            resourceRegistration.registerReadWriteAttribute(attrDef, null, realmAttrHandler);
        }
    }

    /**
     * truststore and truststore-password must be set if ssl-not-required and disable-trust-manager are both false.
     *
     * @param attributes The full set of attributes.
     *
     * @return <code>true</code> if the attributes are valid, <code>false</code> otherwise.
     */
    public static boolean validateTruststoreSetIfRequired(ModelNode attributes) {
        if (!isSet(attributes, SSL_NOT_REQUIRED) && !isSet(attributes, DISABLE_TRUST_MANAGER)) {
            if (!(isSet(attributes, TRUSTSTORE) && isSet(attributes, TRUSTSTORE_PASSWORD))) {
                return false;
            }
        }

        return true;
    }

    private static boolean isSet(ModelNode attributes, SimpleAttributeDefinition def) {
        ModelNode attribute = attributes.get(def.getName());

        if (def.getType() == ModelType.BOOLEAN) {
            return attribute.isDefined() && attribute.asBoolean();
        }

        return attribute.isDefined() && !attribute.asString().isEmpty();
    }

    public static SimpleAttributeDefinition lookup(String name) {
        return DEFINITION_LOOKUP.get(name);
    }
}
