package org.keycloak.saml.processing.core.parsers.saml.metadata;

import java.net.URI;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

/**
 * @author mhajas
 */
public abstract class SAMLEndpointTypeParser extends AbstractStaxSamlMetadataParser<EndpointType> {

    public SAMLEndpointTypeParser(SAMLMetadataQNames expectedStartElement) {
        super(expectedStartElement);
    }

    @Override
    protected EndpointType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        String binding = StaxParserUtil.getRequiredAttributeValue(element, SAMLMetadataQNames.ATTR_BINDING);
        String location = StaxParserUtil.getRequiredAttributeValue(element, SAMLMetadataQNames.ATTR_LOCATION);

        EndpointType endpoint = new EndpointType(URI.create(binding), URI.create(location));

        String responseLocation = StaxParserUtil.getAttributeValue(element, SAMLMetadataQNames.ATTR_RESPONSE_LOCATION);

        if (responseLocation != null) {
            endpoint.setResponseLocation(URI.create(responseLocation));
        }

        return endpoint;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, EndpointType target, SAMLMetadataQNames element, StartElement elementDetail) throws ParsingException {
        throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
    }
}
