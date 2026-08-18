package org.keycloak.it.cli.dist;

import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.TestProvider;
import org.keycloak.it.provider.annotation.AbstractProviderFactory;
import org.keycloak.it.provider.annotation.AbstractProviderFactoryTestProvider;
import org.keycloak.it.provider.annotation.AnnotatedEventListenerProviderFactory;
import org.keycloak.it.provider.annotation.AnnotatedEventListenerTestProvider;
import org.keycloak.it.provider.annotation.AnnotatedSocialIdentityProviderFactory;
import org.keycloak.it.provider.annotation.AnnotatedSocialIdentityProviderTestProvider;
import org.keycloak.it.provider.annotation.NoPublicConstructorProviderFactory;
import org.keycloak.it.provider.annotation.NoPublicConstructorTestProvider;
import org.keycloak.it.provider.annotation.NotAProviderFactory;
import org.keycloak.it.provider.annotation.NotAProviderFactoryTestProvider;
import org.keycloak.provider.KeycloakProvider;

import io.quarkus.test.junit.main.Launch;
import org.junit.jupiter.api.Test;

/**
 * Exercises the build-time {@link KeycloakProvider} discovery against a real distribution: a provider jar
 * without any {@code META-INF/services} descriptor is dropped into {@code providers/}, and augmentation must
 * either pick the annotated factory up or fail the build with the validation error.
 */
@DistributionTest
@RawDistOnly(reason = "Containers are immutable")
public class KeycloakProviderAnnotationDistTest {

    @Test
    @TestProvider(AnnotatedEventListenerTestProvider.class)
    @Launch({ "start-dev" })
    void annotatedProviderIsDiscoveredWithoutServiceDescriptor(CLIResult cliResult) {
        cliResult.assertMessage(AnnotatedEventListenerProviderFactory.INIT_MESSAGE);
        cliResult.assertStartedDevMode();
    }

    /**
     * A factory registered for the {@code social} SPI, whose factory interface extends the
     * {@code identity_provider} SPI's factory interface, must show up under {@code social} only.
     * Assignability alone could not tell the two SPIs apart; registration is by the exact interface
     * named in the annotation, just as with a {@code META-INF/services} descriptor.
     */
    @Test
    @TestProvider(AnnotatedSocialIdentityProviderTestProvider.class)
    @Launch({ "start-dev" })
    void annotatedProviderIsRegisteredForDeclaredSpiOnly(CLIResult cliResult) {
        cliResult.assertMessage(AnnotatedSocialIdentityProviderFactory.REGISTERED_AS_SOCIAL_IDENTITY_PROVIDER_MESSAGE);
        cliResult.assertNoMessage(AnnotatedSocialIdentityProviderFactory.REGISTERED_AS_IDENTITY_PROVIDER_MESSAGE);
        cliResult.assertStartedDevMode();
    }

    @Test
    @TestProvider(AbstractProviderFactoryTestProvider.class)
    @Launch({ "build" })
    void buildFailsForAbstractAnnotatedClass(CLIResult cliResult) {
        cliResult.assertError("@" + KeycloakProvider.class.getSimpleName() + " class "
                + AbstractProviderFactory.class.getName() + " must be a public, non-abstract class");
    }

    @Test
    @TestProvider(NoPublicConstructorTestProvider.class)
    @Launch({ "build" })
    void buildFailsForAnnotatedClassWithoutPublicNoArgConstructor(CLIResult cliResult) {
        cliResult.assertError("@" + KeycloakProvider.class.getSimpleName() + " class "
                + NoPublicConstructorProviderFactory.class.getName() + " must have a public no-arg constructor");
    }

    @Test
    @TestProvider(NotAProviderFactoryTestProvider.class)
    @Launch({ "build" })
    void buildFailsForAnnotatedClassNotImplementingProviderFactory(CLIResult cliResult) {
        cliResult.assertError("@" + KeycloakProvider.class.getSimpleName() + " class "
                + NotAProviderFactory.class.getName() + " does not implement " + EventListenerProviderFactory.class.getName());
    }
}
