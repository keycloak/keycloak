package org.keycloak.broker.provider.util;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.broker.provider.ClientAssertionIdentityProvider;
import org.keycloak.broker.provider.ExchangeExternalToken;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.JWTAuthorizationGrantProvider;
import org.keycloak.broker.provider.UserAuthenticationIdentityProvider;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.models.IdentityProviderCapability;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderFactory;

public class IdentityProviderTypeUtil {

    private IdentityProviderTypeUtil() {
    }

    public static List<IdentityProviderType> listTypesFromFactory(KeycloakSession session, String factoryId) {
        KeycloakSessionFactory sf = session.getKeycloakSessionFactory();
        ProviderFactory<?> factory = sf.getProviderFactory(IdentityProvider.class, factoryId);
        if (factory == null) {
            return List.of();
        }
        Class<?> providerType = getType(factory);
        return Arrays.stream(IdentityProviderType.values())
                .filter(t -> !t.equals(IdentityProviderType.ANY) && toTypeClass(t).isAssignableFrom(providerType))
                .collect(Collectors.toList());
    }

    public static List<String> listFactoriesByCapability(KeycloakSession session, IdentityProviderCapability capability) {
        Set<IdentityProviderType> types = Arrays.stream(IdentityProviderType.values()).filter(t -> t.getCapabilities().contains(capability)).collect(Collectors.toSet());
        return listFactoriesByTypes(session, types);
    }

    public static List<String> listFactoriesByType(KeycloakSession session, IdentityProviderType type) {
        return listFactoriesByTypes(session, Set.of(type));
    }

    private static List<String> listFactoriesByTypes(KeycloakSession session, Set<IdentityProviderType> types) {
        KeycloakSessionFactory sf = session.getKeycloakSessionFactory();

        Stream<ProviderFactory> factories = sf.getProviderFactoriesStream(IdentityProvider.class);
        if (types.contains(IdentityProviderType.ANY) || types.contains(IdentityProviderType.USER_AUTHENTICATION)) {
            factories = Stream.concat(factories, sf.getProviderFactoriesStream(SocialIdentityProvider.class));
        }

        Set<Class<?>> typeClasses = types.stream().map(IdentityProviderTypeUtil::toTypeClass).collect(Collectors.toSet());

        return factories.filter(f -> typeClasses.stream().anyMatch(t -> t.isAssignableFrom(getType(f))))
                .map(ProviderFactory::getId)
                .toList();
    }

    private static Class<?> getType(ProviderFactory<?> f) {
        try {
            return f.getClass().getMethod("create", KeycloakSession.class, IdentityProviderModel.class).getReturnType();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static Class<?> toTypeClass(IdentityProviderType type) {
        return switch (type) {
            case USER_AUTHENTICATION -> UserAuthenticationIdentityProvider.class;
            case CLIENT_ASSERTION -> ClientAssertionIdentityProvider.class;
            case EXCHANGE_EXTERNAL_TOKEN -> ExchangeExternalToken.class;
            case JWT_AUTHORIZATION_GRANT -> JWTAuthorizationGrantProvider.class;
            case ANY -> IdentityProvider.class;
        };
    }

}
