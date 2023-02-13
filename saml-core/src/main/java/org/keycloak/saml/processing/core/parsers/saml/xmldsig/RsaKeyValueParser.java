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
package org.keycloak.saml.processing.core.parsers.saml.xmldsig;

import org.keycloak.dom.xmlsec.w3.xmldsig.RSAKeyValueType;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

public class RsaKeyValueParser extends AbstractStaxXmlDSigParser<RSAKeyValueType> {

    public static final RsaKeyValueParser INSTANCE = new RsaKeyValueParser();

    private RsaKeyValueParser() {
        super(XmlDSigQNames.RSA_KEY_VALUE);
    }

    public static RsaKeyValueParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected RSAKeyValueType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        return new RSAKeyValueType();
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, RSAKeyValueType target, XmlDSigQNames element, StartElement elementDetail) throws ParsingException {
        String text;
        switch (element) {
            case MODULUS:
                StaxParserUtil.advance(xmlEventReader);
                text = StaxParserUtil.getElementText(xmlEventReader);
                target.setModulus(text.getBytes(GeneralConstants.SAML_CHARSET));
                break;

            case EXPONENT:
                StaxParserUtil.advance(xmlEventReader);
                text = StaxParserUtil.getElementText(xmlEventReader);
                target.setExponent(text.getBytes(GeneralConstants.SAML_CHARSET));
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}