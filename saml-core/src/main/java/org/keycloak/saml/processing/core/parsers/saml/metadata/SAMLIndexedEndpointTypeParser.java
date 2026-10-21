package org.keycloak.saml.processing.core.parsers.saml.metadata;

import java.net.URI;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.keycloak.dom.saml.v2.metadata.IndexedEndpointType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

/**
 * @author mhajas
 */
public abstract class SAMLIndexedEndpointTypeParser extends AbstractStaxSamlMetadataParser<IndexedEndpointType> {

    public SAMLIndexedEndpointTypeParser(SAMLMetadataQNames expectedStartElement) {
        super(expectedStartElement);
    }

    @Override
    protected IndexedEndpointType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        String binding = StaxParserUtil.getRequiredAttributeValue(element, SAMLMetadataQNames.ATTR_BINDING);
        String location = StaxParserUtil.getRequiredAttributeValue(element, SAMLMetadataQNames.ATTR_LOCATION);

        IndexedEndpointType endpoint = new IndexedEndpointType(URI.create(binding), URI.create(location));

        Boolean isDefault = StaxParserUtil.getBooleanAttributeValue(element, SAMLMetadataQNames.ATTR_IS_DEFAULT);
        if (isDefault != null) {
            endpoint.setIsDefault(isDefault);
        }
        
        Integer index = StaxParserUtil.getIntegerAttributeValue(element, SAMLMetadataQNames.ATTR_INDEX);
        if (index != null)
            endpoint.setIndex(index);

        // EndpointType attributes
        String responseLocation = StaxParserUtil.getAttributeValue(element, SAMLMetadataQNames.ATTR_RESPONSE_LOCATION);

        if (responseLocation != null) {
            endpoint.setResponseLocation(URI.create(responseLocation));
        }

        return endpoint;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, IndexedEndpointType target, SAMLMetadataQNames element, StartElement elementDetail) throws ParsingException {
        throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
    }
}
