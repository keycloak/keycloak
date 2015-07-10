package org.keycloak.testsuite.page.console;

import javax.ws.rs.core.UriBuilder;

/**
 *
 * @author tkyjovsk
 */
public class Realm extends RealmsRoot {

    public static final String REALM = "realm";
    public static final String MASTER = "master";

    public Realm() {
        setTemplateValue(REALM, MASTER);
    }

    @Override
    public RealmsRoot setTemplateValues(String realm) {
        setTemplateValues(REALM, realm);
        return this;
    }

    public RealmsRoot setTemplateValues(String consoleRealm, String realm) {
        setTemplateValue(CONSOLE_REALM, consoleRealm);
        setTemplateValue(REALM, realm);
        return this;
    }

    @Override
    public UriBuilder createUriBuilder() {
        // Note; The fragment part of URI isn't appendable, like the path is. Need to set the whole fragment.
        return super.createUriBuilder().fragment("/realms/{" + REALM + "}");
    }

}
