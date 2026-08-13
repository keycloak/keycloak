package org.keycloak.tests.workflow;

import java.time.Duration;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.common.util.Time;
import org.keycloak.models.workflow.WorkflowProvider;
import org.keycloak.representations.workflows.WorkflowRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServer;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public abstract class AbstractWorkflowTest {

    protected static final String DEFAULT_REALM_NAME = "default";

    @InjectRunOnServer(permittedPackages = {"org.keycloak.tests", "org.hamcrest"}, realmRef = DEFAULT_REALM_NAME)
    protected RunOnServerClient runOnServer;

    @InjectRealm(lifecycle = LifeCycle.METHOD, ref = DEFAULT_REALM_NAME)
    protected ManagedRealm managedRealm;

    @InjectWebDriver
    protected ManagedWebDriver driver;

    @InjectPage
    protected LoginPage loginPage;

    @InjectOAuthClient(realmRef = DEFAULT_REALM_NAME)
    protected OAuthClient oauth;

    protected void create(WorkflowRepresentation workflow) {
        try (Response response = managedRealm.admin().workflows().create(workflow)) {
            assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        }
    }

    protected void runScheduledSteps(Duration duration) {
        runOnServer.run((RunOnServer) session -> {
            WorkflowProvider provider = session.getProvider(WorkflowProvider.class);

            try {
                Time.setOffset(Math.toIntExact(duration.toSeconds()));
                provider.runScheduledSteps();
            } finally {
                Time.setOffset(0);
            }
        });
    }
}
