package org.keycloak.tests.admin.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.ClientModel;
import org.keycloak.models.cache.infinispan.ClientAdapter;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServer;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServerWrapper;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import java.io.Serializable;

@KeycloakIntegrationTest
public class ClientByAttributesTest {

    @InjectRealm(config = ClientByAttributesRealm.class)
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectAdminClient
    Keycloak adminClient;

    @Test
    public void lookupByAttribute() {
        runOnServer.run(s -> {
            ClientModel c = s.clients().getClientByAttribute(s.getContext().getRealm(), "jwt.credential.sub", "value1");
            Assertions.assertEquals("client1", c.getClientId());
        });
    }

    @Test
    public void lookupByAttributeMultipleMatches() {
        runOnServer.run(s -> {
            try {
                s.clients().getClientByAttribute(s.getContext().getRealm(), "jwt.credential.sub", "value2");
                Assertions.fail("Expected exception");
            } catch (Exception e) {
                Assertions.assertEquals("Multiple clients found with the same attribute name and value", e.getMessage());
            }
        });
    }

    @Test
    public void lookupByAttributeTestCached() {
        CachedTimeStamp cachedTimeStamp = new CachedTimeStamp("value1");
        Long cachedTimeStamp1 = runOnServer.fetch(cachedTimeStamp);
        Assertions.assertEquals(cachedTimeStamp1, runOnServer.fetch(cachedTimeStamp));

        realm.admin().clearRealmCache();

        Assertions.assertNotEquals(cachedTimeStamp1, runOnServer.fetch(cachedTimeStamp));
    }

    public static class ClientByAttributesRealm implements RealmConfig {
        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.addClient("client1").attribute("jwt.credential.sub", "value1");
            realm.addClient("client2").attribute("jwt.credential.sub", "value2");
            realm.addClient("client3").attribute("jwt.credential.sub", "value2");
            return realm;
        }
    }

    private record CachedTimeStamp(String jwtCredentialSub) implements FetchOnServerWrapper<Long>, Serializable {

        @Override
        public FetchOnServer getRunOnServer() {
            return s -> {
                ClientModel client = s.clients().getClientByAttribute(s.getContext().getRealm(), "jwt.credential.sub", jwtCredentialSub);
                return ((ClientAdapter) client).getCacheTimestamp();
            };
        }

        @Override
        public Class<Long> getResultClass() {
            return Long.class;
        }
    }

}
