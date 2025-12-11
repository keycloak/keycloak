package org.keycloak.saml.processing.core.parsers.saml.metadata;

import java.util.List;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.keycloak.dom.saml.v2.metadata.IDPSSODescriptorType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

import static org.keycloak.saml.processing.core.parsers.saml.metadata.SAMLMetadataQNames.IDP_SSO_DESCRIPTOR;

/**
 * @author mhajas
 */
public class SAMLIDPSSODescriptorParser extends SAMLSSODescriptorTypeParser<IDPSSODescriptorType> {

    private static final SAMLIDPSSODescriptorParser INSTANCE = new SAMLIDPSSODescriptorParser();

    private SAMLIDPSSODescriptorParser() {
        super(IDP_SSO_DESCRIPTOR);
    }

    public static SAMLIDPSSODescriptorParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected IDPSSODescriptorType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        List<String> protocolEnum = StaxParserUtil.getRequiredStringListAttributeValue(element, SAMLMetadataQNames.ATTR_PROTOCOL_SUPPORT_ENUMERATION);
        IDPSSODescriptorType descriptor = new IDPSSODescriptorType(protocolEnum);

        // Role descriptor optional arguments
        parseOptionalArguments(element, descriptor);

        // IDPSSODecsriptor optional attributes
        Boolean wantAuthnRequestsSigned = StaxParserUtil.getBooleanAttributeValue(element, SAMLMetadataQNames.ATTR_WANT_AUTHN_REQUESTS_SIGNED);
        if (wantAuthnRequestsSigned != null) {
            descriptor.setWantAuthnRequestsSigned(wantAuthnRequestsSigned);
        }

        return descriptor;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, IDPSSODescriptorType target, SAMLMetadataQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case SINGLE_SIGNON_SERVICE:
                target.addSingleSignOnService(SAMLSingleSignOnServiceParser.getInstance().parse(xmlEventReader));
                break;

            case NAMEID_MAPPING_SERVICE:
                target.addNameIDMappingService(SAMLNameIDMappingServiceParser.getInstance().parse(xmlEventReader));
                break;

            case ASSERTION_ID_REQUEST_SERVICE:
                target.addAssertionIDRequestService(SAMLAssertinIDRequestServiceParser.getInstance().parse(xmlEventReader));
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
