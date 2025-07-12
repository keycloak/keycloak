package org.keycloak.saml.processing.core.parsers.saml.xmlsec;

import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAssertionQNames;
import org.keycloak.dom.xmlsec.w3.xmlenc.EncryptedDataType;
import org.keycloak.dom.xmlsec.w3.xmlenc.EncryptionMethodType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.parsers.AbstractStaxParser;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.parsers.saml.xmldsig.KeyInfoParser;
import org.keycloak.saml.processing.core.parsers.util.QNameEnumLookup;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

public class EncryptedDataParser extends AbstractStaxParser<EncryptedDataType, SAMLAssertionQNames>  {

    protected static final QNameEnumLookup<SAMLAssertionQNames> LOOKUP = new QNameEnumLookup(SAMLAssertionQNames.values());

    private static final EncryptedDataParser INSTANCE = new EncryptedDataParser();

    public EncryptedDataParser() {
        super(SAMLAssertionQNames.ENCRYPTED_DATA.getQName(), SAMLAssertionQNames.UNKNOWN_ELEMENT);
    }

    public static EncryptedDataParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected SAMLAssertionQNames getElementFromName(QName name) {
        return LOOKUP.from(name);
    }

    @Override
    protected EncryptedDataType instantiateElement(XMLEventReader xmlEventReader, StartElement startElement) throws ParsingException {
        return new EncryptedDataType();
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, EncryptedDataType target, SAMLAssertionQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case ENCRYPTION_METHOD:
                target.setEncryptionMethod(new EncryptionMethodType(elementDetail.getAttributeByName(QName.valueOf("Algorithm")).getValue()));
                break;
            case KEY_INFO:
                target.setKeyInfo(KeyInfoParser.getInstance().parse(xmlEventReader));
                break;
            case CIPHER_DATA:
                target.setCipherData(CipherDataParser.getInstance().parse(xmlEventReader));
                break;
            case REFERENCE_LIST:
                StaxParserUtil.bypassElementBlock(xmlEventReader);
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}
