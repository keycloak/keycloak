package org.keycloak.testsuite.console.page.authentication;

/**
 *
 * @author tkyjovsk
 */
public class Flows extends Authentication {
    
    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/flows";
    }

}
