package org.keycloak.adapters.saml.config.parsers;

import org.keycloak.adapters.saml.config.KeycloakSamlAdapter;
import org.keycloak.adapters.saml.config.SP;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.parsers.AbstractParser;
import org.keycloak.saml.common.util.StaxParserUtil;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakSamlAdapterXMLParser extends AbstractParser {

    @Override
    public Object parse(XMLEventReader xmlEventReader) throws ParsingException {
        KeycloakSamlAdapter adapter = new KeycloakSamlAdapter();
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, ConfigXmlConstants.KEYCLOAK_SAML_ADAPTER);
        while (xmlEventReader.hasNext()) {
            startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
            if (startElement == null)
                break;
            String tag = StaxParserUtil.getStartElementName(startElement);
            if (tag.equals(ConfigXmlConstants.SP_ELEMENT)) {
                SPXmlParser parser = new SPXmlParser();
                SP sp = (SP)parser.parse(xmlEventReader);
                if (sp != null) adapter.getSps().add(sp);
            } else {
                StaxParserUtil.bypassElementBlock(xmlEventReader, tag);
            }

        }
        return adapter;
    }

    @Override
    public boolean supports(QName qname) {
        return false;
    }
}
