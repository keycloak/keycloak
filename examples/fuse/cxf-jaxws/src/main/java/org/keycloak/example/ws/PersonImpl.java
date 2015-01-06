package org.keycloak.example.ws;

import javax.jws.WebService;
import javax.xml.ws.Holder;

@WebService(serviceName = "PersonService", endpointInterface = "org.keycloak.example.ws.Person")
public class PersonImpl implements Person {

    public void getPerson(Holder<String> personId, Holder<String> ssn, Holder<String> name)
        throws UnknownPersonFault
    {
        if (personId.value == null || personId.value.length() == 0) {
            org.keycloak.example.ws.types.UnknownPersonFault fault = new org.keycloak.example.ws.types.UnknownPersonFault();
            fault.setPersonId(personId.value);
            throw new UnknownPersonFault(null,fault);
        } else {
            name.value = "John Doe";
            ssn.value = "123-456-7890";
        }
    }

}
