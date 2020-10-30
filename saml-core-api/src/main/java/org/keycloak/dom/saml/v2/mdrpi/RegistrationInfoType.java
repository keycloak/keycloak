package org.keycloak.dom.saml.v2.mdrpi;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.keycloak.dom.saml.v2.metadata.LocalizedURIType;

public class RegistrationInfoType implements Serializable {

    protected URI registrationAuthority;
    protected XMLGregorianCalendar registrationInstant;
    protected List<LocalizedURIType> registrationPolicy = new ArrayList<>();

    public RegistrationInfoType (  URI registrationAuthority) {
        this.registrationAuthority = registrationAuthority;
    }

    public URI getRegistrationAuthority() {
        return registrationAuthority;
    }

    public void setRegistrationAuthority(URI registrationAuthority) {
        this.registrationAuthority = registrationAuthority;
    }

    public XMLGregorianCalendar getRegistrationInstant() {
        return registrationInstant;
    }

    public void setRegistrationInstant(XMLGregorianCalendar registrationInstant) {
        this.registrationInstant = registrationInstant;
    }

    public void addRegistrationPolicy(LocalizedURIType uri) {
        this.registrationPolicy.add(uri);
    }

    public List<LocalizedURIType> getRegistrationPolicy() {
        return registrationPolicy;
    }

}
