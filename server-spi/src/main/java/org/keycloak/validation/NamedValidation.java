package org.keycloak.validation;

/**
 * Decorates a {@link Validation} with a name.
 */
public class NamedValidation extends DelegatingValidation {

    private final String name;

    public NamedValidation(ValidationRegistration registration) {
        this(registration.getName(), registration.getValidation(), registration.getValidation()::isSupported);
    }

    public NamedValidation(String name, Validation delegate, ValidationSupported supported) {
        super(delegate, supported);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "name='" + name + '\'' +
                '}';
    }
}
