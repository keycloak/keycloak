package org.keycloak.client.registration.cli.util;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class AttributeException extends RuntimeException {

    private final String attrName;

    public AttributeException(String attrName, String message) {
        super(message);
        this.attrName = attrName;
    }

    public AttributeException(String attrName, String message, Throwable th) {
        super(message, th);
        this.attrName = attrName;
    }

    public String getAttributeName() {
        return attrName;
    }
}
