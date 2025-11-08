package org.keycloak.tests.admin.model.workflow;

import java.time.Duration;

import org.keycloak.common.util.Time;
import org.keycloak.models.workflow.WorkflowProvider;
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

import org.openqa.selenium.WebDriver;

public abstract class AbstractWorkflowTest {

    protected static final String DEFAULT_REALM_NAME = "default";

    @InjectRunOnServer(permittedPackages = {"org.keycloak.tests", "org.hamcrest"}, realmRef = DEFAULT_REALM_NAME)
    RunOnServerClient runOnServer;

    @InjectRealm(lifecycle = LifeCycle.METHOD, ref = DEFAULT_REALM_NAME)
    ManagedRealm managedRealm;

    @InjectWebDriver
    WebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectOAuthClient(realmRef = DEFAULT_REALM_NAME)
    OAuthClient oauth;

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
