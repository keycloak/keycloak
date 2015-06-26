package org.keycloak.testsuite.console.page;

import javax.ws.rs.core.UriBuilder;

/**
 *
 * @author tkyjovsk
 */
public class Realm extends Realms {

    public static final String MASTER = "master";

    public Realm() {
        setTemplateValue("realm", Realm.MASTER);
    }
    
    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder().path("{realm}");
    }
    
}
