package org.keycloak.test.examples;

import org.keycloak.test.framework.annotations.InjectRealm;
import org.keycloak.test.framework.realm.ManagedRealm;

public abstract class AbstractTest {

    @InjectRealm
    ManagedRealm realm;

}
