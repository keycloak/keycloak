package org.keycloak.example.ws;

import javax.xml.ws.WebFault;

@WebFault(name = "UnknownPersonFault")
public class UnknownPersonFault extends Exception {
    public static final long serialVersionUID = 20081110144906L;

    private org.keycloak.example.ws.types.UnknownPersonFault unknownPersonFault;

    public UnknownPersonFault() {
        super();
    }

    public UnknownPersonFault(String message) {
        super(message);
    }

    public UnknownPersonFault(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownPersonFault(String message, org.keycloak.example.ws.types.UnknownPersonFault unknownPersonFault) {
        super(message);
        this.unknownPersonFault = unknownPersonFault;
    }

    public UnknownPersonFault(String message, org.keycloak.example.ws.types.UnknownPersonFault unknownPersonFault, Throwable cause) {
        super(message, cause);
        this.unknownPersonFault = unknownPersonFault;
    }

    public org.keycloak.example.ws.types.UnknownPersonFault getFaultInfo() {
        return this.unknownPersonFault;
    }
}
