package org.keycloak.exceptions;

public class MetadataPolicyCombinationException extends Exception {

    private static final long serialVersionUID = 1L;

    public MetadataPolicyCombinationException(String message) {
        super(MetadataPolicyCombinationException.class.getSimpleName() + " : " + message);
    }

}
