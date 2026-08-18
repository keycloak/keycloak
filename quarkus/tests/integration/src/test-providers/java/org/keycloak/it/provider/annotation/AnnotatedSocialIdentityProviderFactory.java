package org.keycloak.it.provider.annotation;

import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.KeycloakProvider;

import org.jboss.logging.Logger;

/**
 * Valid {@link KeycloakProvider} factory registered for the {@code social} SPI. Its factory interface,
 * {@link SocialIdentityProviderFactory}, extends {@link IdentityProviderFactory}, the factory interface of the
 * {@code identity_provider} SPI, so the class is assignable to both. Discovery must nevertheless register it
 * for the declared SPI only, exactly like the built-in social providers are listed in
 * {@code META-INF/services/org.keycloak.broker.social.SocialIdentityProviderFactory} but not in the
 * {@code IdentityProviderFactory} descriptor. The factory verifies this from
 * {@link #postInit(KeycloakSessionFactory)} by looking itself up under both provider classes and logging
 * the outcome.
 */
@KeycloakProvider(SocialIdentityProviderFactory.class)
public class AnnotatedSocialIdentityProviderFactory extends AbstractIdentityProviderFactory<SocialIdentityProvider>
        implements SocialIdentityProviderFactory<SocialIdentityProvider> {

    public static final String PROVIDER_ID = "annotated-social";
    public static final String REGISTERED_AS_SOCIAL_IDENTITY_PROVIDER_MESSAGE =
            "AnnotatedSocialIdentityProviderFactory registered for " + SocialIdentityProvider.class.getSimpleName();
    public static final String REGISTERED_AS_IDENTITY_PROVIDER_MESSAGE =
            "AnnotatedSocialIdentityProviderFactory registered for " + IdentityProvider.class.getSimpleName();

    private static final Logger LOG = Logger.getLogger(AnnotatedSocialIdentityProviderFactory.class);

    @Override
    public String getName() {
        return "Annotated Social Test Provider";
    }

    @Override
    public SocialIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        throw new UnsupportedOperationException("test factory, not meant to be configured");
    }

    @Override
    public IdentityProviderModel createConfig() {
        return new IdentityProviderModel();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        if (factory.getProviderFactory(SocialIdentityProvider.class, PROVIDER_ID) != null) {
            LOG.info(REGISTERED_AS_SOCIAL_IDENTITY_PROVIDER_MESSAGE);
        }
        if (factory.getProviderFactory(IdentityProvider.class, PROVIDER_ID) != null) {
            LOG.info(REGISTERED_AS_IDENTITY_PROVIDER_MESSAGE);
        }
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
