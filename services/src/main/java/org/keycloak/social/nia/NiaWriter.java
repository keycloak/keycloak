package org.keycloak.social.nia;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.processing.core.saml.v2.writers.BaseWriter;

public class NiaWriter extends BaseWriter {

    public static final String ATTRIBUTE_NAME = "public";
    public static final String ELEMENT = "eidas:SPType";

    public NiaWriter(XMLStreamWriter writer) {
        super(writer);
    }

    public void writeSptype(NameIDType nameIDType, QName tag, boolean writeNamespace) throws ProcessingException {
        nameIDType.setValue(ATTRIBUTE_NAME);
        write(nameIDType, new QName(ELEMENT), false);
        StaxUtil.flush(writer);
    }

}
