package org.keycloak.testframework.injection;

public abstract class ManagedTestResource {

    private boolean dirty = false;

    public abstract void runCleanup();

    boolean isDirty() {
        return dirty;
    }

    public void dirty() {
        this.dirty = true;
    }

}
