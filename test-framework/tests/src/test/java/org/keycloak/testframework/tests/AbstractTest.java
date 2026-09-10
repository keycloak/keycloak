package org.keycloak.testframework.tests;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.realm.ManagedRealm;

public abstract class AbstractTest {

    @InjectRealm
    ManagedRealm realm;

}
