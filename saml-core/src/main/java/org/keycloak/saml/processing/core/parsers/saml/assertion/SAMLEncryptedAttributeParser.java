package org.keycloak.saml.processing.core.parsers.saml.assertion;

import org.keycloak.dom.saml.v2.assertion.SAMLEncryptedAttribute;
import org.keycloak.saml.processing.core.parsers.saml.xmlsec.EncryptedDataParser;
import org.keycloak.saml.processing.core.parsers.saml.xmlsec.EncryptedKeyParser;
import org.jboss.logging.Logger;
import org.keycloak.dom.xmlsec.w3.xmlenc.EncryptedKeyType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

public class SAMLEncryptedAttributeParser extends AbstractStaxSamlAssertionParser<SAMLEncryptedAttribute> {
    protected static final Logger logger = Logger.getLogger(SAMLEncryptedAttributeParser.class);

    private static final SAMLEncryptedAttributeParser INSTANCE = new SAMLEncryptedAttributeParser();

    private SAMLEncryptedAttributeParser() {
        super(SAMLAssertionQNames.ENCRYPTED_ATTRIBUTE);
    }

    public static SAMLEncryptedAttributeParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected SAMLEncryptedAttribute instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        return new SAMLEncryptedAttribute();
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, SAMLEncryptedAttribute target, SAMLAssertionQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case ENCRYPTED_DATA:
                target.setEncryptedData(EncryptedDataParser.getInstance().parse(xmlEventReader));
                break;
            case ENCRYPTED_KEY:
                EncryptedKeyType encryptedKey = EncryptedKeyParser.getInstance().parse(xmlEventReader);
                encryptedKey.setId(elementDetail.getAttributeByName(QName.valueOf("Id")).getValue());
                Attribute recipient = elementDetail.getAttributeByName(QName.valueOf("Recipient"));
                if(recipient != null)
                    encryptedKey.setRecipient(recipient.getValue());
                target.addEncryptedKey(encryptedKey);
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}
