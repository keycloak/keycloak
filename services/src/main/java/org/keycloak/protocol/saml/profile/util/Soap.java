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

package org.keycloak.protocol.saml.profile.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.MimeHeader;
import jakarta.xml.soap.MimeHeaders;
import jakarta.xml.soap.Name;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPConnection;
import jakarta.xml.soap.SOAPConnectionFactory;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.soap.SOAPHeaderElement;
import jakarta.xml.soap.SOAPMessage;

import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.saml.processing.core.saml.v2.util.DocumentUtil;
import org.keycloak.saml.processing.web.util.PostBindingUtil;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
     * @param document the document containing a valid SOAP message with a Body that contains a SAML message
     *
     * @return a string encoded accordingly with the SAML HTTP POST Binding specification
     */
    public static String toSamlHttpPostMessage(Document document) {
        try {
            return PostBindingUtil.base64Encode(DocumentUtil.asString(document));
        } catch (Exception e) {
            throw new RuntimeException("Error encoding SOAP document to String.", e);
        }
    }

    /**
     * <p>Returns Document based on the given <code>inputStream</code> which must contain a valid SOAP message.
     *
     * <p>The resulting string is based on the Body of the SOAP message, which should map to a valid SAML message.
     *
     * @param inputStream an InputStream consisting of a SOAPMessage
     * @return A document containing the body of the SOAP message
     */
    public static Document extractSoapMessage(InputStream inputStream) {
        try {
            MessageFactory messageFactory = MessageFactory.newInstance();
            SOAPMessage soapMessage = messageFactory.createMessage(null, inputStream);
            return extractSoapMessage(soapMessage);
        } catch (Exception e) {
            throw new RuntimeException("Error creating fault message.", e);
        }
    }

    /**
     * <p>Returns Document based on the given SOAP message.
     *
     * <p>The resulting string is based on the Body of the SOAP message, which should map to a valid SAML message.
     * @param soapMessage a SOAPMessage from which to extract the body
     * @return A document containing the body of the SOAP message
     */
    public static Document extractSoapMessage(SOAPMessage soapMessage) {
        try {
            SOAPBody soapBody = soapMessage.getSOAPBody();
            Node authnRequestNode = getFirstChild(soapBody);
            Document document = DocumentUtil.createDocument();
            document.appendChild(document.importNode(authnRequestNode, true));
            return document;
        } catch (Exception e) {
            throw new RuntimeException("Error creating fault message.", e);
        }
    }

    /**
     * Get the first direct child that is an XML element.
     * In case of pretty-printed XML (with newlines and spaces), this method skips non-element objects (e.g. text)
     * to really fetch the next XML tag.
     */
    public static Node getFirstChild(Node parent) {
        Node n = parent.getFirstChild();
        while (n != null && !(n instanceof Element)) {
            n = n.getNextSibling();
        }
        if (n == null) return null;
        return n;
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

        public SoapMessageBuilder addMimeHeader(String name, String value) {
            this.message.getMimeHeaders().addHeader(name, value);
            return this;
        }

        public Name createName(String name) {
            try {
                return this.envelope.createName(name);
            } catch (SOAPException e) {
                throw new RuntimeException("Could not create Name.", e);
            }
        }

        public byte[] getBytes() {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            try {
                this.message.writeTo(outputStream);
            } catch (Exception e) {
                throw new RuntimeException("Error while building SOAP Fault.", e);
            }
            return outputStream.toByteArray();
        }

        public Response build() {
            return build(Status.OK);
        }

        /**
         * Standard build method, generates a javax ws rs Response
         * @param status the status of the response
         * @return a Response containing the SOAP message
         */
        Response build(Status status) {
            return Response.status(status).entity(getBytes()).type(MediaType.TEXT_XML_TYPE).build();
        }

        /**
         * Build method for testing, generates an apache httpcomponents HttpPost
         * @param uri the URI to which to POST the soap message
         * @return an HttpPost containing the SOAP message
         */
        public HttpPost buildHttpPost(URI uri) {
            HttpPost post = new HttpPost(uri);
            post.setEntity(new ByteArrayEntity(getBytes(), ContentType.TEXT_XML));
            return post;
        }

        /**
         * Performs a synchronous call, sending the current message to the given url
         * @param url a SOAP endpoint url
         * @return the SOAPMessage returned by the contacted SOAP server
         * @throws SOAPException Raised if there's a problem performing the SOAP call
         * @deprecated Use {@link #call(String,KeycloakSession)} to use SimpleHttp configuration
         */
        @Deprecated
        public SOAPMessage call(String url) throws SOAPException {
            SOAPMessage response;
            SOAPConnection soapConnection = null;
            try {
                SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
                soapConnection = soapConnectionFactory.createConnection();
                response = soapConnection.call(message, url);
            } finally {
                if (soapConnection != null) {
                    soapConnection.close();
                }
            }
            return response;
        }

        /**
         * Performs a synchronous call, sending the current message to the given url.
         * SimpleHttp is retrieved using the session parameter.
         * @param url The SOAP endpoint URL to connect
         * @param session The session to use to locate the SimpleHttp sender
         * @return the SOAPMessage returned by the contacted SOAP server
         * @throws SOAPException Raised if there's a problem performing the SOAP call
         */
        public SOAPMessage call(String url, KeycloakSession session) throws SOAPException {
            // https://github.com/eclipse-ee4j/metro-saaj/blob/master/saaj-ri/src/main/java/com/sun/xml/messaging/saaj/client/p2p/HttpSOAPConnection.java
            // save changes of the message, this adds content-type and content-length headers
            if (message.saveRequired()) {
                message.saveChanges();
            }
            // use SimpleHttp from the session
            SimpleHttpRequest simpleHttp = SimpleHttp.create(session).doPost(url);
            // add all the headers as HTTP headers except the ones needed for the HttpEntity
            Iterator<MimeHeader> reqHeaders = message.getMimeHeaders().getAllHeaders();
            ContentType contentType = null;
            int length = -1;
            boolean hasCacheControl = false;
            while (reqHeaders.hasNext()) {
                MimeHeader mimeHeader = reqHeaders.next();
                if (HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(mimeHeader.getName())) {
                    contentType = ContentType.parse(mimeHeader.getValue());
                } else if (HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(mimeHeader.getName())) {
                    length = Integer.parseInt(mimeHeader.getValue());
                } else {
                    if (HttpHeaders.CACHE_CONTROL.equalsIgnoreCase(mimeHeader.getName())) {
                        hasCacheControl = true;
                    }
                    String currentValue = simpleHttp.getHeader(mimeHeader.getName());
                    simpleHttp.header(mimeHeader.getName(), currentValue == null
                            ? mimeHeader.getValue() : currentValue + "," + mimeHeader.getValue());
                }
            }
            if (!hasCacheControl) {
                // set no cache if cache-control was not specified
                simpleHttp.header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store");
            }
            // create the message and send to the parameter URL
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                message.writeTo(out);
                simpleHttp.entity(new ByteArrayEntity(out.toByteArray(), 0, length, contentType));
                try (SimpleHttpResponse res = simpleHttp.asResponse()) {
                    // HTTP_INTERNAL_ERROR (500) and HTTP_BAD_REQUEST (400) should be processed as SOAP faults
                    if (res.getStatus() == HttpStatus.SC_INTERNAL_SERVER_ERROR
                            || res.getStatus() == HttpStatus.SC_BAD_REQUEST
                            || res.getStatus() == HttpStatus.SC_OK) {
                        MimeHeaders resHeaders = new MimeHeaders();
                        Header[] headers = res.getAllHeaders();
                        for (Header header : headers) {
                            resHeaders.addHeader(header.getName(), header.getValue());
                        }
                        String responseString = res.asString();
                        if (responseString == null || responseString.isEmpty()) {
                            // return null if no reply message
                            return null;
                        }
                        return MessageFactory.newInstance().createMessage(resHeaders, new ByteArrayInputStream(responseString.getBytes(res.getContentTypeCharset())));
                    } else {
                        throw new SOAPException("Bad response (" + res.getStatus() + ") :" + res.asString());
                    }
                }
            } catch (IOException e) {
                throw new SOAPException(e);
            }
        }

        public SOAPMessage getMessage() {
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
