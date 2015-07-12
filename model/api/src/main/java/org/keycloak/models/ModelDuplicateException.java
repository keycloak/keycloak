package org.keycloak.models;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ModelDuplicateException extends ModelException {

    private String duplicateFieldName;

    public ModelDuplicateException() {
    }

    public ModelDuplicateException(String message) {
        super(message);
    }

    public ModelDuplicateException(String message, String duplicateFieldName) {
        super(message);
        this.duplicateFieldName = duplicateFieldName;
    }

    public ModelDuplicateException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModelDuplicateException(Throwable cause) {
        super(cause);
    }

    public String getDuplicateFieldName() {
        return duplicateFieldName;
    }
}
