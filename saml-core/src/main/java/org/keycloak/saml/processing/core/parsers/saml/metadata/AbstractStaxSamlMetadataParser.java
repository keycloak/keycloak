package org.keycloak.saml.processing.core.parsers.saml.metadata;

import javax.xml.namespace.QName;

import org.keycloak.saml.common.parsers.AbstractStaxParser;
import org.keycloak.saml.processing.core.parsers.util.QNameEnumLookup;

/**
 * @author mhajas
 */
abstract public class AbstractStaxSamlMetadataParser<T> extends AbstractStaxParser<T, SAMLMetadataQNames> {

    protected static final QNameEnumLookup<SAMLMetadataQNames> LOOKUP = new QNameEnumLookup(SAMLMetadataQNames.values());


    public AbstractStaxSamlMetadataParser(SAMLMetadataQNames expectedStartElement) {
        super(expectedStartElement.getQName(), SAMLMetadataQNames.UNKNOWN_ELEMENT);
    }

    @Override
    protected SAMLMetadataQNames getElementFromName(QName name) {
        return LOOKUP.from(name);
    }
}
