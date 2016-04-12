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

package org.keycloak.protocol.saml.profile.ecp.util;

import org.keycloak.saml.processing.core.saml.v2.util.DocumentUtil;
import org.keycloak.saml.processing.web.util.PostBindingUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public final class Soap {

    public static SoapFaultBuilder createFault() {
        return new SoapFaultBuilder();
    }

    public static SoapMessageBuilder createMessage() {
        return new SoapMessageBuilder();
    }

    /**
     * <p>Returns a string encoded accordingly with the SAML HTTP POST Binding specification based on the
     * given <code>inputStream</code> which must contain a valid SOAP message.
     *
     * <p>The resulting string is based on the Body of the SOAP message, which should map to a valid SAML message.
     *
     * @param inputStream the input stream containing a valid SOAP message with a Body that contains a SAML message
     *
     * @return a string encoded accordingly with the SAML HTTP POST Binding specification
     */
    public static String toSamlHttpPostMessage(InputStream inputStream) {
        try {
            MessageFactory messageFactory = MessageFactory.newInstance();
            SOAPMessage soapMessage = messageFactory.createMessage(null, inputStream);
            SOAPBody soapBody = soapMessage.getSOAPBody();
            Node authnRequestNode = soapBody.getFirstChild();
            Document document = DocumentUtil.createDocument();

            document.appendChild(document.importNode(authnRequestNode, true));

            return PostBindingUtil.base64Encode(DocumentUtil.asString(document));
        } catch (Exception e) {
            throw new RuntimeException("Error creating fault message.", e);
        }
    }

    public static class SoapMessageBuilder {
        private final SOAPMessage message;
        private final SOAPBody body;
        private final SOAPEnvelope envelope;

        private SoapMessageBuilder() {
            try {
                this.message = MessageFactory.newInstance().createMessage();
                this.envelope = message.getSOAPPart().getEnvelope();
                this.body = message.getSOAPBody();
            } catch (Exception e) {
                throw new RuntimeException("Error creating fault message.", e);
            }
        }

        public SoapMessageBuilder addToBody(Document document) {
            try {
                this.body.addDocument(document);
            } catch (SOAPException e) {
                throw new RuntimeException("Could not add document to SOAP body.", e);
            }
            return this;
        }

        public SoapMessageBuilder addNamespace(String prefix, String ns) {
            try {
                envelope.addNamespaceDeclaration(prefix, ns);
            } catch (SOAPException e) {
                throw new RuntimeException("Could not add namespace to SOAP Envelope.", e);
            }
            return this;
        }

        public SOAPHeaderElement addHeader(String name, String prefix) {
            try {
                return this.envelope.getHeader().addHeaderElement(envelope.createQName(name, prefix));
            } catch (SOAPException e) {
                throw new RuntimeException("Could not add SOAP Header.", e);
            }
        }

        public Name createName(String name) {
            try {
                return this.envelope.createName(name);
            } catch (SOAPException e) {
                throw new RuntimeException("Could not create Name.", e);
            }
        }

        public Response build() {
            return build(Status.OK);
        }

        Response build(Status status) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            try {
                this.message.writeTo(outputStream);
            } catch (Exception e) {
                throw new RuntimeException("Error while building SOAP Fault.", e);
            }

            return Response.status(status).entity(outputStream.toByteArray()).build();
        }

        SOAPMessage getMessage() {
            return this.message;
        }
    }

    public static class SoapFaultBuilder {

        private final SOAPFault fault;
        private final SoapMessageBuilder messageBuilder;

        private SoapFaultBuilder() {
            this.messageBuilder = createMessage();
            try {
                this.fault = messageBuilder.getMessage().getSOAPBody().addFault();
            } catch (SOAPException e) {
                throw new RuntimeException("Could not create SOAP Fault.", e);
            }
        }

        public SoapFaultBuilder detail(String detail) {
            try {
                this.fault.addDetail().setValue(detail);
            } catch (SOAPException e) {
                throw new RuntimeException("Error creating fault message.", e);
            }
            return this;
        }

        public SoapFaultBuilder reason(String reason) {
            try {
                this.fault.setFaultString(reason);
            } catch (SOAPException e) {
                throw new RuntimeException("Error creating fault message.", e);
            }
            return this;
        }

        public SoapFaultBuilder code(String code) {
            try {
                this.fault.setFaultCode(code);
            } catch (SOAPException e) {
                throw new RuntimeException("Error creating fault message.", e);
            }
            return this;
        }

        public Response build() {
            return this.messageBuilder.build(Status.INTERNAL_SERVER_ERROR);
        }
    }
}
