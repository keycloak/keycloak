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

package org.keycloak.client.registration.cli;

import java.io.IOException;
import java.util.List;

import org.keycloak.client.cli.common.AttributeOperation;
import org.keycloak.client.cli.util.AttributeException;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import static org.keycloak.client.cli.util.IoUtil.readFileOrStdin;
import static org.keycloak.client.registration.cli.ReflectionUtil.setAttributes;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class CmdStdinContext {

    private EndpointType regType;
    private ClientRepresentation client;
    private OIDCClientRepresentation oidcClient;
    private String content;
    public static final String CLIENT_OPTION_WARN = "You're using what looks like an OPTION as CLIENT: %s";
    public static final String TOKEN_OPTION_WARN = "You're using what looks like an OPTION as TOKEN: %s";

    public CmdStdinContext() {}

    public EndpointType getEndpointType() {
        return regType;
    }

    public void setEndpointType(EndpointType regType) {
        this.regType = regType;
    }

    public ClientRepresentation getClient() {
        return client;
    }

    public void setClient(ClientRepresentation client) {
        this.client = client;
    }

    public OIDCClientRepresentation getOidcClient() {
        return oidcClient;
    }

    public void setOidcClient(OIDCClientRepresentation oidcClient) {
        this.oidcClient = oidcClient;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRegistrationAccessToken() {
        if (client != null) {
            return client.getRegistrationAccessToken();
        } else if (oidcClient != null) {
            return oidcClient.getRegistrationAccessToken();
        }
        return null;
    }

    public static CmdStdinContext parseFileOrStdin(String file, EndpointType type) {
    
        String content = readFileOrStdin(file).trim();
        ClientRepresentation client = null;
        OIDCClientRepresentation oidcClient = null;
    
        if (type == null) {
            // guess the correct endpoint from content of the file
            if (content.startsWith("<")) {
                // looks like XML
                type = EndpointType.SAML2;
            } else if (content.startsWith("{")) {
                // looks like JSON?
                // try parse as ClientRepresentation
                try {
                    client = JsonSerialization.readValue(content, ClientRepresentation.class);
                    type = EndpointType.DEFAULT;
    
                } catch (JsonParseException e) {
                    throw new RuntimeException("Failed to read the input document as JSON: " + e.getMessage(), e);
                } catch (Exception ignored) {
                    // deliberately not logged
                }
    
                if (client == null) {
                    // try parse as OIDCClientRepresentation
                    try {
                        oidcClient = JsonSerialization.readValue(content, OIDCClientRepresentation.class);
                        type = EndpointType.OIDC;
                    } catch (IOException ne) {
                        throw new RuntimeException("Unable to determine input document type. Use -e TYPE to specify the registration endpoint to use");
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to read the input document as JSON", e);
                    }
                }
    
            } else if (content.length() == 0) {
                throw new RuntimeException("Document provided by --file option is empty");
            } else {
                throw new RuntimeException("Unable to determine input document type. Use -e TYPE to specify the registration endpoint to use");
            }
        }
    
        // check content type, making sure it can be parsed into .json if it's not saml xml
        if (content != null) {
            try {
                if (type == EndpointType.DEFAULT && client == null) {
                    client = JsonSerialization.readValue(content, ClientRepresentation.class);
                } else if (type == EndpointType.OIDC && oidcClient == null) {
                    oidcClient = JsonSerialization.readValue(content, OIDCClientRepresentation.class);
                }
            } catch (JsonParseException e) {
                throw new RuntimeException("Not a valid JSON document - " + e.getMessage(), e);
            } catch (UnrecognizedPropertyException e) {
                throw new RuntimeException("Attribute '" + e.getPropertyName() + "' not supported on document type '" + type.getName() + "'", e);
            } catch (IOException e) {
                throw new RuntimeException("Not a valid JSON document", e);
            }
        }
    
        CmdStdinContext ctx = new CmdStdinContext();
        ctx.setEndpointType(type);
        ctx.setContent(content);
        ctx.setClient(client);
        ctx.setOidcClient(oidcClient);
        return ctx;
    }

    public static CmdStdinContext mergeAttributes(CmdStdinContext ctx, List<AttributeOperation> attrs) {
        String content = ctx.getContent();
        ClientRepresentation client = ctx.getClient();
        OIDCClientRepresentation oidcClient = ctx.getOidcClient();
        EndpointType type = ctx.getEndpointType();
        try {
            if (content == null) {
                if (type == EndpointType.DEFAULT) {
                    client = new ClientRepresentation();
                } else if (type == EndpointType.OIDC) {
                    oidcClient = new OIDCClientRepresentation();
                }
            }
            Object rep = client != null ? client : oidcClient;
            if (rep != null) {
                try {
                    setAttributes(rep, attrs);
                } catch (AttributeException e) {
                    throw new RuntimeException("Failed to set attribute '" + e.getAttributeName() + "' on document type '" + type.getName() + "'", e);
                }
                content = JsonSerialization.writeValueAsString(rep);
            } else {
                throw new RuntimeException("Setting attributes is not supported for type: " + type.getName());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to merge set attributes with configuration from file", e);
        }
    
        ctx.setContent(content);
        ctx.setClient(client);
        ctx.setOidcClient(oidcClient);
        return ctx;
    }
}
