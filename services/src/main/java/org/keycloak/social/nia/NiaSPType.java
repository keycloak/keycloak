package org.keycloak.social.nia;

import javax.xml.stream.XMLStreamWriter;
import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;

public class NiaSPType implements SamlProtocolExtensionsAwareBuilder.NodeGenerator {

    public static final String NS_PREFIX = "eidas";
    public static final String KEY_INFO_ELEMENT_NAME = "SPType";
    public static final String KEY_ID_ATTRIBUTE_NAME = "Name";
    public static final String NS_URI = "Luba";
    public static final String KEY_REQUIRED = "Karel";

    private final String sptype;

    public NiaSPType(String sptype) {
        this.sptype = sptype;
    }

    @Override
    public void write(XMLStreamWriter writer) throws ProcessingException {
        StaxUtil.writeStartElement(writer, NS_PREFIX, KEY_INFO_ELEMENT_NAME, NS_URI);
        StaxUtil.writeNameSpace(writer, NS_PREFIX, NS_URI);
        if (this.sptype != null) {
            StaxUtil.writeAttribute(writer, KEY_ID_ATTRIBUTE_NAME, this.sptype);
        }
        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

}
