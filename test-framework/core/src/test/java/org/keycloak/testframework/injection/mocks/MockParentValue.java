package org.keycloak.testframework.injection.mocks;

public class MockParentValue {

    private final String stringOption;
    private final boolean booleanOption;
    private boolean closed = false;

    public MockParentValue(String stringOption, boolean booleanOption) {
        this.stringOption = stringOption;
        this.booleanOption = booleanOption;
        MockInstances.INSTANCES.add(this);
    }

    public String getStringOption() {
        return stringOption;
    }

    public boolean isBooleanOption() {
        return booleanOption;
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
