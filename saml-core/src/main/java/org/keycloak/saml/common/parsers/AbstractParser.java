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
package org.keycloak.saml.common.parsers;

import java.io.InputStream;
import java.util.regex.Pattern;
import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.keycloak.common.util.Environment;
import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.SecurityActions;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.common.util.SystemPropertiesUtil;

import org.w3c.dom.Node;

/**
 * Base class for parsers
 *
 * @author Anil.Saldhana@redhat.com
 * @since Oct 12, 2010
 */
public abstract class AbstractParser implements StaxParser {

    protected static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    private static final ThreadLocal<XMLInputFactory> XML_INPUT_FACTORY = new ThreadLocal<XMLInputFactory>() {
        @Override
        protected XMLInputFactory initialValue() {
            return getXMLInputFactory();
        }

        /**
         * Get the JAXP {@link XMLInputFactory}
         *
         * @return
         */
        private XMLInputFactory getXMLInputFactory() {
            boolean tccl_jaxp = SystemPropertiesUtil.getSystemProperty(GeneralConstants.TCCL_JAXP, "false")
                    .equalsIgnoreCase("true");
            ClassLoader prevTCCL = SecurityActions.getTCCL();
            try {
                if (tccl_jaxp) {
                    SecurityActions.setTCCL(AbstractParser.class.getClassLoader());
                }
                return XMLInputFactory.newInstance();
            } finally {
                if (tccl_jaxp) {
                    SecurityActions.setTCCL(prevTCCL);
                }
            }
        }
    };

    /**
     * Parse an InputStream for payload
     *
     * @param stream
     *
     * @return
     *
     * @throws {@link IllegalArgumentException}
     * @throws {@link IllegalArgumentException} when the configStream is null
     */
    public Object parse(InputStream stream) throws ParsingException {
        XMLEventReader xmlEventReader = createEventReader(stream);
        return parse(xmlEventReader);
    }

    public Object parse(Source source) throws ParsingException {
        XMLEventReader xmlEventReader = createEventReader(source);
        return parse(xmlEventReader);
    }

    public Object parse(Node node) throws ParsingException {
        return parse(new DOMSource(node));
    }

    public static XMLEventReader createEventReader(InputStream configStream) throws ParsingException {
        if (configStream == null)
            throw logger.nullArgumentError("InputStream");

        XMLEventReader xmlEventReader = StaxParserUtil.getXMLEventReader(configStream);

        return filterWhitespaces(xmlEventReader);
    }

    public XMLEventReader createEventReader(Source source) throws ParsingException {
        if (source == null)
            throw logger.nullArgumentError("Source");

        XMLEventReader xmlEventReader = StaxParserUtil.getXMLEventReader(source);

        return filterWhitespaces(xmlEventReader);
    }

    private static final Pattern WHITESPACE_ONLY = Pattern.compile("\\s*");

    /**
     * Creates a derived {@link XMLEventReader} that ignores all events except for: {@link StartElement},
     * {@link EndElement}, and non-empty and non-whitespace-only {@link Characters}.
     * 
     * @param xmlEventReader Original {@link XMLEventReader}
     * @return Derived {@link XMLEventReader}
     * @throws XMLStreamException
     */
    private static XMLEventReader filterWhitespaces(XMLEventReader xmlEventReader) throws ParsingException {
        XMLInputFactory xmlInputFactory = XML_INPUT_FACTORY.get();

        try {
            xmlEventReader = xmlInputFactory.createFilteredReader(xmlEventReader, new EventFilter() {
                @Override
                public boolean accept(XMLEvent xmlEvent) {
                    // We are going to disregard characters that are new line and whitespace
                    if (xmlEvent.isCharacters()) {
                        Characters chars = xmlEvent.asCharacters();
                        String data = chars.getData();
                        return data != null && ! WHITESPACE_ONLY.matcher(data).matches();
                    } else {
                        return xmlEvent.isStartElement() || xmlEvent.isEndElement();
                    }
                }
            });
        } catch (XMLStreamException ex) {
            throw logger.parserException(ex);
        }

        // Handle IBM JDK bug with Stax parsing when EventReader presented
        if (Environment.IS_IBM_JAVA) {
            final XMLEventReader origReader = xmlEventReader;

            xmlEventReader = new EventReaderDelegate(origReader) {

                @Override
                public boolean hasNext() {
                    boolean hasNext = super.hasNext();
                    try {
                        return hasNext && (origReader.peek() != null);
                    } catch (XMLStreamException xse) {
                        throw new IllegalStateException(xse);
                    }
                }

            };
        }

        return xmlEventReader;
    }

}