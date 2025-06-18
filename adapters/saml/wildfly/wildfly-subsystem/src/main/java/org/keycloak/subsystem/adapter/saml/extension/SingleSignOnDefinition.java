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
package org.keycloak.subsystem.adapter.saml.extension;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelType;

import java.util.HashMap;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
abstract class SingleSignOnDefinition {

    static final SimpleAttributeDefinition SIGN_REQUEST =
            new SimpleAttributeDefinitionBuilder(Constants.Model.SIGN_REQUEST, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.XML.SIGN_REQUEST)
                    .build();

    static final SimpleAttributeDefinition VALIDATE_RESPONSE_SIGNATURE =
            new SimpleAttributeDefinitionBuilder(Constants.Model.VALIDATE_RESPONSE_SIGNATURE, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.XML.VALIDATE_RESPONSE_SIGNATURE)
                    .build();

    static final SimpleAttributeDefinition VALIDATE_ASSERTION_SIGNATURE =
            new SimpleAttributeDefinitionBuilder(Constants.Model.VALIDATE_ASSERTION_SIGNATURE, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.XML.VALIDATE_ASSERTION_SIGNATURE)
                    .build();

    static final SimpleAttributeDefinition REQUEST_BINDING =
            new SimpleAttributeDefinitionBuilder(Constants.Model.REQUEST_BINDING, ModelType.STRING, true)
                    .setXmlName(Constants.XML.REQUEST_BINDING)
                    .build();

    static final SimpleAttributeDefinition RESPONSE_BINDING =
            new SimpleAttributeDefinitionBuilder(Constants.Model.RESPONSE_BINDING, ModelType.STRING, true)
                    .setXmlName(Constants.XML.RESPONSE_BINDING)
                    .build();

    static final SimpleAttributeDefinition BINDING_URL =
            new SimpleAttributeDefinitionBuilder(Constants.Model.BINDING_URL, ModelType.STRING, true)
                    .setXmlName(Constants.XML.BINDING_URL)
                    .build();

    static final SimpleAttributeDefinition ASSERTION_CONSUMER_SERVICE_URL =
            new SimpleAttributeDefinitionBuilder(Constants.Model.ASSERTION_CONSUMER_SERVICE_URL, ModelType.STRING, true)
                    .setXmlName(Constants.XML.ASSERTION_CONSUMER_SERVICE_URL)
                    .build();

    static final SimpleAttributeDefinition[] ATTRIBUTES = {SIGN_REQUEST, VALIDATE_RESPONSE_SIGNATURE, VALIDATE_ASSERTION_SIGNATURE, REQUEST_BINDING, RESPONSE_BINDING, BINDING_URL, ASSERTION_CONSUMER_SERVICE_URL};

    static final HashMap<String, SimpleAttributeDefinition> ATTRIBUTE_MAP = new HashMap<>();

    static {
        for (SimpleAttributeDefinition def : ATTRIBUTES) {
            ATTRIBUTE_MAP.put(def.getXmlName(), def);
        }
    }

    static SimpleAttributeDefinition lookup(String xmlName) {
        return ATTRIBUTE_MAP.get(xmlName);
    }
}
