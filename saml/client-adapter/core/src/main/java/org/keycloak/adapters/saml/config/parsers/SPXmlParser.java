package org.keycloak.adapters.saml.config.parsers;

import org.keycloak.adapters.saml.config.IDP;
import org.keycloak.adapters.saml.config.Key;
import org.keycloak.adapters.saml.config.SP;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.parsers.AbstractParser;
import org.keycloak.saml.common.util.StaxParserUtil;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SPXmlParser extends AbstractParser {

    @Override
    public Object parse(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, ConfigXmlConstants.SP_ELEMENT);
        SP sp = new SP();
        String entityID = StaxParserUtil.getAttributeValue(startElement, ConfigXmlConstants.ENTITY_ID_ATTR);
        if (entityID == null) {
            throw new ParsingException("entityID must be set on SP");

        }
        sp.setEntityID(entityID);
        sp.setSslPolicy(StaxParserUtil.getAttributeValue(startElement, ConfigXmlConstants.SSL_POLICY_ATTR));
        sp.setLogoutPage(StaxParserUtil.getAttributeValue(startElement, ConfigXmlConstants.LOGOUT_PAGE_ATTR));
        sp.setNameIDPolicyFormat(StaxParserUtil.getAttributeValue(startElement, ConfigXmlConstants.NAME_ID_POLICY_FORMAT_ATTR));
        sp.setForceAuthentication(StaxParserUtil.getBooleanAttributeValue(startElement, ConfigXmlConstants.FORCE_AUTHENTICATION_ATTR));
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
                List<Key> keys = (List<Key>)parser.parse(xmlEventReader);
                sp.setKeys(keys);
            } else if (tag.equals(ConfigXmlConstants.PRINCIPAL_NAME_MAPPING_ELEMENT)) {
                StartElement element = StaxParserUtil.getNextStartElement(xmlEventReader);
                String policy = StaxParserUtil.getAttributeValue(element, ConfigXmlConstants.POLICY_ATTR);
                if (policy == null) {
                    throw new ParsingException("PrincipalNameMapping element must have the policy attribute set");

                }
                String attribute = StaxParserUtil.getAttributeValue(element, ConfigXmlConstants.ATTRIBUTE_ATTR);
                SP.PrincipalNameMapping mapping = new SP.PrincipalNameMapping();
                mapping.setPolicy(policy);
                mapping.setAttributeName(attribute);
                sp.setPrincipalNameMapping(mapping);

            } else if (tag.equals(ConfigXmlConstants.ROLE_MAPPING_ELEMENT)) {
                parseRoleMapping(xmlEventReader, sp);
            } else if (tag.equals(ConfigXmlConstants.IDP_ELEMENT)) {
                IDPXmlParser parser = new IDPXmlParser();
                IDP idp = (IDP)parser.parse(xmlEventReader);
                sp.setIdp(idp);
            } else {
                StaxParserUtil.bypassElementBlock(xmlEventReader, tag);
            }

        }
        return sp;
    }

    protected void parseRoleMapping(XMLEventReader xmlEventReader, SP sp)  throws ParsingException {
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, ConfigXmlConstants.ROLE_MAPPING_ELEMENT);
        Set<String> roleAttributes = new HashSet<>();
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent == null)
                break;
            if (xmlEvent instanceof EndElement) {
                EndElement endElement = (EndElement) StaxParserUtil.getNextEvent(xmlEventReader);
                String endElementName = StaxParserUtil.getEndElementName(endElement);
                if (endElementName.equals(ConfigXmlConstants.ROLE_MAPPING_ELEMENT))
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
                String attributeValue = StaxParserUtil.getAttributeValue(element, ConfigXmlConstants.NAME_ATTR);
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
