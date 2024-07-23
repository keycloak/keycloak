package org.keycloak.test.framework.injection.mocks;

public class MockParentValue {

    private boolean closed = false;

    public MockParentValue() {
        MockInstances.INSTANCES.add(this);
    }

    public boolean isClosed() {
        return closed;
    }

    public void close() {
        boolean removed = MockInstances.INSTANCES.remove(this);
        if (!removed) {
            throw new RuntimeException("Instance already removed");
        } else {
            MockInstances.CLOSED_INSTANCES.add(this);
        }
        closed = true;
    }

}
