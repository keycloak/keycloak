package org.keycloak.saml.processing.core.parsers.saml.metadata;

import org.keycloak.dom.saml.v2.metadata.AttributeAuthorityDescriptorType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;
import java.util.List;

/**
 * @author mhajas
 */
public class SAMLAttributeAuthorityDescriptorParser extends SAMLRoleDecriptorTypeParser<AttributeAuthorityDescriptorType> {

    private static final SAMLAttributeAuthorityDescriptorParser INSTANCE = new SAMLAttributeAuthorityDescriptorParser();

    public SAMLAttributeAuthorityDescriptorParser() {
        super(SAMLMetadataQNames.ATTRIBUTE_AUTHORITY_DESCRIPTOR);
    }

    public static SAMLAttributeAuthorityDescriptorParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected AttributeAuthorityDescriptorType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        List<String> protocolEnum = StaxParserUtil.getRequiredStringListAttributeValue(element, SAMLMetadataQNames.ATTR_PROTOCOL_SUPPORT_ENUMERATION);
        AttributeAuthorityDescriptorType descriptor = new AttributeAuthorityDescriptorType(protocolEnum);

        parseOptionalArguments(element, descriptor);

        return descriptor;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, AttributeAuthorityDescriptorType target, SAMLMetadataQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case ATTRIBUTE_SERVICE:
            target.addAttributeService(SAMLAttributeServiceParser.getInstance().parse(xmlEventReader));
            break;

        case ASSERTION_ID_REQUEST_SERVICE:
            target.addAssertionIDRequestService(SAMLAssertinIDRequestServiceParser.getInstance().parse(xmlEventReader));
            break;

        case NAMEID_FORMAT:
            StaxParserUtil.advance(xmlEventReader);
            target.addNameIDFormat(StaxParserUtil.getElementText(xmlEventReader));
            break;

        case ATTRIBUTE_PROFILE:
            StaxParserUtil.advance(xmlEventReader);
            target.addAttributeProfile(StaxParserUtil.getElementText(xmlEventReader));
            break;

        case ATTRIBUTE:
            target.addAttribute(SAMLAttributeParser.getInstance().parse(xmlEventReader));
            break;

        default:
            super.processSubElement(xmlEventReader, target, element, elementDetail);
        }
    }
}
