/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.saml;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;

import static org.keycloak.saml.common.constants.GeneralConstants.RELAY_STATE;

/**
 * Minimal SAML client utility for test framework
 *
 */
public class SamlClient {

    /**
     * SAML bindings
     */
    public enum Binding {
        POST {
            @Override
            public HttpPost createSamlUnsignedRequest(URI samlEndpoint, String relayState, Document samlRequest) {
                return createSamlPostMessage(samlEndpoint, relayState, samlRequest, GeneralConstants.SAML_REQUEST_KEY);
            }

            private HttpPost createSamlPostMessage(URI samlEndpoint, String relayState, Document samlRequest, String messageType) {
                HttpPost post = new HttpPost(samlEndpoint);
                List<NameValuePair> parameters = new LinkedList<>();

                try {
                    BaseSAML2BindingBuilder binding = new BaseSAML2BindingBuilder();
                    parameters.add(
                            new BasicNameValuePair(messageType,
                                    binding
                                            .postBinding(samlRequest)
                                            .encoded())
                    );
                } catch (IOException | ConfigurationException | ProcessingException ex) {
                    throw new RuntimeException(ex);
                }

                if (relayState != null) {
                    parameters.add(new BasicNameValuePair(RELAY_STATE, relayState));
                }

                UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
                post.setEntity(formEntity);

                return post;
            }

            @Override
            public URI getBindingUri() {
                return JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.getUri();
            }
        };

        public abstract HttpPost createSamlUnsignedRequest(URI samlEndpoint, String relayState, Document samlRequest);
        public abstract URI getBindingUri();
    }

    /**
     * Creates a SAML login request document
     */
    public static AuthnRequestType createLoginRequestDocument(String issuer, String assertionConsumerURL, URI destinationURI) {
        try {
            SAML2Request samlReq = new SAML2Request();
            AuthnRequestType loginReq = samlReq.createAuthnRequestType(
                    UUID.randomUUID().toString(),
                    assertionConsumerURL,
                    destinationURI.toString(),
                    issuer
            );
            return loginReq;
        } catch (ConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
