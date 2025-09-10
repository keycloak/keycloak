package org.keycloak.tests.admin.model.policy;

import org.junit.jupiter.api.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.policy.AddRequiredActionProviderFactory;
import org.keycloak.models.policy.UserCreationTimeResourcePolicyProviderFactory;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.resources.policies.ResourcePolicyActionRepresentation;
import org.keycloak.representations.resources.policies.ResourcePolicyRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@KeycloakIntegrationTest(config = RLMServerConfig.class)
public class AddRequiredActionTest {

    private static final String REALM_NAME = "default";

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests")
    RunOnServerClient runOnServer;

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @Test
    public void testActionRun() {
        managedRealm.admin().resources().policies().create(ResourcePolicyRepresentation.create()
                .of(UserCreationTimeResourcePolicyProviderFactory.ID)
                .immediate()
                .withActions(
                        ResourcePolicyActionRepresentation.create()
                                .of(AddRequiredActionProviderFactory.ID)
                                .withConfig("action", "UPDATE_PASSWORD")
                                .build()
                ).build()).close();

        managedRealm.admin().users().create(UserConfigBuilder.create().username("test").build()).close();

        List< UserRepresentation> users = managedRealm.admin().users().search("test");
        assertThat(users, hasSize(1));
        UserRepresentation userRepresentation = users.get(0);
        assertThat(userRepresentation.getRequiredActions(), hasSize(1));
        assertThat(userRepresentation.getRequiredActions().get(0), is(UserModel.RequiredAction.UPDATE_PASSWORD.name()));
    }

    private static RealmModel configureSessionContext(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(REALM_NAME);
        session.getContext().setRealm(realm);
        return realm;
    }

}
