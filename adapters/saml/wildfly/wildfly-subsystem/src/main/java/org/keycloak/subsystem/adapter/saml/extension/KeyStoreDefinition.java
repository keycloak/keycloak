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

import java.util.HashMap;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
abstract class KeyStoreDefinition {

    static final SimpleAttributeDefinition RESOURCE =
            new SimpleAttributeDefinitionBuilder(Constants.Model.RESOURCE, ModelType.STRING, true)
                    .setXmlName(Constants.XML.RESOURCE)
                    .build();

    static final SimpleAttributeDefinition PASSWORD =
            new SimpleAttributeDefinitionBuilder(Constants.Model.PASSWORD, ModelType.STRING, true)
                    .setXmlName(Constants.XML.PASSWORD)
                    .build();

    static final SimpleAttributeDefinition FILE =
            new SimpleAttributeDefinitionBuilder(Constants.Model.FILE, ModelType.STRING, true)
                    .setXmlName(Constants.XML.FILE)
                    .build();

    static final SimpleAttributeDefinition TYPE =
            new SimpleAttributeDefinitionBuilder(Constants.Model.TYPE, ModelType.STRING, true)
                    .setXmlName(Constants.XML.TYPE)
                    .build();

    static final SimpleAttributeDefinition ALIAS =
            new SimpleAttributeDefinitionBuilder(Constants.Model.ALIAS, ModelType.STRING, true)
                    .setXmlName(Constants.XML.ALIAS)
                    .build();

    static final SimpleAttributeDefinition[] ATTRIBUTES = {RESOURCE, PASSWORD, FILE, TYPE, ALIAS};
    static final SimpleAttributeDefinition[] ALL_ATTRIBUTES = {RESOURCE, PASSWORD, FILE, TYPE, ALIAS,
            KeyStorePrivateKeyDefinition.PRIVATE_KEY_ALIAS,
            KeyStorePrivateKeyDefinition.PRIVATE_KEY_PASSWORD,
            KeyStoreCertificateDefinition.CERTIFICATE_ALIAS
    };

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
