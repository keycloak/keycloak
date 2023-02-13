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
package org.keycloak.saml.processing.core.parsers.saml.assertion;

import org.keycloak.dom.saml.v2.assertion.EncryptedElementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectConfirmationDataType;
import org.keycloak.dom.saml.v2.assertion.SubjectConfirmationType;
import org.keycloak.saml.common.ErrorCodes;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.parsers.StaxParser;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.parsers.util.SAMLParserUtil;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;
import org.w3c.dom.Element;

public class SAMLSubjectConfirmationParser extends AbstractStaxSamlAssertionParser<SubjectConfirmationType> implements StaxParser {

    public static final SAMLSubjectConfirmationParser INSTANCE = new SAMLSubjectConfirmationParser();

    public SAMLSubjectConfirmationParser() {
        super(SAMLAssertionQNames.SUBJECT_CONFIRMATION);
    }

    @Override
    protected SubjectConfirmationType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        final SubjectConfirmationType res = new SubjectConfirmationType();

        res.setMethod(StaxParserUtil.getAttributeValue(element, SAMLAssertionQNames.ATTR_METHOD));

        return res;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, SubjectConfirmationType target, SAMLAssertionQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case NAMEID:
                NameIDType nameID = SAMLParserUtil.parseNameIDType(xmlEventReader);
                target.setNameID(nameID);
                break;

            case ENCRYPTED_ID:
                Element domElement = StaxParserUtil.getDOMElement(xmlEventReader);
                target.setEncryptedID(new EncryptedElementType(domElement));
                break;

            case SUBJECT_CONFIRMATION_DATA:
                SubjectConfirmationDataType subjectConfirmationData = SAMLSubjectConfirmationDataParser.INSTANCE.parse(xmlEventReader);
                target.setSubjectConfirmationData(subjectConfirmationData);
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}