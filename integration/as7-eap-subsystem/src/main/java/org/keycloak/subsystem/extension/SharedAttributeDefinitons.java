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
import java.util.List;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Defines attributes that can be present in both a realm and an application (secure-deployment).
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
public class SharedAttributeDefinitons {

    protected static final SimpleAttributeDefinition ENABLE_CORS =
            new SimpleAttributeDefinitionBuilder("enable-cors", ModelType.BOOLEAN, true)
            .setXmlName("enable-cors")
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .build();
    protected static final SimpleAttributeDefinition CLIENT_KEYSTORE =
            new SimpleAttributeDefinitionBuilder("client-keystore", ModelType.STRING, true)
            .setXmlName("client-keystore")
            .setAllowExpression(true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
            .build();
    protected static final SimpleAttributeDefinition CLIENT_KEYSTORE_PASSWORD =
            new SimpleAttributeDefinitionBuilder("client-keystore-password", ModelType.STRING, true)
            .setXmlName("client-keystore-password")
            .setAllowExpression(true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
            .build();
    protected static final SimpleAttributeDefinition CLIENT_KEY_PASSWORD =
            new SimpleAttributeDefinitionBuilder("client-key-password", ModelType.STRING, true)
            .setXmlName("client-key-password")
            .setAllowExpression(true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
            .build();
    protected static final SimpleAttributeDefinition CORS_MAX_AGE =
            new SimpleAttributeDefinitionBuilder("cors-max-age", ModelType.INT, true)
            .setXmlName("cors-max-age")
            .setAllowExpression(true)
            .setValidator(new IntRangeValidator(-1, true))
            .build();
    protected static final SimpleAttributeDefinition CORS_ALLOWED_HEADERS =
            new SimpleAttributeDefinitionBuilder("cors-allowed-headers", ModelType.STRING, true)
            .setXmlName("cors-allowed-headers")
            .setAllowExpression(true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
            .build();
    protected static final SimpleAttributeDefinition CORS_ALLOWED_METHODS =
            new SimpleAttributeDefinitionBuilder("cors-allowed-methods", ModelType.STRING, true)
            .setXmlName("cors-allowed-methods")
            .setAllowExpression(true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
            .build();
    protected static final SimpleAttributeDefinition EXPOSE_TOKEN =
            new SimpleAttributeDefinitionBuilder("expose-token", ModelType.BOOLEAN, true)
            .setXmlName("expose-token")
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .build();


    protected static final List<SimpleAttributeDefinition> ATTRIBUTES = new ArrayList<SimpleAttributeDefinition>();
    static {
        ATTRIBUTES.add(ENABLE_CORS);
        ATTRIBUTES.add(CLIENT_KEYSTORE);
        ATTRIBUTES.add(CLIENT_KEYSTORE_PASSWORD);
        ATTRIBUTES.add(CLIENT_KEY_PASSWORD);
        ATTRIBUTES.add(CORS_MAX_AGE);
        ATTRIBUTES.add(CORS_ALLOWED_HEADERS);
        ATTRIBUTES.add(CORS_ALLOWED_METHODS);
        ATTRIBUTES.add(EXPOSE_TOKEN);
    }

}
