package org.keycloak.testsuite.util;

import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.mappers.AddressMapper;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.protocol.oidc.mappers.HardcodedRole;
import org.keycloak.protocol.oidc.mappers.RoleNameMapper;
import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

/**
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>.
 */
public class ProtocolMapperUtil {

    /**
     * This is the migration of the method present at RoleNameMapper
     *
     * @param name
     * @param role
     * @param newName
     * @return
     */
    public static ProtocolMapperRepresentation createRoleNameMapper(String name,
                                                                    String role,
                                                                    String newName) {
        return ModelToRepresentation.toRepresentation(RoleNameMapper.create(name, role, newName));

    }

    public static ProtocolMapperRepresentation createHardcodedRole(String name,
                                                                   String role) {
        return ModelToRepresentation.toRepresentation(HardcodedRole.create(name, role));
    }

    /**
     * This is the migration of the method present at AddressMapper
     *
     * @param idToken
     * @param accessToken
     * @return
     */
    public static ProtocolMapperRepresentation createAddressMapper(boolean idToken, boolean accessToken) {
        return ModelToRepresentation.toRepresentation(AddressMapper.createAddressMapper(idToken, accessToken));
    }

    /**
     * This is the migration of the method present at HardcodedClaim
     *
     * @param name
     * @param hardcodedName
     * @param hardcodedValue
     * @param claimType
     * @param consentRequired
     * @param consentText
     * @param accessToken
     * @param idToken
     * @return
     */
    public static ProtocolMapperRepresentation createHardcodedClaim(String name,
                                                                    String hardcodedName,
                                                                    String hardcodedValue, String claimType,
                                                                    boolean consentRequired, String consentText,
                                                                    boolean accessToken, boolean idToken) {
        return ModelToRepresentation.toRepresentation(HardcodedClaim.create(name, hardcodedName, hardcodedValue,
                claimType, consentRequired, consentText, accessToken, idToken));
    }

    /**
     * Migrated from UserAttributeMapper
     *
     * @param name
     * @param userAttribute
     * @param tokenClaimName
     * @param claimType
     * @param consentRequired
     * @param consentText
     * @param accessToken
     * @param idToken
     * @param multivalued
     * @return
     */
    public static ProtocolMapperRepresentation createClaimMapper(String name,
                                                                 String userAttribute,
                                                                 String tokenClaimName, String claimType,
                                                                 boolean consentRequired, String consentText,
                                                                 boolean accessToken, boolean idToken, boolean multivalued) {
        return ModelToRepresentation.toRepresentation(UserAttributeMapper.createClaimMapper(name, userAttribute, tokenClaimName,
                claimType, consentRequired, consentText, accessToken, idToken, multivalued));

    }

    public static ProtocolMapperRepresentation createClaimMapper(String name,
                                                                 String userSessionNote,
                                                                 String tokenClaimName, String jsonType,
                                                                 boolean consentRequired, String consentText,
                                                                 boolean accessToken, boolean idToken) {

        return ModelToRepresentation.toRepresentation(UserSessionNoteMapper.createClaimMapper(name,
                userSessionNote,
                tokenClaimName, jsonType,
                consentRequired, consentText,
                accessToken, idToken));
    }
}
