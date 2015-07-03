package org.keycloak.testsuite;

/**
 *
 * @author tkyjovsk
 */
public interface TestRealmsManager {
    
    public abstract void importTestRealm(String testRealm);
    public abstract void removeTestRealm(String testRealm);
    
}
