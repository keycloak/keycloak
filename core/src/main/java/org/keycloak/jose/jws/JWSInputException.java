package org.keycloak.jose.jws;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class JWSInputException extends Exception {

    public JWSInputException(String s) {
        super(s);
    }

    public JWSInputException() {
    }

    public JWSInputException(Throwable throwable) {
        super(throwable);
    }
}
