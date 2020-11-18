package org.keycloak.broker.nia;

import javax.xml.stream.XMLStreamWriter;
import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;

public class NiaCustomAttribute implements SamlProtocolExtensionsAwareBuilder.NodeGenerator {

    public static final String NS_PREFIX = "eidas";
    public static final String KEY_INFO_ELEMENT_NAME = "RequestedAttribute";
    public static final String KEY_ID_ATTRIBUTE_NAME = "Name";
    public static final String NAME_FORMAT = "NameFormat";
    public static final String NS_URI = "urn:oasis:names:tc:SAML:2.0:attrname-format:uri";
    public static final String KEY_REQUIRED = "isRequired";

    private final String keyId;
    private final String required;

    public NiaCustomAttribute(String keyId, String required) {
        this.keyId = keyId;
        this.required = required;
    }

    @Override
    public void write(XMLStreamWriter writer) throws ProcessingException {

        StaxUtil.writeStartElement(writer, NS_PREFIX, KEY_INFO_ELEMENT_NAME, NS_URI);
        StaxUtil.writeAttribute(writer, NAME_FORMAT, NS_URI);
        if (this.keyId != null) {
            StaxUtil.writeAttribute(writer, KEY_ID_ATTRIBUTE_NAME, this.keyId);
        }
        if (this.required != null) {
            StaxUtil.writeAttribute(writer, KEY_REQUIRED, this.required);
        }
        StaxUtil.writeEndElement(writer);

        StaxUtil.flush(writer);
    }

}
