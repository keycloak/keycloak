package org.keycloak.saml.processing.core.parsers.saml.metadata;

import java.util.List;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.keycloak.dom.saml.v2.metadata.PDPDescriptorType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

/**
 * @author mhajas
 */
public class SAMLPDPDescriptorParser extends SAMLRoleDecriptorTypeParser<PDPDescriptorType> {

    private static final SAMLPDPDescriptorParser INSTANCE = new SAMLPDPDescriptorParser();

    public SAMLPDPDescriptorParser() {
        super(SAMLMetadataQNames.PDP_DESCRIPTOR);
    }

    public static SAMLPDPDescriptorParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected PDPDescriptorType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        List<String> protocolEnum = StaxParserUtil.getRequiredStringListAttributeValue(element, SAMLMetadataQNames.ATTR_PROTOCOL_SUPPORT_ENUMERATION);
        PDPDescriptorType descriptor = new PDPDescriptorType(protocolEnum);

        parseOptionalArguments(element, descriptor);

        return descriptor;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, PDPDescriptorType target, SAMLMetadataQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case AUTHZ_SERVICE:
            target.addAuthZService(SAMLAuthzServiceParser.getInstance().parse(xmlEventReader));
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
