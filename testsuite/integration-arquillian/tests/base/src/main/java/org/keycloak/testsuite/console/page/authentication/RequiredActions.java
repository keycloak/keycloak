package org.keycloak.testsuite.console.page.authentication;

/**
 *
 * @author tkyjovsk
 */
public class RequiredActions extends Authentication {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/required-actions";
    }

}
