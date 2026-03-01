/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

import java.util.Objects;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

/**
 * Simple support for STaX type of parsing. Parses single element and allows processing its direct children.
 *
 * @param <T> Java class that will be result of parsing this element
 * @param <E> Type containing all tokens that can be found in subelements of the element parsed by this parser, usually an enum
 * @author hmlnarik
 */
public abstract class AbstractStaxParser<T, E> implements StaxParser {

    protected static final PicketLinkLogger LOGGER = PicketLinkLoggerFactory.getLogger();
    protected final QName expectedStartElement;
    private final E unknownElement;

    public AbstractStaxParser(QName expectedStartElement, E unknownElement) {
        this.unknownElement = unknownElement;
        this.expectedStartElement = expectedStartElement;
    }

    @Override
    public T parse(XMLEventReader xmlEventReader) throws ParsingException {
        // STATE: should be before the expected start element

        // Get the start element and validate it is the expected one
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        final QName actualQName = startElement.getName();
        validateStartElement(startElement);
        T target = instantiateElement(xmlEventReader, startElement);

        // STATE: Start element has been read.
        QName currentSubelement = null;

        while (xmlEventReader.hasNext()) {
            // STATE: the only end element that can be found at this phase must correspond to the expected start element
            XMLEvent xmlEvent = StaxParserUtil.peekNextTag(xmlEventReader);
            if (xmlEvent == null) {
                break;
            }

            if (xmlEvent instanceof EndElement) {
                EndElement endElement = (EndElement) xmlEvent;
                final QName qName = endElement.getName();

                // If leftover from processed subelement, just consume.
                if (Objects.equals(qName, currentSubelement)) {
                    StaxParserUtil.advance(xmlEventReader);
                    currentSubelement = null;
                    continue;
                }

                // If end element corresponding to this start element, stop processing.
                if (Objects.equals(qName, actualQName)) {
                    // consume the end element and finish parsing of this tag
                    StaxParserUtil.advance(xmlEventReader);
                    break;
                }

                // No other case is valid
                String elementName = StaxParserUtil.getElementName(endElement);
                throw LOGGER.parserUnknownEndElement(elementName, xmlEvent.getLocation());
            }

            startElement = (StartElement) xmlEvent;
            currentSubelement = startElement.getName();
            E token = getElementFromName(currentSubelement);
            if (token == null) {
                token = unknownElement;
            }
            processSubElement(xmlEventReader, target, token, startElement);

            // If the XMLEventReader has not advanced inside processSubElement (hence using "==" and not "equals"), advance it.
            if (StaxParserUtil.peek(xmlEventReader) == startElement) {
                StaxParserUtil.bypassElementBlock(xmlEventReader);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("Element %s bypassed", currentSubelement));
                }
            }

            // In case of recursive nesting the same element, the corresponding end element MUST be handled
            // in the {@code processSubElement} method and MUST NOT be consumed here.
            if (Objects.equals(actualQName, currentSubelement) || isUnknownElement(token)) {
                currentSubelement = null;
            }
        }
        return target;
    }

    /**
     * Validates that the given startElement has the expected properties (namely {@link QName} matches the expected one).
     * @param startElement
     * @return
     */
    protected void validateStartElement(StartElement startElement) {
        StaxParserUtil.validate(startElement, expectedStartElement);
    }

    protected boolean isUnknownElement(E token) {
        return token == null || Objects.equals(token, unknownElement);
    }

    protected abstract E getElementFromName(QName name);

    /**
     * Instantiates the target Java class representing the current element.<br>
     * <b>Precondition:</b> Current event is the {@link StartElement}<br>
     * <b>Postcondition:</b> Current event is the {@link StartElement} or the {@link EndElement} corresponding to the {@link StartElement}
     * @param xmlEventReader
     * @param element The XML event that was just read from the {@code xmlEventReader}
     * @return
     * @throws ParsingException
     */
    protected abstract T instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException;

    /**
     * Processes the subelement of the element processed in {@link #instantiateElement} method.<br>
     * <b>Precondition:</b> Current event: Last before the {@link StartElement} corresponding to the processed subelement, i.e.
     *    event obtained by {@link XMLEventReader#next()} is the {@link StartElement} of the subelement being processed<br>
     * <b>Postcondition:</b> Event obtained by {@link XMLEventReader#next()} is either
     *    the same {@link StartElement} (i.e. no change in position which causes this subelement to be skipped),
     *    the corresponding {@link EndElement}, or the event after the corresponding {@link EndElement}.
     * <p>
     * Note that in case of recursive nesting the same element, the corresponding end element MUST be consumed in this method.
     * @param xmlEventReader
     * @param target Target object (the one created by the {@link #instantiateElement} method.
     * @param element The constant corresponding to the current start element.
     * @param elementDetail The XML event that was just read from the {@code xmlEventReader}
     * @return
     * @throws ParsingException
     */
    protected abstract void processSubElement(XMLEventReader xmlEventReader, T target, E element, StartElement elementDetail) throws ParsingException;

}
