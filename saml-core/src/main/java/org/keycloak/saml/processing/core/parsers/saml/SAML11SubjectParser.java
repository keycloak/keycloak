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
package org.keycloak.saml.processing.core.parsers.saml;

import org.keycloak.dom.saml.v1.assertion.SAML11NameIdentifierType;
import org.keycloak.dom.saml.v1.assertion.SAML11SubjectConfirmationType;
import org.keycloak.dom.saml.v1.assertion.SAML11SubjectType;
import org.keycloak.dom.saml.v1.assertion.SAML11SubjectType.SAML11SubjectTypeChoice;
import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.parsers.util.SAML11ParserUtil;
import org.keycloak.saml.processing.core.saml.v1.SAML11Constants;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.net.URI;
import org.keycloak.saml.common.parsers.StaxParser;

/**
 * Parse the saml subject
 *
 * @author Anil.Saldhana@redhat.com
 * @since Oct 12, 2010
 */
public class SAML11SubjectParser implements StaxParser {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    /**
     * @see {@link ParserNamespaceSupport#parse(XMLEventReader)}
     */
    public Object parse(XMLEventReader xmlEventReader) throws ParsingException {
        StaxParserUtil.getNextEvent(xmlEventReader);

        SAML11SubjectType subject = new SAML11SubjectType();

        // Peek at the next event
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                EndElement endElement = (EndElement) xmlEvent;
                if (StaxParserUtil.matches(endElement, JBossSAMLConstants.SUBJECT.get())) {
                    endElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                    break;
                } else
                    throw logger.parserUnknownEndElement(StaxParserUtil.getElementName(endElement), xmlEvent.getLocation());
            }

            StartElement peekedElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
            if (peekedElement == null)
                break;

            String tag = StaxParserUtil.getElementName(peekedElement);

            if (SAML11Constants.NAME_IDENTIFIER.equalsIgnoreCase(tag)) {
                peekedElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                String val = StaxParserUtil.getElementText(xmlEventReader);
                SAML11NameIdentifierType nameID = new SAML11NameIdentifierType(val);
                Attribute formatAtt = peekedElement.getAttributeByName(new QName(SAML11Constants.FORMAT));
                if (formatAtt != null) {
                    nameID.setFormat(URI.create(StaxParserUtil.getAttributeValue(formatAtt)));
                }

                Attribute nameQAtt = peekedElement.getAttributeByName(new QName(SAML11Constants.NAME_QUALIFIER));
                if (nameQAtt != null) {
                    nameID.setNameQualifier(StaxParserUtil.getAttributeValue(nameQAtt));
                }

                SAML11SubjectTypeChoice subChoice = new SAML11SubjectTypeChoice(nameID);
                subject.setChoice(subChoice);
            } else if (JBossSAMLConstants.SUBJECT_CONFIRMATION.get().equalsIgnoreCase(tag)) {
                SAML11SubjectConfirmationType subjectConfirmationType = SAML11ParserUtil
                        .parseSAML11SubjectConfirmation(xmlEventReader);
                subject.setSubjectConfirmation(subjectConfirmationType);
            } else
                throw logger.parserUnknownTag(tag, peekedElement.getLocation());
        }
        return subject;
    }


}