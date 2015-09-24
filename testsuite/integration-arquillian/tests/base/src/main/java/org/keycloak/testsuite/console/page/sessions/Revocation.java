package org.keycloak.testsuite.console.page.sessions;

/**
 *
 * @author tkyjovsk
 */
public class Revocation extends Sessions {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/revocation";
    }

}
