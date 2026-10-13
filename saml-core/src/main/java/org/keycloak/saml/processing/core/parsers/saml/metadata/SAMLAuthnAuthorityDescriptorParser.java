package org.keycloak.saml.processing.core.parsers.saml.metadata;

import java.util.List;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.keycloak.dom.saml.v2.metadata.AuthnAuthorityDescriptorType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

/**
 * @author mhajas
 */
public class SAMLAuthnAuthorityDescriptorParser extends SAMLRoleDecriptorTypeParser<AuthnAuthorityDescriptorType> {

    private static final SAMLAuthnAuthorityDescriptorParser INSTANCE = new SAMLAuthnAuthorityDescriptorParser();

    public SAMLAuthnAuthorityDescriptorParser() {
        super(SAMLMetadataQNames.AUTHN_AUTHORITY_DESCRIPTOR);
    }

    public static SAMLAuthnAuthorityDescriptorParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected AuthnAuthorityDescriptorType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        List<String> protocolEnum = StaxParserUtil.getRequiredStringListAttributeValue(element, SAMLMetadataQNames.ATTR_PROTOCOL_SUPPORT_ENUMERATION);
        AuthnAuthorityDescriptorType descriptor = new AuthnAuthorityDescriptorType(protocolEnum);

        parseOptionalArguments(element, descriptor);

        return descriptor;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, AuthnAuthorityDescriptorType target, SAMLMetadataQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
        case AUTHN_QUERY_SERVICE:
            target.addAuthnQueryService(SAMLAuthnQueryServiceParser.getInstance().parse(xmlEventReader));
            break;

        case ASSERTION_ID_REQUEST_SERVICE:
            target.addAssertionIDRequestService(SAMLAssertinIDRequestServiceParser.getInstance().parse(xmlEventReader));
            break;

        case NAMEID_FORMAT:
            StaxParserUtil.advance(xmlEventReader);
            target.addNameIDFormat(StaxParserUtil.getElementText(xmlEventReader));
            break;

        default:
            super.processSubElement(xmlEventReader, target, element, elementDetail);
        }
    }
}
