package org.keycloak.testsuite.console.page.sessions;

/**
 *
 * @author tkyjovsk
 */
public class Revocation extends Sessions {

    @Override
    public String getFragment() {
        return super.getFragment() + "/revocation";
    }

}
