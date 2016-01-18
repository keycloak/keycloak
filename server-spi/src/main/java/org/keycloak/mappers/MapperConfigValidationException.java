package org.keycloak.mappers;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MapperConfigValidationException extends Exception {

    public MapperConfigValidationException(String message) {
        super(message);
    }

    public MapperConfigValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
