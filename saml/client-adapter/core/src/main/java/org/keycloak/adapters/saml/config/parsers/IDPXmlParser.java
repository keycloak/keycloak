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
        String entityID = StaxParserUtil.getAttributeValue(startElement, ConfigXmlConstants.ENTITY_ID_ATTR);
        if (entityID == null) {
            throw new ParsingException("entityID must be set on IDP");

        }
        idp.setEntityID(entityID);
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
                IDP.SingleSignOnService sso = parseSingleSignOnService(xmlEventReader);
                idp.setSingleSignOnService(sso);

            } else if (tag.equals(ConfigXmlConstants.SINGLE_LOGOUT_SERVICE_ELEMENT)) {
                IDP.SingleLogoutService slo = parseSingleLogoutService(xmlEventReader);
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

    protected IDP.SingleLogoutService parseSingleLogoutService(XMLEventReader xmlEventReader) throws ParsingException {
        IDP.SingleLogoutService slo = new IDP.SingleLogoutService();
        StartElement element = StaxParserUtil.getNextStartElement(xmlEventReader);
        slo.setSignRequest(StaxParserUtil.getBooleanAttributeValue(element, ConfigXmlConstants.SIGN_REQUEST_ATTR));
        slo.setValidateResponseSignature(StaxParserUtil.getBooleanAttributeValue(element, ConfigXmlConstants.VALIDATE_RESPONSE_SIGNATURE_ATTR));
        slo.setValidateRequestSignature(StaxParserUtil.getBooleanAttributeValue(element, ConfigXmlConstants.VALIDATE_REQUEST_SIGNATURE_ATTR));
        slo.setRequestBinding(StaxParserUtil.getAttributeValue(element, ConfigXmlConstants.REQUEST_BINDING_ATTR));
        slo.setResponseBinding(StaxParserUtil.getAttributeValue(element, ConfigXmlConstants.RESPONSE_BINDING_ATTR));
        slo.setSignResponse(StaxParserUtil.getBooleanAttributeValue(element, ConfigXmlConstants.SIGN_RESPONSE_ATTR));
        slo.setPostBindingUrl(StaxParserUtil.getAttributeValue(element, ConfigXmlConstants.POST_BINDING_URL_ATTR));
        slo.setRedirectBindingUrl(StaxParserUtil.getAttributeValue(element, ConfigXmlConstants.REDIRECT_BINDING_URL_ATTR));
        return slo;
    }

    protected IDP.SingleSignOnService parseSingleSignOnService(XMLEventReader xmlEventReader) throws ParsingException {
        IDP.SingleSignOnService sso = new IDP.SingleSignOnService();
        StartElement element = StaxParserUtil.getNextStartElement(xmlEventReader);
        sso.setSignRequest(StaxParserUtil.getBooleanAttributeValue(element, ConfigXmlConstants.SIGN_REQUEST_ATTR));
        sso.setValidateResponseSignature(StaxParserUtil.getBooleanAttributeValue(element, ConfigXmlConstants.VALIDATE_RESPONSE_SIGNATURE_ATTR));
        sso.setRequestBinding(StaxParserUtil.getAttributeValue(element, ConfigXmlConstants.REQUEST_BINDING_ATTR));
        sso.setResponseBinding(StaxParserUtil.getAttributeValue(element, ConfigXmlConstants.RESPONSE_BINDING_ATTR));
        sso.setBindingUrl(StaxParserUtil.getAttributeValue(element, ConfigXmlConstants.BINDING_URL_ATTR));
        return sso;
    }

    @Override
    public boolean supports(QName qname) {
        return false;
    }
}
