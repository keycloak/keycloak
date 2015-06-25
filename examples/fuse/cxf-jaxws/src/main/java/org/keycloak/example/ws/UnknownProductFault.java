package org.keycloak.example.ws;

import javax.xml.ws.WebFault;

@WebFault(name = "UnknownProductFault")
public class UnknownProductFault extends Exception {
    public static final long serialVersionUID = 20081110144906L;

    private org.keycloak.example.ws.types.UnknownProductFault unknownProductFault;

    public UnknownProductFault() {
        super();
    }

    public UnknownProductFault(String message) {
        super(message);
    }

    public UnknownProductFault(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownProductFault(String message, org.keycloak.example.ws.types.UnknownProductFault unknownProductFault) {
        super(message);
        this.unknownProductFault = unknownProductFault;
    }

    public UnknownProductFault(String message, org.keycloak.example.ws.types.UnknownProductFault unknownProductFault, Throwable cause) {
        super(message, cause);
        this.unknownProductFault = unknownProductFault;
    }

    public org.keycloak.example.ws.types.UnknownProductFault getFaultInfo() {
        return this.unknownProductFault;
    }
}
