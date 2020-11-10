package org.keycloak.social.nia;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;

public class NiaSPType implements SamlProtocolExtensionsAwareBuilder.NodeGenerator {

    public static final String PREFIX = "samlp";
    public static final String URI = "urn:oasis:names:tc:SAML:2.0:protocol";
    public static final String NS_PREFIX = "eidas";
    public static final String SAML_EXTENSIONS = "http://eidas.europa.eu/saml-extensions";
    public static final NameIDType nameidtype = new NameIDType();

    public static final String ELEMENT = "eidas:SPType";
    public static final String ATTRIBUTE_NAME = "public";

    public NiaSPType() {
    }

    @Override
    public void write(XMLStreamWriter writer) throws ProcessingException {
        // StaxUtil.flush(writer);
        StaxUtil.writeNameSpace(writer, NS_PREFIX, SAML_EXTENSIONS);

        //StaxUtil.writeNameSpace(writer, PREFIX, URI);
        NiaWriter niaWriter = new NiaWriter(writer);
        nameidtype.setValue(ATTRIBUTE_NAME);
        niaWriter.writeSptype(nameidtype, new QName(ELEMENT));
        StaxUtil.flush(writer);
    }

}
