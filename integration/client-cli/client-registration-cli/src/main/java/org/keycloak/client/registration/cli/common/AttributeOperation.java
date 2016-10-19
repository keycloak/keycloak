package org.keycloak.client.registration.cli.common;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class AttributeOperation {

    private Type type;
    private AttributeKey key;
    private String value;

    public AttributeOperation(Type type, String key) {
        this(type, key, null);
    }

    public AttributeOperation(Type type, String key, String value) {
        if (type == Type.DELETE && value != null) {
            throw new IllegalArgumentException("When type is DELETE, value has to be null");
        }
        this.type = type;
        this.key = new AttributeKey(key);
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public AttributeKey getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }


    public enum Type {
        SET,
        DELETE
    }
}
