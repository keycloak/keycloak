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
package org.keycloak.saml.processing.core.parsers.saml.assertion;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.keycloak.dom.saml.v2.assertion.EncryptedElementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.parsers.StaxParser;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.parsers.util.SAMLParserUtil;

import org.w3c.dom.Element;

/**
 * Parse the saml subject
 *
 * @since Oct 12, 2010
 */
public class SAMLSubjectParser extends AbstractStaxSamlAssertionParser<SubjectType> implements StaxParser {

    private static final SAMLSubjectParser INSTANCE = new SAMLSubjectParser();

    private SAMLSubjectParser() {
        super(SAMLAssertionQNames.SUBJECT);
    }

    public static SAMLSubjectParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected SubjectType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        return new SubjectType();
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, SubjectType target, SAMLAssertionQNames element, StartElement elementDetail) throws ParsingException {
        SubjectType.STSubType subType;
        switch (element) {
            case NAMEID:
                NameIDType nameID = SAMLParserUtil.parseNameIDType(xmlEventReader);
                subType = new SubjectType.STSubType();
                subType.addBaseID(nameID);
                target.setSubType(subType);
                break;

            case ENCRYPTED_ID:
                Element domElement = StaxParserUtil.getDOMElement(xmlEventReader);
                subType = new SubjectType.STSubType();
                subType.setEncryptedID(new EncryptedElementType(domElement));
                target.setSubType(subType);
                break;

            case SUBJECT_CONFIRMATION:
                target.addConfirmation(SAMLSubjectConfirmationParser.INSTANCE.parse(xmlEventReader));
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}