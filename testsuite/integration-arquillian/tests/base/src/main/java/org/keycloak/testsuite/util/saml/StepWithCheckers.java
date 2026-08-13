package org.keycloak.testsuite.util.saml;

public interface StepWithCheckers {
    default Runnable getBeforeStepChecker() {
        return null;
    }
    default Runnable getAfterStepChecker() {
        return null;
    }
}
