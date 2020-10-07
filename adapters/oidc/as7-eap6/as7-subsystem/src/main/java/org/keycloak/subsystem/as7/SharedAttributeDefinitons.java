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

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines attributes that can be present in both a realm and an application (secure-deployment).
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
class SharedAttributeDefinitons {

    protected static final SimpleAttributeDefinition REALM_PUBLIC_KEY =
            new SimpleAttributeDefinitionBuilder("realm-public-key", ModelType.STRING, true)
                    .setXmlName("realm-public-key")
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();
    protected static final SimpleAttributeDefinition AUTH_SERVER_URL =
            new SimpleAttributeDefinitionBuilder("auth-server-url", ModelType.STRING, true)
                    .setXmlName("auth-server-url")
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();
    protected static final SimpleAttributeDefinition SSL_REQUIRED =
            new SimpleAttributeDefinitionBuilder("ssl-required", ModelType.STRING, true)
                    .setXmlName("ssl-required")
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode("external"))
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
                    .setValidator(new IntRangeValidator(0, true))
                    .build();

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
            .setAllowExpression(true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
            .build();
    protected static final SimpleAttributeDefinition EXPOSE_TOKEN =
            new SimpleAttributeDefinitionBuilder("expose-token", ModelType.BOOLEAN, true)
                    .setXmlName("expose-token")
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();
    protected static final SimpleAttributeDefinition AUTH_SERVER_URL_FOR_BACKEND_REQUESTS =
            new SimpleAttributeDefinitionBuilder("auth-server-url-for-backend-requests", ModelType.STRING, true)
                    .setXmlName("auth-server-url-for-backend-requests")
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();
    protected static final SimpleAttributeDefinition ALWAYS_REFRESH_TOKEN =
            new SimpleAttributeDefinitionBuilder("always-refresh-token", ModelType.BOOLEAN, true)
                    .setXmlName("always-refresh-token")
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();
    protected static final SimpleAttributeDefinition REGISTER_NODE_AT_STARTUP =
            new SimpleAttributeDefinitionBuilder("register-node-at-startup", ModelType.BOOLEAN, true)
                    .setXmlName("register-node-at-startup")
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();
    protected static final SimpleAttributeDefinition REGISTER_NODE_PERIOD =
            new SimpleAttributeDefinitionBuilder("register-node-period", ModelType.INT, true)
                    .setXmlName("register-node-period")
                    .setAllowExpression(true)
                    .setValidator(new IntRangeValidator(-1, true))
                    .build();
    protected static final SimpleAttributeDefinition TOKEN_STORE =
            new SimpleAttributeDefinitionBuilder("token-store", ModelType.STRING, true)
                    .setXmlName("token-store")
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();
    protected static final SimpleAttributeDefinition PRINCIPAL_ATTRIBUTE =
            new SimpleAttributeDefinitionBuilder("principal-attribute", ModelType.STRING, true)
                    .setXmlName("principal-attribute")
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();

    protected static final SimpleAttributeDefinition PROXY_URL =
            new SimpleAttributeDefinitionBuilder("proxy-url", ModelType.STRING, true)
                    .setXmlName("proxy-url")
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();

    protected static final SimpleAttributeDefinition VERIFY_TOKEN_AUDIENCE =
            new SimpleAttributeDefinitionBuilder("verify-token-audience", ModelType.BOOLEAN, true)
                    .setXmlName("verify-token-audience")
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();



    protected static final List<SimpleAttributeDefinition> ATTRIBUTES = new ArrayList<>();
    static {
        ATTRIBUTES.add(REALM_PUBLIC_KEY);
        ATTRIBUTES.add(AUTH_SERVER_URL);
        ATTRIBUTES.add(TRUSTSTORE);
        ATTRIBUTES.add(TRUSTSTORE_PASSWORD);
        ATTRIBUTES.add(SSL_REQUIRED);
        ATTRIBUTES.add(ALLOW_ANY_HOSTNAME);
        ATTRIBUTES.add(DISABLE_TRUST_MANAGER);
        ATTRIBUTES.add(CONNECTION_POOL_SIZE);
        ATTRIBUTES.add(ENABLE_CORS);
        ATTRIBUTES.add(CLIENT_KEYSTORE);
        ATTRIBUTES.add(CLIENT_KEYSTORE_PASSWORD);
        ATTRIBUTES.add(CLIENT_KEY_PASSWORD);
        ATTRIBUTES.add(CORS_MAX_AGE);
        ATTRIBUTES.add(CORS_ALLOWED_HEADERS);
        ATTRIBUTES.add(CORS_ALLOWED_METHODS);
        ATTRIBUTES.add(EXPOSE_TOKEN);
        ATTRIBUTES.add(AUTH_SERVER_URL_FOR_BACKEND_REQUESTS);
        ATTRIBUTES.add(ALWAYS_REFRESH_TOKEN);
        ATTRIBUTES.add(REGISTER_NODE_AT_STARTUP);
        ATTRIBUTES.add(REGISTER_NODE_PERIOD);
        ATTRIBUTES.add(TOKEN_STORE);
        ATTRIBUTES.add(PRINCIPAL_ATTRIBUTE);
        ATTRIBUTES.add(PROXY_URL);
        ATTRIBUTES.add(VERIFY_TOKEN_AUDIENCE);
    }

    /**
     * truststore and truststore-password must be set if ssl-required is not none and disable-trust-manager is false.
     *
     * @param attributes The full set of attributes.
     *
     * @return <code>true</code> if the attributes are valid, <code>false</code> otherwise.
     */
    public static boolean validateTruststoreSetIfRequired(ModelNode attributes) {
        if (isSet(attributes, DISABLE_TRUST_MANAGER)) {
            return true;
        }

        if (isSet(attributes, SSL_REQUIRED) && attributes.get(SSL_REQUIRED.getName()).asString().equals("none")) {
            return true;
        }
        //TODO, look into alternatives & requires properties on AttributeDefinition
        return isSet(attributes, TRUSTSTORE) && isSet(attributes, TRUSTSTORE_PASSWORD);
    }

    private static boolean isSet(ModelNode attributes, SimpleAttributeDefinition def) {
        ModelNode attribute = attributes.get(def.getName());

        if (def.getType() == ModelType.BOOLEAN) {
            return attribute.isDefined() && attribute.asBoolean();
        }

        return attribute.isDefined() && !attribute.asString().isEmpty();
    }


}
