package org.keycloak.tests.admin.realm;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

public class AbstractRealmTest {

    @InjectRealm(ref = "managedRealm")
    ManagedRealm managedRealm;

    @InjectAdminClient(ref = "managed", realmRef = "managedRealm")
    Keycloak adminClient;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    @InjectRunOnServer(ref = "managed", realmRef = "managedRealm")
    RunOnServerClient runOnServer;

    @InjectAdminEvents(ref = "managedEvents", realmRef = "managedRealm")
    AdminEvents adminEvents;
}
