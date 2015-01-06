
package org.keycloak.example.ws.types;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the org.apache.servicemix.samples.wsdl_first.types package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 *
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.apache.servicemix.samples.wsdl_first.types
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link GetPersonResponse }
     *
     */
    public GetPersonResponse createGetPersonResponse() {
        return new GetPersonResponse();
    }

    /**
     * Create an instance of {@link GetPerson }
     *
     */
    public GetPerson createGetPerson() {
        return new GetPerson();
    }

    /**
     * Create an instance of {@link UnknownPersonFault }
     *
     */
    public UnknownPersonFault createUnknownPersonFault() {
        return new UnknownPersonFault();
    }

}
