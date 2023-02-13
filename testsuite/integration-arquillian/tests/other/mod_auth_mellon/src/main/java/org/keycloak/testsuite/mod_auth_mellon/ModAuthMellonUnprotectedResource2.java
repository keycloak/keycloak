package org.keycloak.testsuite.mod_auth_mellon;

import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author mhajas
 */
public class ModAuthMellonUnprotectedResource2 extends AbstractPageWithInjectedUrl {

    @Override
    public URL getInjectedUrl() {
        try {
            return new URL(System.getProperty("apache.mod_auth_mellon2.url", "https://app-saml-127-0-0-1.nip.io:8843/"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }
}
