package org.keycloak.models.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.admin.v2.OIDCClientRepresentation;
import org.keycloak.representations.admin.v2.SAMLClientRepresentation;
import org.keycloak.utils.StringUtil;

/**
 * Strip secrets from representations
 */
public class StripSecretsUtilsV2 extends StripSecretsUtils {
    private static final Map<Class<?>, BiConsumer<KeycloakSession, Object>> REPRESENTATION_FORMATTER = new HashMap<>();

    static {
        REPRESENTATION_FORMATTER.put(OIDCClientRepresentation.class, (session, o) -> StripSecretsUtilsV2.stripOidcClient((OIDCClientRepresentation) o));
        REPRESENTATION_FORMATTER.put(SAMLClientRepresentation.class, (session, o) -> StripSecretsUtilsV2.stripSamlClient((SAMLClientRepresentation) o));
    }

    public static <T> T stripSecrets(KeycloakSession session, T representation) {
        return stripSecrets(session, representation, REPRESENTATION_FORMATTER);
    }

    protected static OIDCClientRepresentation stripOidcClient(OIDCClientRepresentation rep) {
        Optional.ofNullable(rep.getAuth())
                .map(OIDCClientRepresentation.Auth::getSecret)
                .filter(StringUtil::isNotBlank)
                .ifPresent(secret -> rep.getAuth().setSecret(maskNonVaultValue(secret)));
        return rep;
    }

    protected static SAMLClientRepresentation stripSamlClient(SAMLClientRepresentation rep) {
        Optional.ofNullable(rep.getSigningCertificate())
                .filter(StringUtil::isNotBlank)
                .ifPresent(cert -> rep.setSigningCertificate(maskNonVaultValue(cert)));

        return rep;
    }

}
