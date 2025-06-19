package org.keycloak.saml.processing.core.parsers.saml.xmlsec;

import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAssertionQNames;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.parsers.StaxParser;
import org.keycloak.saml.common.util.StaxParserUtil;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;
import java.nio.charset.StandardCharsets;

/**
 *
 */
public class CipherValueParser implements StaxParser {

    private static final CipherValueParser INSTANCE = new CipherValueParser();
    private static final QName NIL = new QName(JBossSAMLURIConstants.XSI_NSURI.get(), "nil", JBossSAMLURIConstants.XSI_PREFIX.get());
    private static final QName XSI_TYPE = new QName(JBossSAMLURIConstants.XSI_NSURI.get(), "type", JBossSAMLURIConstants.XSI_PREFIX.get());

    private static final ThreadLocal<XMLEventFactory> XML_EVENT_FACTORY = ThreadLocal.withInitial(XMLEventFactory::newInstance);

    public static CipherValueParser getInstance() {
        return INSTANCE;
    }

    @Override
    public byte[] parse(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement element = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(element, SAMLAssertionQNames.CIPHER_VALUE.getQName());

        return StaxParserUtil.getElementText(xmlEventReader).getBytes(StandardCharsets.UTF_8);
    }
}
