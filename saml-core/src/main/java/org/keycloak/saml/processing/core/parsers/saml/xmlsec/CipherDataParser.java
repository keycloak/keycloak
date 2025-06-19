package org.keycloak.saml.processing.core.parsers.saml.xmlsec;

import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAssertionQNames;
import org.keycloak.dom.xmlsec.w3.xmlenc.CipherDataType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.parsers.AbstractStaxParser;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.parsers.util.QNameEnumLookup;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

public class CipherDataParser extends AbstractStaxParser<CipherDataType, SAMLAssertionQNames> {
    protected static final QNameEnumLookup<SAMLAssertionQNames> LOOKUP = new QNameEnumLookup(SAMLAssertionQNames.values());

    private static final CipherDataParser INSTANCE = new CipherDataParser();

    public static CipherDataParser getInstance() {
        return INSTANCE;
    }

    public CipherDataParser() {
        super(SAMLAssertionQNames.CIPHER_DATA.getQName(), SAMLAssertionQNames.UNKNOWN_ELEMENT);
    }

    @Override
    protected SAMLAssertionQNames getElementFromName(QName name) {
        return LOOKUP.from(name);
    }

    @Override
    protected CipherDataType instantiateElement(XMLEventReader xmlEventReader, StartElement startElement) throws ParsingException {
        return new CipherDataType();
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, CipherDataType target, SAMLAssertionQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case CIPHER_VALUE:
                target.setCipherValue(CipherValueParser.getInstance().parse(xmlEventReader));
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}
