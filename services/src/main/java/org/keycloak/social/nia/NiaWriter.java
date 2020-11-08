package org.keycloak.social.nia;

import java.util.LinkedList;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.ExtensionsType;
import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder;
import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder.NodeGenerator;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.PROTOCOL_NSURI;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.processing.core.saml.v2.writers.BaseWriter;
import org.w3c.dom.Node;

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

    public void writeStartingTag() throws ProcessingException {
        NameIDType nameIDType = new NameIDType();
        nameIDType.setValue("KAREL");
        write(nameIDType, new QName("eidas:RequestedAttributes"), false);
        StaxUtil.flush(writer);

    }

    @Override
    public void write(ExtensionsType extensions) throws ProcessingException {
        if (extensions.getAny().isEmpty()) {
            return;
        }

        StaxUtil.writeStartElement(writer, PROTOCOL_PREFIX, JBossSAMLConstants.EXTENSIONS__PROTOCOL.get(), PROTOCOL_NSURI.get());

        for (Object o : extensions.getAny()) {
            if (o instanceof Node) {
                StaxUtil.writeDOMNode(writer, (Node) o);
            } else if (o instanceof SamlProtocolExtensionsAwareBuilder.NodeGenerator) {
                SamlProtocolExtensionsAwareBuilder.NodeGenerator ng = (SamlProtocolExtensionsAwareBuilder.NodeGenerator) o;
                ng.write(writer);
            } else {
                throw logger.samlExtensionUnknownChild(o == null ? null : o.getClass());
            }
        }

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }
}
