package org.keycloak.testsuite.arquillian.containers;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.keycloak.common.Profile;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.arquillian.TestContext;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.arquillian.annotation.DisableFeatures;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.EnableFeatures;
import org.keycloak.testsuite.arquillian.annotation.SetDefaultProvider;
import org.keycloak.testsuite.client.KeycloakTestingClient;
import org.keycloak.testsuite.util.SpiProvidersSwitchingUtils;

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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
        ENABLE_AND_RESET((c, f) -> {
            c.enableFeature(f);
            // Without reset, feature will be persisted resulting e.g. in versioned features being disabled which is an invalid operation.
            // At the same time we can't just reset the feature as we don't know in the server context whether the feature should be enabled
            // or disabled after the reset.
            c.resetFeature(f);
        }),
        DISABLE(KeycloakTestingClient::disableFeature),
        DISABLE_AND_RESET((c, f) -> {
            c.disableFeature(f);
            c.resetFeature(f);
        });

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
        private final AnnotatedElement annotatedElement;

        public UpdateFeature(Profile.Feature feature, boolean skipRestart, FeatureAction action, AnnotatedElement annotatedElement) {
            this.feature = feature;
            this.skipRestart = skipRestart;
            this.action = action;
            this.annotatedElement = annotatedElement;
        }

        private void assertPerformed() {
            assertThat("An annotation requested to " + action.name() +
                            " feature " + feature.getKey() + ", however after performing this operation " +
                            "the feature is not in desired state",
                    ProfileAssume.isFeatureEnabled(feature),
                    is(action == FeatureAction.ENABLE || action == FeatureAction.ENABLE_AND_RESET));
        }

        public void performAction() {
            if ((action == FeatureAction.ENABLE && !ProfileAssume.isFeatureEnabled(feature))
                    || (action == FeatureAction.DISABLE && ProfileAssume.isFeatureEnabled(feature))
                    || action == FeatureAction.ENABLE_AND_RESET || action == FeatureAction.DISABLE_AND_RESET) {
                action.accept(testContextInstance.get().getTestingClient(), feature);
                SetDefaultProvider setDefaultProvider = annotatedElement.getAnnotation(SetDefaultProvider.class);
                if (setDefaultProvider != null) {
                    try {
                        if (action == FeatureAction.ENABLE || action == FeatureAction.ENABLE_AND_RESET) {
                            SpiProvidersSwitchingUtils.addProviderDefaultValue(suiteContextInstance.get(), setDefaultProvider);
                        } else {
                            SpiProvidersSwitchingUtils.removeProvider(suiteContextInstance.get(), setDefaultProvider);
                        }
                    } catch (Exception cause) {
                        throw new RuntimeException("Failed to (un)set default provider", cause);
                    }
                }
            }
        }

        public Profile.Feature getFeature() {
            return feature;
        }

        public boolean isSkipRestart() {
            return skipRestart;
        }

        public FeatureAction getAction() {
            return action;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UpdateFeature that = (UpdateFeature) o;
            return feature == that.feature;
        }

        @Override
        public int hashCode() {
            return Objects.hash(feature);
        }
    }

    public void restartAuthServer() {
        stopContainerEvent.fire(new StopContainer(suiteContextInstance.get().getAuthServerInfo().getArquillianContainer()));
        startContainerEvent.fire(new StartContainer(suiteContextInstance.get().getAuthServerInfo().getArquillianContainer()));
    }

    private void updateFeatures(Set<UpdateFeature> updateFeatures) throws Exception {
        updateFeatures = updateFeatures.stream()
                .collect(Collectors.toSet());

        updateFeatures.forEach(UpdateFeature::performAction);

        if (updateFeatures.stream().anyMatch(updateFeature -> !updateFeature.skipRestart)) {
            restartAuthServer();
            testContextInstance.get().reconnectAdminClient();
        }

        updateFeatures.forEach(UpdateFeature::assertPerformed);
    }

    private void checkAnnotatedElementForFeatureAnnotations(AnnotatedElement annotatedElement, State state) throws Exception {
        Set<UpdateFeature> updateFeatureSet = new HashSet<>();

        updateFeatureSet.addAll(getUpdateFeaturesSet(annotatedElement, state));

        // we can't rely on @Inherited annotations as it stops "searching" when it finds the first occurrence of given
        // annotation, i.e. annotation from the most specific test class
        if (annotatedElement instanceof Class) {
            Class<?> clazz = ((Class<?>) annotatedElement).getSuperclass();
            while (clazz != null) {
                // duplicates (i.e. annotations from less specific test classes) won't be added
                updateFeatureSet.addAll(getUpdateFeaturesSet(clazz, state));
                clazz = clazz.getSuperclass();
            }
        }

        if (!updateFeatureSet.isEmpty()) {
            updateFeatures(updateFeatureSet);
        }
    }

    private UpdateFeature getUpdateFeature(State state, boolean enableFeature, boolean skipRestart, AnnotatedElement annotatedElement, Profile.Feature feature) {

        if (state.equals(State.BEFORE)) {
            return new UpdateFeature(feature, skipRestart, enableFeature ? FeatureAction.ENABLE : FeatureAction.DISABLE, annotatedElement);
        }

        //in case of method, checks if there is a feature annotation on the class to set the correct state for the feature
        if (annotatedElement instanceof Method) {
            Class<?> clazz = ((Method) annotatedElement).getDeclaringClass();
            while (clazz != null) {
                if(Arrays.stream(clazz.getAnnotationsByType(EnableFeature.class)).anyMatch(a -> a.value() == feature)) {
                    return new UpdateFeature(feature, skipRestart, FeatureAction.ENABLE_AND_RESET, annotatedElement);
                } else if(Arrays.stream(clazz.getAnnotationsByType(DisableFeature.class)).anyMatch(a -> a.value() == feature)) {
                    return new UpdateFeature(feature, skipRestart, FeatureAction.DISABLE_AND_RESET, annotatedElement);
                }
                clazz = clazz.getSuperclass();
            }
        }

        //class element or no feature annotation found for the method, using state from profile
        Profile activeProfile = Optional.ofNullable(Profile.getInstance()).orElse(Profile.defaults());
        return new UpdateFeature(feature, skipRestart, activeProfile.getDisabledFeatures().contains(feature) ? FeatureAction.DISABLE_AND_RESET : FeatureAction.ENABLE_AND_RESET, annotatedElement);
    }

    private Set<UpdateFeature> getUpdateFeaturesSet(AnnotatedElement annotatedElement, State state) {
        Set<UpdateFeature> ret = new HashSet<>();

        ret.addAll(Arrays.stream(annotatedElement.getAnnotationsByType(EnableFeature.class))
                .map(annotation -> getUpdateFeature(state, true, annotation.skipRestart(), annotatedElement, annotation.value()))
                .collect(Collectors.toSet()));

        ret.addAll(Arrays.stream(annotatedElement.getAnnotationsByType(DisableFeature.class))
                .map(annotation -> getUpdateFeature(state, false, annotation.skipRestart(), annotatedElement, annotation.value()))
                .collect(Collectors.toSet()));

        return ret;
    }

    private boolean isEnableFeature(AnnotatedElement annotatedElement) {
        return (annotatedElement.isAnnotationPresent(EnableFeatures.class) || annotatedElement.isAnnotationPresent(EnableFeature.class));
    }

    private boolean isDisableFeature(AnnotatedElement annotatedElement) {
        return (annotatedElement.isAnnotationPresent(DisableFeatures.class) || annotatedElement.isAnnotationPresent(DisableFeature.class));
    }

    private boolean shouldExecuteAsLast(AnnotatedElement annotatedElement) {
        if (isEnableFeature(annotatedElement)) {
            return Arrays.stream(annotatedElement.getAnnotationsByType(EnableFeature.class))
                    .anyMatch(EnableFeature::executeAsLast);
        }

        if (isDisableFeature(annotatedElement)) {
            return Arrays.stream(annotatedElement.getAnnotationsByType(DisableFeature.class))
                    .anyMatch(DisableFeature::executeAsLast);
        }

        return false;
    }

    public void handleEnableFeaturesAnnotationBeforeClass(@Observes(precedence = 1) BeforeClass event) throws Exception {
        checkAnnotatedElementForFeatureAnnotations(event.getTestClass().getJavaClass(), State.BEFORE);
    }

    public void handleEnableFeaturesAnnotationBeforeTest(@Observes(precedence = 1) Before event) throws Exception {
        if (!shouldExecuteAsLast(event.getTestMethod())) {
            checkAnnotatedElementForFeatureAnnotations(event.getTestMethod(), State.BEFORE);
        }
    }

    // KEYCLOAK-13572 Precedence is too low in order to ensure the feature change will be executed as last.
    // If some fail occurs in @Before method, the feature doesn't change its state.
    public void handleChangeStateFeaturePriorityBeforeTest(@Observes(precedence = -100) Before event) throws Exception {
        if (shouldExecuteAsLast(event.getTestMethod())) {
            checkAnnotatedElementForFeatureAnnotations(event.getTestMethod(), State.BEFORE);
        }
    }

    public void handleEnableFeaturesAnnotationAfterTest(@Observes(precedence = 2) After event) throws Exception {
        checkAnnotatedElementForFeatureAnnotations(event.getTestMethod(), State.AFTER);
    }

    public void handleEnableFeaturesAnnotationAfterClass(@Observes(precedence = 2) AfterClass event) throws Exception {
        checkAnnotatedElementForFeatureAnnotations(event.getTestClass().getJavaClass(), State.AFTER);
    }

}
