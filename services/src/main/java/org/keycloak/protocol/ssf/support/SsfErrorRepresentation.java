package org.keycloak.protocol.ssf.support;

import org.keycloak.representations.idm.OAuth2ErrorRepresentation;

public class SsfErrorRepresentation extends OAuth2ErrorRepresentation {

    public SsfErrorRepresentation() {}

    public SsfErrorRepresentation(String error, String errorDescription) {
        super(error, errorDescription);
    }
}
