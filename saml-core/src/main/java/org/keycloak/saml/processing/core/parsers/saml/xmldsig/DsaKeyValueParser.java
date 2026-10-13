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

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.keycloak.dom.xmlsec.w3.xmldsig.DSAKeyValueType;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

public class DsaKeyValueParser extends AbstractStaxXmlDSigParser<DSAKeyValueType> {

    public static final DsaKeyValueParser INSTANCE = new DsaKeyValueParser();

    private DsaKeyValueParser() {
        super(XmlDSigQNames.DSA_KEY_VALUE);
    }

    public static DsaKeyValueParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected DSAKeyValueType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        return new DSAKeyValueType();
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, DSAKeyValueType target, XmlDSigQNames element, StartElement elementDetail) throws ParsingException {
        String text;
        switch (element) {
            case P:
                StaxParserUtil.advance(xmlEventReader);
                text = StaxParserUtil.getElementText(xmlEventReader);
                target.setP(text.getBytes(GeneralConstants.SAML_CHARSET));
                break;

            case Q:
                StaxParserUtil.advance(xmlEventReader);
                text = StaxParserUtil.getElementText(xmlEventReader);
                target.setQ(text.getBytes(GeneralConstants.SAML_CHARSET));
                break;

            case G:
                StaxParserUtil.advance(xmlEventReader);
                text = StaxParserUtil.getElementText(xmlEventReader);
                target.setG(text.getBytes(GeneralConstants.SAML_CHARSET));
                break;

            case Y:
                StaxParserUtil.advance(xmlEventReader);
                text = StaxParserUtil.getElementText(xmlEventReader);
                target.setY(text.getBytes(GeneralConstants.SAML_CHARSET));
                break;

            case J:
                StaxParserUtil.advance(xmlEventReader);
                text = StaxParserUtil.getElementText(xmlEventReader);
                target.setJ(text.getBytes(GeneralConstants.SAML_CHARSET));
                break;

            case SEED:
                StaxParserUtil.advance(xmlEventReader);
                text = StaxParserUtil.getElementText(xmlEventReader);
                target.setSeed(text.getBytes(GeneralConstants.SAML_CHARSET));
                break;

            case PGEN_COUNTER:
                StaxParserUtil.advance(xmlEventReader);
                text = StaxParserUtil.getElementText(xmlEventReader);
                target.setPgenCounter(text.getBytes(GeneralConstants.SAML_CHARSET));
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}