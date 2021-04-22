package org.keycloak.events;

public class EventPersistenceFailedException extends RuntimeException {

    private Object[] parameters;

    public EventPersistenceFailedException() {
    }

    public EventPersistenceFailedException(String message) {
        super(message);
    }

    public EventPersistenceFailedException(String message, Object ... parameters) {
        super(message);
        this.parameters = parameters;
    }

    public EventPersistenceFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public EventPersistenceFailedException(Throwable cause) {
        super(cause);
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }
}
