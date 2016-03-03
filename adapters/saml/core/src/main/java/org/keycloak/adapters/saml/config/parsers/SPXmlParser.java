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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.keycloak.adapters.saml.config.IDP;
import org.keycloak.adapters.saml.config.Key;
import org.keycloak.adapters.saml.config.SP;
import org.keycloak.common.util.StringPropertyReplacer;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.parsers.AbstractParser;
import org.keycloak.saml.common.util.StaxParserUtil;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SPXmlParser extends AbstractParser {

    public static String getAttributeValue(StartElement startElement, String tag) {
        String str = StaxParserUtil.getAttributeValue(startElement, tag);
        if (str != null)
            return StringPropertyReplacer.replaceProperties(str);
        else
            return str;
    }

    public static boolean getBooleanAttributeValue(StartElement startElement, String tag, boolean defaultValue) {
        String result = getAttributeValue(startElement, tag);
        if (result == null)
            return defaultValue;
        return Boolean.valueOf(result);
    }

    public static boolean getBooleanAttributeValue(StartElement startElement, String tag) {
        return getBooleanAttributeValue(startElement, tag, false);
    }

    public static String getElementText(XMLEventReader xmlEventReader) throws ParsingException {
        String result = StaxParserUtil.getElementText(xmlEventReader);
        if (result != null)
            result = StringPropertyReplacer.replaceProperties(result);
        return result;
    }

    @Override
    public Object parse(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, ConfigXmlConstants.SP_ELEMENT);
        SP sp = new SP();
        String entityID = getAttributeValue(startElement, ConfigXmlConstants.ENTITY_ID_ATTR);
        if (entityID == null) {
            throw new ParsingException("entityID must be set on SP");

        }
        sp.setEntityID(entityID);
        sp.setSslPolicy(getAttributeValue(startElement, ConfigXmlConstants.SSL_POLICY_ATTR));
        sp.setLogoutPage(getAttributeValue(startElement, ConfigXmlConstants.LOGOUT_PAGE_ATTR));
        sp.setNameIDPolicyFormat(getAttributeValue(startElement, ConfigXmlConstants.NAME_ID_POLICY_FORMAT_ATTR));
        sp.setForceAuthentication(getBooleanAttributeValue(startElement, ConfigXmlConstants.FORCE_AUTHENTICATION_ATTR));
        sp.setIsPassive(getBooleanAttributeValue(startElement, ConfigXmlConstants.IS_PASSIVE_ATTR));
        sp.setTurnOffChangeSessionIdOnLogin(getBooleanAttributeValue(startElement, ConfigXmlConstants.TURN_OFF_CHANGE_SESSSION_ID_ON_LOGIN_ATTR));
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent == null)
                break;
            if (xmlEvent instanceof EndElement) {
                EndElement endElement = (EndElement) StaxParserUtil.getNextEvent(xmlEventReader);
                String endElementName = StaxParserUtil.getEndElementName(endElement);
                if (endElementName.equals(ConfigXmlConstants.SP_ELEMENT))
                    break;
                else
                    continue;
            }
            startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
            if (startElement == null)
                break;
            String tag = StaxParserUtil.getStartElementName(startElement);
            if (tag.equals(ConfigXmlConstants.KEYS_ELEMENT)) {
                KeysXmlParser parser = new KeysXmlParser();
                List<Key> keys = (List<Key>) parser.parse(xmlEventReader);
                sp.setKeys(keys);
            } else if (tag.equals(ConfigXmlConstants.PRINCIPAL_NAME_MAPPING_ELEMENT)) {
                StartElement element = StaxParserUtil.getNextStartElement(xmlEventReader);
                String policy = getAttributeValue(element, ConfigXmlConstants.POLICY_ATTR);
                if (policy == null) {
                    throw new ParsingException("PrincipalNameMapping element must have the policy attribute set");

                }
                String attribute = getAttributeValue(element, ConfigXmlConstants.ATTRIBUTE_ATTR);
                SP.PrincipalNameMapping mapping = new SP.PrincipalNameMapping();
                mapping.setPolicy(policy);
                mapping.setAttributeName(attribute);
                sp.setPrincipalNameMapping(mapping);

            } else if (tag.equals(ConfigXmlConstants.ROLE_IDENTIFIERS_ELEMENT)) {
                parseRoleMapping(xmlEventReader, sp);
            } else if (tag.equals(ConfigXmlConstants.IDP_ELEMENT)) {
                IDPXmlParser parser = new IDPXmlParser();
                IDP idp = (IDP) parser.parse(xmlEventReader);
                sp.setIdp(idp);
            } else {
                StaxParserUtil.bypassElementBlock(xmlEventReader, tag);
            }

        }
        return sp;
    }

    protected void parseRoleMapping(XMLEventReader xmlEventReader, SP sp) throws ParsingException {
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, ConfigXmlConstants.ROLE_IDENTIFIERS_ELEMENT);
        Set<String> roleAttributes = new HashSet<>();
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent == null)
                break;
            if (xmlEvent instanceof EndElement) {
                EndElement endElement = (EndElement) StaxParserUtil.getNextEvent(xmlEventReader);
                String endElementName = StaxParserUtil.getEndElementName(endElement);
                if (endElementName.equals(ConfigXmlConstants.ROLE_IDENTIFIERS_ELEMENT))
                    break;
                else
                    continue;
            }
            startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
            if (startElement == null)
                break;
            String tag = StaxParserUtil.getStartElementName(startElement);
            if (tag.equals(ConfigXmlConstants.ATTRIBUTE_ELEMENT)) {
                StartElement element = StaxParserUtil.getNextStartElement(xmlEventReader);
                String attributeValue = getAttributeValue(element, ConfigXmlConstants.NAME_ATTR);
                if (attributeValue == null) {
                    throw new ParsingException("RoleMapping Attribute element must have the name attribute set");

                }
                roleAttributes.add(attributeValue);
            } else {
                StaxParserUtil.bypassElementBlock(xmlEventReader, tag);
            }

        }
        sp.setRoleAttributes(roleAttributes);
    }

    @Override
    public boolean supports(QName qname) {
        return false;
    }
}
