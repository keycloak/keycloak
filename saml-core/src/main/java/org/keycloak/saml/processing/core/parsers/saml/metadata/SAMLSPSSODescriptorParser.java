package org.keycloak.saml.processing.core.parsers.saml.metadata;

import java.util.List;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

import static org.keycloak.saml.processing.core.parsers.saml.metadata.SAMLMetadataQNames.SP_SSO_DESCRIPTOR;

/**
 * @author mhajas
 */
public class SAMLSPSSODescriptorParser extends SAMLSSODescriptorTypeParser<SPSSODescriptorType> {

    private static final SAMLSPSSODescriptorParser INSTANCE = new SAMLSPSSODescriptorParser();

    private SAMLSPSSODescriptorParser() {
        super(SP_SSO_DESCRIPTOR);
    }

    public static SAMLSPSSODescriptorParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected SPSSODescriptorType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        List<String> protocolEnum = StaxParserUtil.getRequiredStringListAttributeValue(element, SAMLMetadataQNames.ATTR_PROTOCOL_SUPPORT_ENUMERATION);
        SPSSODescriptorType descriptor = new SPSSODescriptorType(protocolEnum);

        // Role descriptor optional arguments
        parseOptionalArguments(element, descriptor);

        // SPSSODecsriptor optional attributes
        Boolean authnRequestsSigned = StaxParserUtil.getBooleanAttributeValue(element, SAMLMetadataQNames.ATTR_AUTHN_REQUESTS_SIGNED);
        if (authnRequestsSigned != null) {
            descriptor.setAuthnRequestsSigned(authnRequestsSigned);
        }

        Boolean wantAssertionSigned = StaxParserUtil.getBooleanAttributeValue(element, SAMLMetadataQNames.ATTR_WANT_ASSERTIONS_SIGNED);
        if (wantAssertionSigned != null) {
            descriptor.setWantAssertionsSigned(wantAssertionSigned);
        }

        return descriptor;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, SPSSODescriptorType target, SAMLMetadataQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case ASSERTION_CONSUMER_SERVICE:
                target.addAssertionConsumerService(SAMLAssertionConsumerServiceParser.getInstance().parse(xmlEventReader));
                break;

            case ATTRIBUTE_CONSUMING_SERVICE:
                target.addAttributeConsumerService(SAMLAttributeConsumingServiceParser.getInstance().parse(xmlEventReader));
                break;

            default:
                super.processSubElement(xmlEventReader, target, element, elementDetail);
        }
    }
}
