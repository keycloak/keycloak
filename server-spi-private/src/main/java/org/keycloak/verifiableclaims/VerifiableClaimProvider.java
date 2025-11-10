package org.keycloak.verifiableclaims;

import org.keycloak.models.*;
import org.keycloak.provider.Provider;
import org.keycloak.userprofile.Attributes;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.verifiableclaims.model.*;

import java.util.Map;

public interface VerifiableClaimProvider extends Provider {

    Map<String, VerifiableAttributeConfig> getVerifiableAttributeConfig(KeycloakSession session, RealmModel realm);

    UpdateDecision onAttributesAboutToUpdate(KeycloakSession session,
                                             RealmModel realm,
                                             UserModel user,            // null on create
                                             UserProfile profile,
                                             UserProfileContext context,
                                             Attributes incoming) throws VerifiableClaimException;

    Attributes enrichAttributesForRepresentation(KeycloakSession session,
                                                 RealmModel realm,
                                                 UserModel user,
                                                 Attributes base,
                                                 UserProfileContext context);

    default boolean completeAttestation(KeycloakSession session,
                                        RealmModel realm,
                                        String userId,
                                        String correlationRef,
                                        AttestationResult result) {
        return false;
    }
}
