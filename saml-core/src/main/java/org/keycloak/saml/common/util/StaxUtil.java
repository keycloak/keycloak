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
package org.keycloak.saml.common.util;

import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Stack;

/**
 * Utility class that deals with StAX
 *
 * @author Anil.Saldhana@redhat.com
 * @since Oct 19, 2010
 */
public class StaxUtil {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    private static final ThreadLocal<Stack<String>> registeredNSStack = new ThreadLocal<Stack<String>>();

    /**
     * Flush the stream writer
     *
     * @param writer
     *
     * @throws org.keycloak.saml.common.exceptions.ProcessingException
     */
    public static void flush(XMLStreamWriter writer) throws ProcessingException {
        try {
            writer.flush();
        } catch (XMLStreamException e) {
            throw logger.processingError(e);
        }
    }

    /**
     * Get an {@code XMLEventWriter}
     *
     * @param outStream
     *
     * @return
     *
     * @throws ProcessingException
     */
    public static XMLEventWriter getXMLEventWriter(final OutputStream outStream) throws ProcessingException {
        XMLOutputFactory xmlOutputFactory = getXMLOutputFactory();
        try {
            return xmlOutputFactory.createXMLEventWriter(outStream, GeneralConstants.SAML_CHARSET_NAME);
        } catch (XMLStreamException e) {
            throw logger.processingError(e);
        }
    }

    /**
     * Get an {@code XMLStreamWriter}
     *
     * @param outStream
     *
     * @return
     *
     * @throws ProcessingException
     */
    public static XMLStreamWriter getXMLStreamWriter(final OutputStream outStream) throws ProcessingException {
        XMLOutputFactory xmlOutputFactory = getXMLOutputFactory();
        try {
            return xmlOutputFactory.createXMLStreamWriter(outStream, GeneralConstants.SAML_CHARSET_NAME);
        } catch (XMLStreamException e) {
            throw logger.processingError(e);
        }
    }

    /**
     * Get an {@code XMLStreamWriter}
     *
     * @param writer {@code Writer}
     *
     * @return
     *
     * @throws ProcessingException
     */
    public static XMLStreamWriter getXMLStreamWriter(final Writer writer) throws ProcessingException {
        XMLOutputFactory xmlOutputFactory = getXMLOutputFactory();
        try {
            return xmlOutputFactory.createXMLStreamWriter(writer);
        } catch (XMLStreamException e) {
            throw logger.processingError(e);
        }
    }

    public static XMLStreamWriter getXMLStreamWriter(final Result result) throws ProcessingException {
        XMLOutputFactory factory = getXMLOutputFactory();
        try {
            return factory.createXMLStreamWriter(result);
        } catch (XMLStreamException xe) {
            throw logger.processingError(xe);
        }
    }

    /**
     * Set a prefix
     *
     * @param writer
     * @param prefix
     * @param nsURI
     *
     * @throws ProcessingException
     */
    public static void setPrefix(XMLStreamWriter writer, String prefix, String nsURI) throws ProcessingException {
        try {
            writer.setPrefix(prefix, nsURI);
        } catch (XMLStreamException e) {
            throw logger.processingError(e);
        }
    }

    /**
     * Write an attribute
     *
     * @param writer
     * @param attributeName QName of the attribute
     * @param attributeValue
     *
     * @throws ProcessingException
     */
    public static void writeAttribute(XMLStreamWriter writer, String attributeName, QName attributeValue)
            throws ProcessingException {
        writeAttribute(writer, attributeName, attributeValue.toString());
    }

    /**
     * Write an attribute
     *
     * @param writer
     * @param attributeName QName of the attribute
     * @param attributeValue
     *
     * @throws ProcessingException
     */
    public static void writeAttribute(XMLStreamWriter writer, QName attributeName, String attributeValue)
            throws ProcessingException {
        try {
            writer.writeAttribute(attributeName.getPrefix(), attributeName.getNamespaceURI(), attributeName.getLocalPart(),
                    attributeValue);
        } catch (XMLStreamException e) {
            throw logger.processingError(e);
        }
    }

    /**
     * Write an xml attribute
     *
     * @param writer
     * @param localName localpart
     * @param value value of the attribute
     *
     * @throws ProcessingException
     */
    public static void writeAttribute(XMLStreamWriter writer, String localName, String value) throws ProcessingException {
        try {
            writer.writeAttribute(localName, value);
        } catch (XMLStreamException e) {
            throw logger.processingError(e);
        }
    }

    /**
     * Write an xml attribute
     *
     * @param writer
     * @param localName localpart
     * @param type typically xsi:type
     * @param value value of the attribute
     *
     * @throws ProcessingException
     */
    public static void writeAttribute(XMLStreamWriter writer, String localName, String type, String value)
            throws ProcessingException {
        try {
            writer.writeAttribute(localName, type, value);
        } catch (XMLStreamException e) {
            throw logger.processingError(e);
        }
    }

    /**
     * Write an xml attribute
     *
     * @param writer
     * @param prefix prefix for the attribute
     * @param localName localpart
     * @param type typically xsi:type
     * @param value value of the attribute
     *
     * @throws ProcessingException
     */
    public static void writeAttribute(XMLStreamWriter writer, String prefix, String localName, String type, String value)
            throws ProcessingException {
        try {
            writer.writeAttribute(prefix, localName, type, value);
        } catch (XMLStreamException e) {
            throw logger.processingError(e);
        }
    }

    /**
     * Write a string as text node
     *
     * @param writer
     * @param value
     *
     * @throws ProcessingException
     */
    public static void writeCharacters(XMLStreamWriter writer, String value) throws ProcessingException {
        try {
            writer.writeCharacters(value);
        } catch (XMLStreamException e) {
            throw logger.processingError(e);
        }
    }

