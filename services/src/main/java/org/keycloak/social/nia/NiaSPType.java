package org.keycloak.social.nia;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;

public class NiaSPType implements SamlProtocolExtensionsAwareBuilder.NodeGenerator {

    public static final String PREFIX = "saml2p";
    public static final String URI = "urn:oasis:names:tc:SAML:2.0:protocol";
    public static final String NS_PREFIX = "eidas";
    public static final String SAML_EXTENSIONS = "http://eidas.europa.eu/saml-extensions";
    public static final String KEY_INFO_ELEMENT_NAME = "SPType";
    public static final String KEY_ID_ATTRIBUTE_NAME = "Name";
    public static final String NS_URI = "SPType";
    public static final NameIDType nameidtype = new NameIDType();

    private final String sptype;

    public NiaSPType(String sptype) {
        this.sptype = sptype;
    }

    @Override
    public void write(XMLStreamWriter writer) throws ProcessingException {
        NiaWriter niaWriter = new NiaWriter(writer);
        StaxUtil.writeNameSpace(writer, PREFIX, URI);
        StaxUtil.writeNameSpace(writer, NS_PREFIX, SAML_EXTENSIONS);
        niaWriter.writeSptype(nameidtype, new QName("G"), true);
        StaxUtil.writeStartElement(writer, NS_PREFIX, NS_URI, NS_URI);
        if (this.sptype != null) {
            StaxUtil.writeAttribute(writer, KEY_INFO_ELEMENT_NAME, this.sptype);
        }
        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

}
