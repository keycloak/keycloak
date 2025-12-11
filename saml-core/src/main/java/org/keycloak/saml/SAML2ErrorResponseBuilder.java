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

package org.keycloak.saml;

import java.util.LinkedList;
import java.util.List;

import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.ExtensionsType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusType;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.response.SAML2Response;
import org.keycloak.saml.processing.core.saml.v2.common.IDGenerator;
import org.keycloak.saml.processing.core.saml.v2.factories.JBossSAMLAuthnResponseFactory;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;

import org.w3c.dom.Document;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SAML2ErrorResponseBuilder implements SamlProtocolExtensionsAwareBuilder<SAML2ErrorResponseBuilder> {

    protected String status;
    protected String statusMessage;
    protected String destination;
    protected NameIDType issuer;
    protected final List<NodeGenerator> extensions = new LinkedList<>();

    public SAML2ErrorResponseBuilder status(String status) {
        this.status = status;
        return this;
    }

    public SAML2ErrorResponseBuilder statusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
        return this;
    }

    public SAML2ErrorResponseBuilder destination(String destination) {
        this.destination = destination;
        return this;
    }

    public SAML2ErrorResponseBuilder issuer(NameIDType issuer) {
        this.issuer = issuer;
        return this;
    }

    public SAML2ErrorResponseBuilder issuer(String issuer) {
        return issuer(SAML2NameIDBuilder.value(issuer).build());
    }

    @Override
    public SAML2ErrorResponseBuilder addExtension(NodeGenerator extension) {
        this.extensions.add(extension);
        return this;
    }

    public Document buildDocument() throws ProcessingException {

        try {
            StatusResponseType statusResponse = new ResponseType(IDGenerator.create("ID_"), XMLTimeUtil.getIssueInstant());

            StatusType statusType = JBossSAMLAuthnResponseFactory.createStatusTypeForResponder(status);
            statusType.setStatusMessage(statusMessage);
            statusResponse.setStatus(statusType);
            statusResponse.setIssuer(issuer);
            statusResponse.setDestination(destination);

            if (! this.extensions.isEmpty()) {
                ExtensionsType extensionsType = new ExtensionsType();
                for (NodeGenerator extension : this.extensions) {
                    extensionsType.addExtension(extension);
                }
                statusResponse.setExtensions(extensionsType);
            }

            SAML2Response saml2Response = new SAML2Response();
            return saml2Response.convert(statusResponse);
        } catch (ConfigurationException e) {
            throw new ProcessingException(e);
        } catch (ParsingException e) {
            throw new ProcessingException(e);
        }

    }


}