    /**
     * Write a string as text node
     *
     * @param writer
     * @param value
     *
     * @throws ProcessingException
     */
    public static void writeCData(XMLStreamWriter writer, String value) throws ProcessingException {
        try {
            writer.writeCData(value);
        } catch (XMLStreamException e) {
            throw logger.processingError(e);
        }
    }

    /**
     * Write the default namespace
     *
     * @param writer
     * @param ns
     *
     * @throws ProcessingException
     */
    public static void writeDefaultNameSpace(XMLStreamWriter writer, String ns) throws ProcessingException {
        try {
            writer.writeDefaultNamespace(ns);
        } catch (XMLStreamException e) {
            throw logger.processingError(e);
        }
    }

    /**
     * Write a DOM Node to the stream
     *
     * @param writer
     * @param node
     *
     * @throws ProcessingException
     */
    public static void writeDOMNode(XMLStreamWriter writer, Node node) throws ProcessingException {
        try {
            short nodeType = node.getNodeType();

            switch (nodeType) {
                case Node.ELEMENT_NODE:
                    writeDOMElement(writer, (Element) node);
                    break;
                case Node.TEXT_NODE:
                    writer.writeCharacters(node.getNodeValue());
                    break;
                case Node.COMMENT_NODE:
                    writer.writeComment(node.getNodeValue());
                    break;
                case Node.CDATA_SECTION_NODE:
                    writer.writeCData(node.getNodeValue());
                    break;
                default:
                    // Don't care
            }
        } catch (DOMException e) {
            throw logger.processingError(e);
        } catch (XMLStreamException e) {
            throw logger.processingError(e);
        }
    }

    /**
     * Write DOM Element to the stream
     *
     * @param writer
     * @param domElement
     *
     * @throws ProcessingException
     */
    public static void writeDOMElement(XMLStreamWriter writer, Element domElement) throws ProcessingException {
        if (registeredNSStack.get() == null) {
            registeredNSStack.set(new Stack<String>());
        }
        String domElementPrefix = domElement.getPrefix();

        if (domElementPrefix == null) {
            domElementPrefix = "";
        }

        String domElementNS = domElement.getNamespaceURI();
        if (domElementNS == null) {
            domElementNS = "";
        }

        writeStartElement(writer, domElementPrefix, domElement.getLocalName(), domElementNS);

        // Should we register namespace
        if (! domElementPrefix.isEmpty() && !registeredNSStack.get().contains(domElementNS)) {
            // writeNameSpace(writer, domElementPrefix, domElementNS );
            registeredNSStack.get().push(domElementNS);
        } else if (domElementPrefix.isEmpty() && ! domElementNS.isEmpty()) {
            writeNameSpace(writer, "xmlns", domElementNS);
        }

        // Deal with Attributes
        NamedNodeMap attrs = domElement.getAttributes();
        for (int i = 0, len = attrs.getLength(); i < len; ++i) {
            Attr attr = (Attr) attrs.item(i);
            String attributePrefix = attr.getPrefix();
            String attribLocalName = attr.getLocalName();
            String attribValue = attr.getValue();

            if (attributePrefix == null || attributePrefix.length() == 0) {
                if (!("xmlns".equals(attribLocalName))) {
                    writeAttribute(writer, attribLocalName, attribValue);
                }
            } else {
                if ("xmlns".equals(attributePrefix)) {
                    writeNameSpace(writer, attribLocalName, attribValue);
                } else {
                    writeAttribute(writer, new QName(attr.getNamespaceURI(), attribLocalName, attributePrefix), attribValue);
                }
            }
        }

        for (Node child = domElement.getFirstChild(); child != null; child = child.getNextSibling()) {
            writeDOMNode(writer, child);
        }

        writeEndElement(writer);
    }

    /**
     * Write a namespace
     *
     * @param writer
     * @param prefix prefix
     * @param ns Namespace URI
     *
     * @throws ProcessingException
     */
    public static void writeNameSpace(XMLStreamWriter writer, String prefix, String ns) throws ProcessingException {
        try {
            writer.writeNamespace(prefix, ns);
        } catch (XMLStreamException e) {
            throw logger.processingError(e);
        }
    }

    /**
     * Write a start element
     *
     * @param writer
     * @param prefix
     * @param localPart
     * @param ns
     *
     * @throws ProcessingException
     */
    public static void writeStartElement(XMLStreamWriter writer, String prefix, String localPart, String ns)
            throws ProcessingException {
        try {
            writer.writeStartElement(prefix, localPart, ns);
        } catch (XMLStreamException e) {
            throw logger.processingError(e);
        }
    }

    /**
     * <p> Write an end element. The stream writer keeps track of which start element needs to be closed with an end
     * tag. </p>
     *
     * @param writer
     *
     * @throws ProcessingException
     */
    public static void writeEndElement(XMLStreamWriter writer) throws ProcessingException {
        try {
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            throw logger.processingError(e);
        }
    }

    private static XMLOutputFactory getXMLOutputFactory() {
        boolean tccl_jaxp = SystemPropertiesUtil.getSystemProperty(GeneralConstants.TCCL_JAXP, "false")
                .equalsIgnoreCase("true");
        ClassLoader prevTCCL = SecurityActions.getTCCL();
        try {
            if (tccl_jaxp) {
                SecurityActions.setTCCL(StaxUtil.class.getClassLoader());
            }
            return XMLOutputFactory.newInstance();
        } finally {
            if (tccl_jaxp) {
                SecurityActions.setTCCL(prevTCCL);
            }
        }
    }
}