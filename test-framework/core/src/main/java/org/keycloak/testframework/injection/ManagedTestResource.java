package org.keycloak.testframework.injection;

public abstract class ManagedTestResource {

    private boolean dirty = false;

    public abstract void runCleanup();

    boolean isDirty() {
        return dirty;
    }

    /**
     * Marking the resource as dirty will result in the test framework re-creating the resource after the test
     * has executed
     */
    public void dirty() {
        this.dirty = true;
    }

}
