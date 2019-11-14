package org.keycloak.testsuite.arquillian.containers;

import org.jboss.arquillian.container.spi.event.StartContainer;
import org.jboss.arquillian.container.spi.event.StopContainer;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.After;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.arquillian.test.spi.event.suite.Before;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;
import org.keycloak.common.Profile;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.arquillian.TestContext;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.arquillian.annotation.DisableFeatures;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.EnableFeatures;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.arquillian.AuthServerTestEnricher.getManagementClient;
import static org.keycloak.testsuite.arquillian.AuthServerTestEnricher.isAuthServerRemote;

/**
 * @author mhajas
 */
public class KeycloakContainerFeaturesController {

    @Inject
    private Instance<TestContext> testContextInstance;
    @Inject
    private Instance<SuiteContext> suiteContextInstance;
    @Inject
    private Event<StartContainer> startContainerEvent;
    @Inject
    private Event<StopContainer> stopContainerEvent;

    public enum FeatureAction {
        ENABLE(KeycloakTestingClient::enableFeature),
        DISABLE(KeycloakTestingClient::disableFeature);

        private BiConsumer<KeycloakTestingClient, Profile.Feature> featureConsumer;

        FeatureAction(BiConsumer<KeycloakTestingClient, Profile.Feature> featureConsumer) {
            this.featureConsumer = featureConsumer;
        }

        public void accept(KeycloakTestingClient testingClient, Profile.Feature feature) {
            featureConsumer.accept(testingClient, feature);
        }
    }

    public enum State {
        BEFORE,
        AFTER
    }

    private class UpdateFeature {
        private Profile.Feature feature;
        private boolean skipRestart;
        private FeatureAction action;

        public UpdateFeature(Profile.Feature feature, boolean skipRestart, FeatureAction action) {
            this.feature = feature;
            this.skipRestart = skipRestart;
            this.action = action;
        }

        /**
         * All features we want to enable/disable must be disabled/enabled
         * otherwise at the end of a test the environment will be in an inconsistent state because we would disable/enable
         * some feature which was enabled/disabled before test
         *
         */
        private void assertValid() {
            assertThat("An annotation requested to " + action.name()
                            + " feature " + feature.name() + " however it was already in that state" ,
                    ProfileAssume.isFeatureEnabled(feature),
                    is(!(action == FeatureAction.ENABLE)));
        }

        private void assertPerformed() {
            assertThat("An annotation requested to " + action.name() +
                            " feature " + feature.name() + ", however after performing this operation " +
                            "the feature is not in desired state" ,
                    ProfileAssume.isFeatureEnabled(feature),
                    is(action == FeatureAction.ENABLE));
        }

        public void performAction() {
            assertValid();
            action.accept(testContextInstance.get().getTestingClient(), feature);
        }
    }

    public void restartAuthServer() throws Exception {
        if (isAuthServerRemote()) {
            try (OnlineManagementClient client = getManagementClient()) {
                int timeoutInSec = Integer.getInteger(System.getProperty("auth.server.jboss.startup.timeout"), 300);
                Administration administration = new Administration(client, timeoutInSec);
                administration.reload();
            }
        } else {
            stopContainerEvent.fire(new StopContainer(suiteContextInstance.get().getAuthServerInfo().getArquillianContainer()));
            startContainerEvent.fire(new StartContainer(suiteContextInstance.get().getAuthServerInfo().getArquillianContainer()));
        }
    }

    private void updateFeatures(List<UpdateFeature> updateFeatures) throws Exception {
        updateFeatures.forEach(UpdateFeature::performAction);

        if (updateFeatures.stream().anyMatch(updateFeature -> !updateFeature.skipRestart)) {
            restartAuthServer();
            testContextInstance.get().reconnectAdminClient();
        }

        updateFeatures.forEach(UpdateFeature::assertPerformed);
    }

    private void checkAnnotatedElementForFeatureAnnotations(AnnotatedElement annotatedElement, State state) throws Exception {
        List<UpdateFeature> updateFeatureList = new ArrayList<>(0);

        if (annotatedElement.isAnnotationPresent(EnableFeatures.class) || annotatedElement.isAnnotationPresent(EnableFeature.class)) {
            updateFeatureList.addAll(Arrays.stream(annotatedElement.getAnnotationsByType(EnableFeature.class))
                    .map(annotation -> new UpdateFeature(annotation.value(), annotation.skipRestart(),
                            state == State.BEFORE ? FeatureAction.ENABLE : FeatureAction.DISABLE))
                    .collect(Collectors.toList()));
        }

        if (annotatedElement.isAnnotationPresent(DisableFeatures.class) || annotatedElement.isAnnotationPresent(DisableFeature.class)) {
            updateFeatureList.addAll(Arrays.stream(annotatedElement.getAnnotationsByType(DisableFeature.class))
                    .map(annotation -> new UpdateFeature(annotation.value(), annotation.skipRestart(),
                            state == State.BEFORE ? FeatureAction.DISABLE : FeatureAction.ENABLE))
                    .collect(Collectors.toList()));
        }

        if (!updateFeatureList.isEmpty()) {
            updateFeatures(updateFeatureList);
        }
    }

    public void handleEnableFeaturesAnnotationBeforeClass(@Observes(precedence = 1) BeforeClass event) throws Exception {
        checkAnnotatedElementForFeatureAnnotations(event.getTestClass().getJavaClass(), State.BEFORE);
    }

    public void handleEnableFeaturesAnnotationBeforeTest(@Observes(precedence = 1) Before event) throws Exception {
        checkAnnotatedElementForFeatureAnnotations(event.getTestMethod(), State.BEFORE);
    }

    public void handleEnableFeaturesAnnotationAfterTest(@Observes(precedence = 2) After event) throws Exception {
        checkAnnotatedElementForFeatureAnnotations(event.getTestMethod(), State.AFTER);
    }

    public void handleEnableFeaturesAnnotationAfterClass(@Observes(precedence = 2) AfterClass event) throws Exception {
        checkAnnotatedElementForFeatureAnnotations(event.getTestClass().getJavaClass(), State.AFTER);
    }

}
