package org.keycloak.saml.processing.core.parsers.saml.mdrpi;

import static org.keycloak.saml.processing.core.parsers.saml.metadata.SAMLMetadataQNames.ATTR_LANG;

import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.keycloak.dom.saml.v2.mdrpi.RegistrationInfoType;
import org.keycloak.dom.saml.v2.metadata.LocalizedURIType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.parsers.saml.metadata.AbstractStaxSamlMetadataParser;
import org.keycloak.saml.processing.core.parsers.saml.metadata.SAMLMetadataQNames;

public class SAMLRegistrationInfoParser extends AbstractStaxSamlMetadataParser<RegistrationInfoType> {

    private static final SAMLRegistrationInfoParser INSTANCE = new SAMLRegistrationInfoParser();

    private SAMLRegistrationInfoParser() {
        super(SAMLMetadataQNames.REGISTRATION_INFO);
    }

    public static SAMLRegistrationInfoParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected RegistrationInfoType instantiateElement(XMLEventReader xmlEventReader, StartElement element)
        throws ParsingException {

        URI registrationAuthority = URI
            .create(StaxParserUtil.getRequiredAttributeValue(element, SAMLMetadataQNames.ATTR_REGISTRATION_AUTHORITY));
        RegistrationInfoType registrationInfo = new RegistrationInfoType(registrationAuthority);
        registrationInfo.setRegistrationInstant(
            StaxParserUtil.getXmlTimeAttributeValue(element, SAMLMetadataQNames.ATTR_REGISTRATION_INSTANT));
        return registrationInfo;

    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, RegistrationInfoType target, SAMLMetadataQNames element,
        StartElement elementDetail) throws ParsingException {
        switch (element) {
            case REGISTRATION_POLICY:
                LocalizedURIType registrationPolicy = new LocalizedURIType(
                    StaxParserUtil.getRequiredAttributeValue(elementDetail, ATTR_LANG));
                StaxParserUtil.advance(xmlEventReader);

                registrationPolicy.setValue(URI.create(StaxParserUtil.getElementText(xmlEventReader)));

                target.addRegistrationPolicy(registrationPolicy);
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}
