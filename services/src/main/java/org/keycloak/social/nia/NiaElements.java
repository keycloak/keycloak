package org.keycloak.social.nia;

import javax.xml.stream.XMLStreamWriter;
import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;

public class NiaElements implements SamlProtocolExtensionsAwareBuilder.NodeGenerator {

    @Override
    public void write(XMLStreamWriter writer) throws ProcessingException {
        NiaWriter niaWriter = new NiaWriter(writer);
        niaWriter.writeStartingTag();
        StaxUtil.flush(writer);

    }

}
