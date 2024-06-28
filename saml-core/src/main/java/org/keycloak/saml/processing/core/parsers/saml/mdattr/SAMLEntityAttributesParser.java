package org.keycloak.saml.processing.core.parsers.saml.mdattr;

import java.io.Serializable;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.keycloak.dom.saml.v2.mdattr.EntityAttributes;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAssertionParser;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAttributeParser;
import org.keycloak.saml.processing.core.parsers.saml.metadata.AbstractStaxSamlMetadataParser;
import org.keycloak.saml.processing.core.parsers.saml.metadata.SAMLMetadataQNames;

public class SAMLEntityAttributesParser extends AbstractStaxSamlMetadataParser<EntityAttributes> implements Serializable {
    private static final SAMLEntityAttributesParser INSTANCE = new SAMLEntityAttributesParser();

    private SAMLEntityAttributesParser() {
        super(SAMLMetadataQNames.ENTITY_ATTRIBUTES);
    }

    public static SAMLEntityAttributesParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected EntityAttributes instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        return new EntityAttributes();
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, EntityAttributes target, SAMLMetadataQNames element,
        StartElement elementDetail) throws ParsingException {
        switch (element) {
            case ATTRIBUTE:
                target.addAttribute(SAMLAttributeParser.getInstance().parse(xmlEventReader));
                break;
            case ASSERTION:
                target.addAssertion(SAMLAssertionParser.getInstance().parse(xmlEventReader));
                break;
            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}