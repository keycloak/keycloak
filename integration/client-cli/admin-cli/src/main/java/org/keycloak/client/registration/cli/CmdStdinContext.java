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
        if (oidcClient != null) {
            return oidcClient.getRegistrationAccessToken();
        }
        return null;
    }

    public static CmdStdinContext parseFileOrStdin(String file, EndpointType type) {

        String content = readFileOrStdin(file).trim();
        OIDCClientRepresentation oidcClient = null;

        if (content.length() == 0) {
            throw new RuntimeException("Document provided by --file option is empty");
        }

        if (!content.startsWith("{")) {
            throw new RuntimeException(
                    "Expected OIDC JSON input. Keycloak JSON (default) and SAML metadata are no longer supported by kcreg; "
                            + "use OIDC field names or the Admin REST API / kcadm instead.");
        }

        if (type == null) {
            type = EndpointType.OIDC;
        } else if (type != EndpointType.OIDC) {
            throw new RuntimeException("Only the 'oidc' endpoint type is supported");
        }

        try {
            oidcClient = JsonSerialization.readValue(content, OIDCClientRepresentation.class);
        } catch (JsonParseException e) {
            throw new RuntimeException("Failed to read the input document as JSON: " + e.getMessage(), e);
        } catch (UnrecognizedPropertyException e) {
            throw new RuntimeException("Attribute '" + e.getPropertyName()
                    + "' not supported on document type 'oidc'. Keycloak JSON (default) format is no longer supported by kcreg; "
                    + "use OIDC field names or the Admin REST API / kcadm.", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read the input document as JSON", e);
        }

        CmdStdinContext ctx = new CmdStdinContext();
        ctx.setEndpointType(type);
        ctx.setContent(content);
        ctx.setOidcClient(oidcClient);
        return ctx;
    }

    public static CmdStdinContext mergeAttributes(CmdStdinContext ctx, List<AttributeOperation> attrs) {
        String content = ctx.getContent();
        OIDCClientRepresentation oidcClient = ctx.getOidcClient();
        EndpointType type = ctx.getEndpointType();
        try {
            if (content == null) {
                oidcClient = new OIDCClientRepresentation();
            }
            if (oidcClient != null) {
                try {
                    setAttributes(oidcClient, attrs);
                } catch (AttributeException e) {
                    throw new RuntimeException("Failed to set attribute '" + e.getAttributeName() + "' on document type '" + type.getName() + "'", e);
                }
                content = JsonSerialization.writeValueAsString(oidcClient);
            } else {
                throw new RuntimeException("Setting attributes is not supported for type: " + type.getName());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to merge set attributes with configuration from file", e);
        }

        ctx.setContent(content);
        ctx.setOidcClient(oidcClient);
        return ctx;
    }
}
