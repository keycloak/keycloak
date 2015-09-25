package org.keycloak.adapters.saml.config.parsers;

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
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeysXmlParser extends AbstractParser {

    @Override
    public Object parse(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, ConfigXmlConstants.KEYS_ELEMENT);
        List<Key> keys = new LinkedList<>();
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent == null)
                break;
            if (xmlEvent instanceof EndElement) {
                EndElement endElement = (EndElement) StaxParserUtil.getNextEvent(xmlEventReader);
                String endElementName = StaxParserUtil.getEndElementName(endElement);
                if (endElementName.equals(ConfigXmlConstants.KEYS_ELEMENT))
                    break;
                else
                    throw logger.parserUnknownEndElement(endElementName);
            }
            startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
            if (startElement == null)
                break;
            String tag = StaxParserUtil.getStartElementName(startElement);
            if (tag.equals(ConfigXmlConstants.KEY_ELEMENT)) {
                KeyXmlParser parser = new KeyXmlParser();
                Key key = (Key)parser.parse(xmlEventReader);
                keys.add(key);
            } else {
                StaxParserUtil.bypassElementBlock(xmlEventReader, tag);
            }

        }
        return keys;
    }

    @Override
    public boolean supports(QName qname) {
        return false;
    }
}
