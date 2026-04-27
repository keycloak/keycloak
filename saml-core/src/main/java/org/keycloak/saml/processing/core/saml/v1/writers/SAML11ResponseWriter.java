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
package org.keycloak.saml.processing.core.saml.v1.writers;

import java.net.URI;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;

import org.keycloak.dom.saml.common.CommonStatusDetailType;
import org.keycloak.dom.saml.v1.assertion.SAML11AssertionType;
import org.keycloak.dom.saml.v1.protocol.SAML11ResponseType;
import org.keycloak.dom.saml.v1.protocol.SAML11StatusCodeType;
import org.keycloak.dom.saml.v1.protocol.SAML11StatusType;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.common.util.StringUtil;
import org.keycloak.saml.processing.core.saml.v1.SAML11Constants;

import org.w3c.dom.Element;

/**
 * Write the {@link SAML11ResponseType} to stream
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 29, 2011
 */
public class SAML11ResponseWriter extends BaseSAML11Writer {

    protected String namespace = SAML11Constants.PROTOCOL_11_NSURI;

    protected SAML11AssertionWriter assertionWriter;

    public SAML11ResponseWriter(XMLStreamWriter writer) {
        super(writer);
        assertionWriter = new SAML11AssertionWriter(writer);
    }

    public void write(SAML11ResponseType response) throws ProcessingException {
        StaxUtil.writeStartElement(writer, PROTOCOL_PREFIX, SAML11Constants.RESPONSE, namespace);
        StaxUtil.writeNameSpace(writer, PROTOCOL_PREFIX, namespace);
        StaxUtil.writeNameSpace(writer, ASSERTION_PREFIX, SAML11Constants.ASSERTION_11_NSURI);

        // Attributes
        StaxUtil.writeAttribute(writer, SAML11Constants.RESPONSE_ID, response.getID());
        StaxUtil.writeAttribute(writer, SAML11Constants.MAJOR_VERSION, response.getMajorVersion() + "");
        StaxUtil.writeAttribute(writer, SAML11Constants.MINOR_VERSION, response.getMinorVersion() + "");
        StaxUtil.writeAttribute(writer, JBossSAMLConstants.ISSUE_INSTANT.get(), response.getIssueInstant().toString());
        String inResp = response.getInResponseTo();
        if (StringUtil.isNotNull(inResp)) {
            StaxUtil.writeAttribute(writer, SAML11Constants.IN_RESPONSE_TO, inResp);
        }

        URI recipient = response.getRecipient();
        if (recipient != null) {
            StaxUtil.writeAttribute(writer, SAML11Constants.RECIPIENT, recipient.toString());
        }

        Element sig = response.getSignature();
        if (sig != null) {
            StaxUtil.writeDOMElement(writer, sig);
        }

        SAML11StatusType status = response.getStatus();
        if (status != null) {
            write(status);
        }

        List<SAML11AssertionType> assertions = response.get();
        for (SAML11AssertionType assertion : assertions) {
            assertionWriter.write(assertion);
        }

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    public void write(SAML11StatusType status) throws ProcessingException {
        StaxUtil.writeStartElement(writer, PROTOCOL_PREFIX, SAML11Constants.STATUS, namespace);

        SAML11StatusCodeType statusCode = status.getStatusCode();
        if (statusCode != null) {
            write(statusCode);
        }

        String statusMsg = status.getStatusMessage();
        if (StringUtil.isNotNull(statusMsg)) {
            StaxUtil.writeStartElement(writer, PROTOCOL_PREFIX, SAML11Constants.STATUS_MSG, namespace);
            StaxUtil.writeCharacters(writer, statusMsg);
            StaxUtil.writeEndElement(writer);
        }

        CommonStatusDetailType details = status.getStatusDetail();
        if (details != null) {
            StaxUtil.writeStartElement(writer, PROTOCOL_PREFIX, SAML11Constants.STATUS_DETAIL, namespace);
            List<Object> objs = details.getAny();
            for (Object theObj : objs) {
                StaxUtil.writeCharacters(writer, theObj.toString());
            }
            StaxUtil.writeEndElement(writer);
        }
        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    public void write(SAML11StatusCodeType statusCode) throws ProcessingException {
        StaxUtil.writeStartElement(writer, PROTOCOL_PREFIX, SAML11Constants.STATUS_CODE, namespace);

        QName value = statusCode.getValue();
        if (value == null)
            throw logger.writerNullValueError("Attribute Value");
        StaxUtil.writeAttribute(writer, SAML11Constants.VALUE, value);

        SAML11StatusCodeType secondCode = statusCode.getStatusCode();
        if (secondCode != null) {
            write(secondCode);
        }

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }
}