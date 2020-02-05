/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.subsystem.saml.as7;

import java.util.HashMap;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelType;

/**
 * This class contains the definitions for the {@code HttpClient} attributes, as specified in the schema's {@code http-client-type}
 * complex type.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
abstract class HttpClientDefinition {

    private static final SimpleAttributeDefinition ALLOW_ANY_HOSTNAME =
            new SimpleAttributeDefinitionBuilder(Constants.Model.ALLOW_ANY_HOSTNAME, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.XML.ALLOW_ANY_HOSTNAME)
                    .setAllowExpression(true)
                    .build();

    private static final SimpleAttributeDefinition CLIENT_KEYSTORE =
            new SimpleAttributeDefinitionBuilder(Constants.Model.CLIENT_KEYSTORE, ModelType.STRING, true)
                    .setXmlName(Constants.XML.CLIENT_KEYSTORE)
                    .setAllowExpression(true)
                    .build();

    private static final SimpleAttributeDefinition CLIENT_KEYSTORE_PASSWORD =
            new SimpleAttributeDefinitionBuilder(Constants.Model.CLIENT_KEYSTORE_PASSWORD, ModelType.STRING, true)
                    .setXmlName(Constants.XML.CLIENT_KEYSTORE_PASSWORD)
                    .setAllowExpression(true)
                    .build();

    private static final SimpleAttributeDefinition CONNECTION_POOL_SIZE =
            new SimpleAttributeDefinitionBuilder(Constants.Model.CONNECTION_POOL_SIZE, ModelType.INT, true)
                    .setXmlName(Constants.XML.CONNECTION_POOL_SIZE)
                    .setAllowExpression(true)
                    .build();

    private static final SimpleAttributeDefinition DISABLE_TRUST_MANAGER =
            new SimpleAttributeDefinitionBuilder(Constants.Model.DISABLE_TRUST_MANAGER, ModelType.BOOLEAN, true)
                    .setXmlName(Constants.XML.DISABLE_TRUST_MANAGER)
                    .setAllowExpression(true)
                    .build();

    private static final SimpleAttributeDefinition PROXY_URL =
            new SimpleAttributeDefinitionBuilder(Constants.Model.PROXY_URL, ModelType.STRING, true)
                    .setXmlName(Constants.XML.PROXY_URL)
                    .setAllowExpression(true)
                    .build();

    private static final SimpleAttributeDefinition TRUSTSTORE =
            new SimpleAttributeDefinitionBuilder(Constants.Model.TRUSTSTORE, ModelType.STRING, true)
                    .setXmlName(Constants.XML.TRUSTSTORE)
                    .setAllowExpression(true)
                    .build();

    private static final SimpleAttributeDefinition TRUSTSTORE_PASSWORD =
            new SimpleAttributeDefinitionBuilder(Constants.Model.TRUSTSTORE_PASSWORD, ModelType.STRING, true)
                    .setXmlName(Constants.XML.TRUSTSTORE_PASSWORD)
                    .setAllowExpression(true)
                    .build();

    static final SimpleAttributeDefinition[] ATTRIBUTES = {ALLOW_ANY_HOSTNAME, CLIENT_KEYSTORE, CLIENT_KEYSTORE_PASSWORD,
            CONNECTION_POOL_SIZE, DISABLE_TRUST_MANAGER, PROXY_URL, TRUSTSTORE, TRUSTSTORE_PASSWORD};

    private static final HashMap<String, SimpleAttributeDefinition> ATTRIBUTE_MAP = new HashMap<>();

    static {
        for (SimpleAttributeDefinition def : ATTRIBUTES) {
            ATTRIBUTE_MAP.put(def.getXmlName(), def);
        }
    }

    static SimpleAttributeDefinition lookup(String xmlName) {
        return ATTRIBUTE_MAP.get(xmlName);
    }
}
