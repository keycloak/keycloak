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

import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType.ASTChoiceType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

/**
 * Parse the <conditions> in the saml assertion
 *
 * @since Oct 14, 2010
 */
public class SAMLAttributeStatementParser extends AbstractStaxSamlAssertionParser<AttributeStatementType> {

    private static final SAMLAttributeStatementParser INSTANCE = new SAMLAttributeStatementParser();

    private SAMLAttributeStatementParser() {
        super(SAMLAssertionQNames.ATTRIBUTE_STATEMENT);
    }

    public static SAMLAttributeStatementParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected AttributeStatementType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        return new AttributeStatementType();
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, AttributeStatementType target, SAMLAssertionQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case ATTRIBUTE:
                target.addAttribute(new ASTChoiceType(SAMLAttributeParser.getInstance().parse(xmlEventReader)));
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}