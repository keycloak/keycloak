package org.keycloak.freemarker;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FreeMarkerException extends Exception {

    public FreeMarkerException(String message) {
        super(message);
    }

    public FreeMarkerException(String message, Throwable cause) {
        super(message, cause);
    }
}
