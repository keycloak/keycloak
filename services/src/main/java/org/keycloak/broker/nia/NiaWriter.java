package org.keycloak.broker.nia;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.core.saml.v2.writers.BaseWriter;

public class NiaWriter extends BaseWriter {

    public NiaWriter(XMLStreamWriter writer) {
        super(writer);
    }

    public void writeSptype(NameIDType nameIDType, QName tag) throws ProcessingException {
        write(nameIDType, tag);
    }

}
