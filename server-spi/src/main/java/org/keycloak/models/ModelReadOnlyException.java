package org.keycloak.models;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ModelReadOnlyException extends ModelException {

    public ModelReadOnlyException() {
    }

    public ModelReadOnlyException(String message) {
        super(message);
    }

    public ModelReadOnlyException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModelReadOnlyException(Throwable cause) {
        super(cause);
    }
}
