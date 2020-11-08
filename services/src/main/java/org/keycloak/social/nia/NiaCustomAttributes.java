package org.keycloak.social.nia;

import javax.xml.stream.XMLStreamWriter;
import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;

public class NiaCustomAttributes implements SamlProtocolExtensionsAwareBuilder.NodeGenerator {

    public NiaCustomAttributes() {
    }

    @Override
    public void write(XMLStreamWriter writer) throws ProcessingException {
        StaxUtil.writeStartElement(writer, "eidas", "RequestedAttributes", "");
        NiaCustomAttribute nia = new NiaCustomAttribute("http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName", "true");
        nia.write(writer);
        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }
}
