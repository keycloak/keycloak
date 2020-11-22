package org.keycloak.social.nia;

import javax.xml.stream.XMLStreamWriter;
import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;

public class NiaCustomAttributes implements SamlProtocolExtensionsAwareBuilder.NodeGenerator {

    public static final String ELEMENT = "eidas";
    public static final String REQUESTED = "RequestedAttributes";
    public static final String TRUE = "true";

    public NiaCustomAttributes() {
    }

    @Override
    public void write(XMLStreamWriter writer) throws ProcessingException {
        StaxUtil.writeStartElement(writer, ELEMENT, REQUESTED, "");
        NiaCustomAttribute nia = new NiaCustomAttribute("http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName", TRUE);
        nia.write(writer);
        nia = new NiaCustomAttribute("http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName", TRUE);
        nia.write(writer);
        nia = new NiaCustomAttribute("http://eidas.europa.eu/attributes/naturalperson/DateOfBirth", TRUE);
        nia.write(writer);
        nia = new NiaCustomAttribute("http://eidas.europa.eu/attributes/naturalperson/PlaceOfBirth", TRUE);
        nia.write(writer);
        nia = new NiaCustomAttribute("http://eidas.europa.eu/attributes/naturalperson/CurrentAddress", TRUE);
        nia.write(writer);
        nia = new NiaCustomAttribute("http://www.stork.gov.eu/1.0/eMail", TRUE);
        nia.write(writer);
        nia = new NiaCustomAttribute("http://www.stork.gov.eu/1.0/age", TRUE);
        nia.write(writer);
//        nia = new NiaCustomAttribute("http://schemas.eidentity.cz/moris/2016/identity/claims/phonenumber", TRUE);
//        nia.write(writer);
        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }
}
