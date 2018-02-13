package org.keycloak.saml.processing.core.parsers.saml.metadata;

import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import static org.keycloak.saml.processing.core.parsers.saml.metadata.SAMLMetadataQNames.KEY_DESCRIPTOR;

/**
 * @author mhajas
 */
public class SAMLKeyDescriptorParser extends AbstractStaxSamlMetadataParser<KeyDescriptorType> {

    private static final SAMLKeyDescriptorParser INSTANCE = new SAMLKeyDescriptorParser();

    public SAMLKeyDescriptorParser() {
        super(KEY_DESCRIPTOR);
    }

    public static SAMLKeyDescriptorParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected KeyDescriptorType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        KeyDescriptorType keyDescriptor = new KeyDescriptorType();

        String use = StaxParserUtil.getAttributeValue(element, SAMLMetadataQNames.ATTR_USE);

        if (use != null && !use.isEmpty()) {
            keyDescriptor.setUse(KeyTypes.fromValue(use));
        }

        return keyDescriptor;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, KeyDescriptorType target, SAMLMetadataQNames element, StartElement elementDetail) throws ParsingException {
        switch(element) {
            case KEY_INFO:
                target.setKeyInfo(StaxParserUtil.getDOMElement(xmlEventReader));
                break;

            case ENCRYPTION_METHOD:
                target.addEncryptionMethod(SAMLEncryptionMethodParser.getInstance().parse(xmlEventReader));
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}
