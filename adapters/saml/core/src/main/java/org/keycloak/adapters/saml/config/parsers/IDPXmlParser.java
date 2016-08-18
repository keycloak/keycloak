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

package org.keycloak.adapters.saml.config.parsers;

import org.keycloak.adapters.saml.config.IDP;
import org.keycloak.adapters.saml.config.Key;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.parsers.AbstractParser;
import org.keycloak.saml.common.util.StaxParserUtil;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class IDPXmlParser extends AbstractParser {

    @Override
    public Object parse(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, ConfigXmlConstants.IDP_ELEMENT);
        IDP idp = new IDP();
        String entityID = SPXmlParser.getAttributeValue(startElement, ConfigXmlConstants.ENTITY_ID_ATTR);
        if (entityID == null) {
            throw new ParsingException("entityID must be set on IDP");

        }
        idp.setEntityID(entityID);

        boolean signaturesRequired = SPXmlParser.getBooleanAttributeValue(startElement, ConfigXmlConstants.SIGNATURES_REQUIRED_ATTR);
        idp.setSignatureCanonicalizationMethod(SPXmlParser.getAttributeValue(startElement, ConfigXmlConstants.SIGNATURE_CANONICALIZATION_METHOD_ATTR));
        idp.setSignatureAlgorithm(SPXmlParser.getAttributeValue(startElement, ConfigXmlConstants.SIGNATURE_ALGORITHM_ATTR));
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent == null)
                break;
            if (xmlEvent instanceof EndElement) {
                EndElement endElement = (EndElement) StaxParserUtil.getNextEvent(xmlEventReader);
                String endElementName = StaxParserUtil.getEndElementName(endElement);
                if (endElementName.equals(ConfigXmlConstants.IDP_ELEMENT))
                    break;
                else
                    continue;
            }
            startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
            if (startElement == null)
                break;
            String tag = StaxParserUtil.getStartElementName(startElement);
            if (tag.equals(ConfigXmlConstants.SINGLE_SIGN_ON_SERVICE_ELEMENT)) {
                IDP.SingleSignOnService sso = parseSingleSignOnService(xmlEventReader, signaturesRequired);
                idp.setSingleSignOnService(sso);

            } else if (tag.equals(ConfigXmlConstants.SINGLE_LOGOUT_SERVICE_ELEMENT)) {
                IDP.SingleLogoutService slo = parseSingleLogoutService(xmlEventReader, signaturesRequired);
                idp.setSingleLogoutService(slo);

            } else if (tag.equals(ConfigXmlConstants.KEYS_ELEMENT)) {
                KeysXmlParser parser = new KeysXmlParser();
                List<Key> keys = (List<Key>)parser.parse(xmlEventReader);
                idp.setKeys(keys);
            } else {
                StaxParserUtil.bypassElementBlock(xmlEventReader, tag);
            }

        }
        return idp;
    }

    protected IDP.SingleLogoutService parseSingleLogoutService(XMLEventReader xmlEventReader, boolean signaturesRequired) throws ParsingException {
        IDP.SingleLogoutService slo = new IDP.SingleLogoutService();
        StartElement element = StaxParserUtil.getNextStartElement(xmlEventReader);
        slo.setSignRequest(SPXmlParser.getBooleanAttributeValue(element, ConfigXmlConstants.SIGN_REQUEST_ATTR, signaturesRequired));
        slo.setValidateResponseSignature(SPXmlParser.getBooleanAttributeValue(element, ConfigXmlConstants.VALIDATE_RESPONSE_SIGNATURE_ATTR, signaturesRequired));
        slo.setValidateRequestSignature(SPXmlParser.getBooleanAttributeValue(element, ConfigXmlConstants.VALIDATE_REQUEST_SIGNATURE_ATTR, signaturesRequired));
        slo.setRequestBinding(SPXmlParser.getAttributeValue(element, ConfigXmlConstants.REQUEST_BINDING_ATTR));
        slo.setResponseBinding(SPXmlParser.getAttributeValue(element, ConfigXmlConstants.RESPONSE_BINDING_ATTR));
        slo.setSignResponse(SPXmlParser.getBooleanAttributeValue(element, ConfigXmlConstants.SIGN_RESPONSE_ATTR, signaturesRequired));
        slo.setPostBindingUrl(SPXmlParser.getAttributeValue(element, ConfigXmlConstants.POST_BINDING_URL_ATTR));
        slo.setRedirectBindingUrl(SPXmlParser.getAttributeValue(element, ConfigXmlConstants.REDIRECT_BINDING_URL_ATTR));
        return slo;
    }

    protected IDP.SingleSignOnService parseSingleSignOnService(XMLEventReader xmlEventReader, boolean signaturesRequired) throws ParsingException {
        IDP.SingleSignOnService sso = new IDP.SingleSignOnService();
        StartElement element = StaxParserUtil.getNextStartElement(xmlEventReader);
        sso.setSignRequest(SPXmlParser.getBooleanAttributeValue(element, ConfigXmlConstants.SIGN_REQUEST_ATTR, signaturesRequired));
        sso.setValidateResponseSignature(SPXmlParser.getBooleanAttributeValue(element, ConfigXmlConstants.VALIDATE_RESPONSE_SIGNATURE_ATTR, signaturesRequired));
        sso.setValidateAssertionSignature(SPXmlParser.getBooleanAttributeValue(element, ConfigXmlConstants.VALIDATE_ASSERTION_SIGNATURE_ATTR));
        sso.setRequestBinding(SPXmlParser.getAttributeValue(element, ConfigXmlConstants.REQUEST_BINDING_ATTR));
        sso.setResponseBinding(SPXmlParser.getAttributeValue(element, ConfigXmlConstants.RESPONSE_BINDING_ATTR));
        sso.setBindingUrl(SPXmlParser.getAttributeValue(element, ConfigXmlConstants.BINDING_URL_ATTR));
        return sso;
    }

    @Override
    public boolean supports(QName qname) {
        return false;
    }
}
