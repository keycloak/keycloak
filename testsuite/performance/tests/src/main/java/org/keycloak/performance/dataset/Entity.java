package org.keycloak.performance.dataset;

import org.apache.commons.lang.Validate;

/**
 *
 * @author tkyjovsk
 * @param <REP> representation type
 */
public abstract class Entity<REP> implements Representable<REP> {

    private REP representation;

    public Entity() {
        setRepresentation(newRepresentation());
    }

    @Override
    public REP getRepresentation() {
        return representation;
    }

    @Override
    public final void setRepresentation(REP representation) {
        Validate.notNull(representation);
        this.representation = representation;
    }

    public String simpleClassName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int hashCode() {
        return simpleClassName().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other != null
                && this.getClass() == other.getClass()
                && this.hashCode() == ((Entity) other).hashCode());
    }

}
