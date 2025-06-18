package org.keycloak.testframework.injection.mocks;

public class MockChildValue {

    private final MockParentValue parent;

    public MockChildValue(MockParentValue parent) {
        MockInstances.INSTANCES.add(this);
        this.parent = parent;
    }

    public MockParentValue getParent() {
        return parent;
    }

    public void close() {
        if (parent.isClosed()) {
            throw new RuntimeException("Parent is closed!");
        }

        boolean removed = MockInstances.INSTANCES.remove(this);
        if (!removed) {
            throw new RuntimeException("Instance already removed");
        } else {
            MockInstances.CLOSED_INSTANCES.add(this);
        }
    }

}
