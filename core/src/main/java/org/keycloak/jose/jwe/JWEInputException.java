package org.keycloak.jose.jwe;

public class JWEInputException extends Exception {

    public JWEInputException(String s) {
        super(s);
    }

    public JWEInputException() {
    }

    public JWEInputException(Throwable throwable) {
        super(throwable);
    }
}
