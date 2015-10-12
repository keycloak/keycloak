package org.keycloak.adapters.saml.config.parsers;

import org.keycloak.adapters.saml.config.Key;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.parsers.AbstractParser;
import org.keycloak.saml.common.util.StaxParserUtil;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeyXmlParser extends AbstractParser {

    @Override
    public Object parse(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, ConfigXmlConstants.KEY_ELEMENT);
        Key key = new Key();
        key.setSigning(SPXmlParser.getBooleanAttributeValue(startElement, ConfigXmlConstants.SIGNING_ATTR));
        key.setEncryption(SPXmlParser.getBooleanAttributeValue(startElement, ConfigXmlConstants.ENCRYPTION_ATTR));
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent == null)
                break;
            if (xmlEvent instanceof EndElement) {
                EndElement endElement = (EndElement) StaxParserUtil.getNextEvent(xmlEventReader);
                String endElementName = StaxParserUtil.getEndElementName(endElement);
                if (endElementName.equals(ConfigXmlConstants.KEY_ELEMENT))
                    break;
                else
                    throw logger.parserUnknownEndElement(endElementName);
            }
            startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
            if (startElement == null)
                break;
            String tag = StaxParserUtil.getStartElementName(startElement);
            if (tag.equals(ConfigXmlConstants.KEYS_STORE_ELEMENT)) {
                key.setKeystore(parseKeyStore(xmlEventReader));
            } else if (tag.equals(ConfigXmlConstants.CERTIFICATE_PEM_ELEMENT)) {
                StartElement element = StaxParserUtil.getNextStartElement(xmlEventReader);
                key.setCertificatePem(SPXmlParser.getElementText(xmlEventReader));
            } else if (tag.equals(ConfigXmlConstants.PUBLIC_KEY_PEM_ELEMENT)) {
                StartElement element = StaxParserUtil.getNextStartElement(xmlEventReader);
                key.setPublicKeyPem(SPXmlParser.getElementText(xmlEventReader));
            } else if (tag.equals(ConfigXmlConstants.PRIVATE_KEY_PEM_ELEMENT)) {
                StartElement element = StaxParserUtil.getNextStartElement(xmlEventReader);
                key.setPrivateKeyPem(SPXmlParser.getElementText(xmlEventReader));
            } else {
                StaxParserUtil.bypassElementBlock(xmlEventReader, tag);
            }

        }
        return key;
    }

    protected Key.KeyStoreConfig parseKeyStore(XMLEventReader xmlEventReader)  throws ParsingException {
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, ConfigXmlConstants.KEYS_STORE_ELEMENT);
        Key.KeyStoreConfig keyStore = new Key.KeyStoreConfig();
        keyStore.setType(SPXmlParser.getAttributeValue(startElement, ConfigXmlConstants.TYPE_ATTR));
        keyStore.setAlias(SPXmlParser.getAttributeValue(startElement, ConfigXmlConstants.ALIAS_ATTR));
        keyStore.setFile(SPXmlParser.getAttributeValue(startElement, ConfigXmlConstants.FILE_ATTR));
        keyStore.setResource(SPXmlParser.getAttributeValue(startElement, ConfigXmlConstants.RESOURCE_ATTR));
        if (keyStore.getFile() == null && keyStore.getResource() == null) {
            throw new ParsingException("KeyStore element must have the url or classpath attribute set");
        }
        keyStore.setPassword(SPXmlParser.getAttributeValue(startElement, ConfigXmlConstants.PASSWORD_ATTR));
        if (keyStore.getPassword() == null) {
            throw new ParsingException("KeyStore element must have the password attribute set");
        }



        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent == null)
                break;
            if (xmlEvent instanceof EndElement) {
                EndElement endElement = (EndElement) StaxParserUtil.getNextEvent(xmlEventReader);
                String endElementName = StaxParserUtil.getEndElementName(endElement);
                if (endElementName.equals(ConfigXmlConstants.KEYS_STORE_ELEMENT))
                    break;
                else
                    continue;
            }
            startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
            if (startElement == null)
                break;
            String tag = StaxParserUtil.getStartElementName(startElement);
            if (tag.equals(ConfigXmlConstants.CERTIFICATE_ELEMENT)) {
                StartElement element = StaxParserUtil.getNextStartElement(xmlEventReader);
                keyStore.setCertificateAlias(SPXmlParser.getAttributeValue(element, ConfigXmlConstants.ALIAS_ATTR));
                if (keyStore.getCertificateAlias() == null) {
                    throw new ParsingException("KeyStore Certificate element must have the alias attribute set");

                }
            } else if (tag.equals(ConfigXmlConstants.PRIVATE_KEY_ELEMENT)) {
                StartElement element = StaxParserUtil.getNextStartElement(xmlEventReader);
                keyStore.setPrivateKeyAlias(SPXmlParser.getAttributeValue(element, ConfigXmlConstants.ALIAS_ATTR));
                if (keyStore.getPrivateKeyAlias() == null) {
                    throw new ParsingException("KeyStore PrivateKey element must have the alias attribute set");

                }
                keyStore.setPrivateKeyPassword(SPXmlParser.getAttributeValue(element, ConfigXmlConstants.PASSWORD_ATTR));
                if (keyStore.getPrivateKeyPassword() == null) {
                    throw new ParsingException("KeyStore PrivateKey element must have the password attribute set");

                }
            } else {
                StaxParserUtil.bypassElementBlock(xmlEventReader, tag);
            }

        }
        return keyStore;

    }

    @Override
    public boolean supports(QName qname) {
        return false;
    }
}
