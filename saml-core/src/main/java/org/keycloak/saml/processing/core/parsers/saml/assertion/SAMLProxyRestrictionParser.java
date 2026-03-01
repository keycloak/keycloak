package org.keycloak.saml.processing.core.parsers.saml.assertion;

import java.math.BigInteger;
import java.net.URI;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.keycloak.dom.saml.v2.assertion.ProxyRestrictionType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

/**
 * Parse the <ProxyRestriction Count=\"\"> tag
 *
 * @author Patric Vormstein
 * @since 21.03.2018
 */
public class SAMLProxyRestrictionParser extends AbstractStaxSamlAssertionParser<ProxyRestrictionType> {

    private static final SAMLProxyRestrictionParser INSTANCE = new SAMLProxyRestrictionParser();

    public SAMLProxyRestrictionParser() {
        super(SAMLAssertionQNames.PROXY_RESTRICTION);
    }

    public static SAMLProxyRestrictionParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected ProxyRestrictionType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        ProxyRestrictionType proxyRestriction = new ProxyRestrictionType();
        Integer count = StaxParserUtil.getIntegerAttributeValue(element, SAMLAssertionQNames.ATTR_COUNT);

        if (count != null) {
            proxyRestriction.setCount(BigInteger.valueOf(count));
        }

        return proxyRestriction;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, ProxyRestrictionType target, SAMLAssertionQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case AUDIENCE:
                StaxParserUtil.advance(xmlEventReader);
                String audienceValue = StaxParserUtil.getElementText(xmlEventReader);
                target.addAudience(URI.create(audienceValue));
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}
