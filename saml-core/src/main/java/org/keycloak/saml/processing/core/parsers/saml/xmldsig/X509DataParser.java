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
package org.keycloak.saml.processing.core.parsers.saml.xmldsig;

import org.keycloak.dom.xmlsec.w3.xmldsig.X509CertificateType;
import org.keycloak.dom.xmlsec.w3.xmldsig.X509DataType;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

/**
 * Base Class for all Response Type parsing for SAML2
 *
 */
public class X509DataParser extends AbstractStaxXmlDSigParser<X509DataType> {

    private static final X509DataParser INSTANCE = new X509DataParser();

    public X509DataParser() {
        super(XmlDSigQNames.X509_DATA);
    }

    public static X509DataParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected X509DataType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        return new X509DataType();
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, X509DataType target, XmlDSigQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case X509_CERTIFICATE:
                StaxParserUtil.advance(xmlEventReader);
                String certValue = StaxParserUtil.getElementText(xmlEventReader);

                X509CertificateType cert = new X509CertificateType();
                cert.setEncodedCertificate(certValue.getBytes(GeneralConstants.SAML_CHARSET));
                target.add(cert);
                
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}