package org.keycloak.testsuite.util;

import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.mappers.AddressMapper;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.protocol.oidc.mappers.HardcodedRole;
import org.keycloak.protocol.oidc.mappers.RoleNameMapper;
import org.keycloak.protocol.oidc.mappers.SHA256PairwiseSubMapper;
import org.keycloak.protocol.oidc.mappers.ScriptBasedOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;
import org.keycloak.protocol.oidc.mappers.UserClientRoleMappingMapper;
import org.keycloak.protocol.oidc.mappers.UserRealmRoleMappingMapper;
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
    public static ProtocolMapperRepresentation createAddressMapper(boolean idToken, boolean accessToken, boolean userInfo) {
        return ModelToRepresentation.toRepresentation(AddressMapper.createAddressMapper(idToken, accessToken, userInfo));
    }

    /**
     * This is the migration of the method present at HardcodedClaim
     *
     * @param name
     * @param hardcodedName
     * @param hardcodedValue
     * @param claimType
     * @param accessToken
     * @param idToken
     * @return
     */
    public static ProtocolMapperRepresentation createHardcodedClaim(String name,
                                                                    String hardcodedName,
                                                                    String hardcodedValue, String claimType,
                                                                    boolean accessToken, boolean idToken) {
        return ModelToRepresentation.toRepresentation(HardcodedClaim.create(name, hardcodedName, hardcodedValue,
                claimType, accessToken, idToken));
    }

    /**
     * Migrated from UserAttributeMapper
     *
     * @param name
     * @param userAttribute
     * @param tokenClaimName
     * @param claimType
     * @param accessToken
     * @param idToken
     * @param multivalued
     * @return
     */
    public static ProtocolMapperRepresentation createClaimMapper(String name,
                                                                 String userAttribute,
                                                                 String tokenClaimName, String claimType,
                                                                 boolean accessToken, boolean idToken, boolean multivalued) {
        return ModelToRepresentation.toRepresentation(UserAttributeMapper.createClaimMapper(name, userAttribute, tokenClaimName,
                claimType, accessToken, idToken, multivalued, false));

    }

    public static ProtocolMapperRepresentation createClaimMapper(String name,
            String userAttribute, String userProfileAttribute,
            String tokenClaimName, String claimType,
            boolean accessToken, boolean idToken, boolean multivalued) {
        return ModelToRepresentation.toRepresentation(UserAttributeMapper.createClaimMapper(name, userAttribute, userProfileAttribute, tokenClaimName,
                claimType, accessToken, idToken, multivalued, false));

    }

    public static ProtocolMapperRepresentation createClaimMapper(String name,
                                                                 String userAttribute,
                                                                 String tokenClaimName, String claimType,
                                                                 boolean accessToken, boolean idToken,
                                                                 boolean multivalued, boolean aggregateAttrs) {
        return ModelToRepresentation.toRepresentation(UserAttributeMapper.createClaimMapper(name, userAttribute, tokenClaimName,
                claimType, accessToken, idToken, multivalued, aggregateAttrs));

    }

    public static ProtocolMapperRepresentation createClaimMapper(String name,
                                                                 String userSessionNote,
                                                                 String tokenClaimName, String jsonType,
                                                                 boolean accessToken, boolean idToken) {

        return ModelToRepresentation.toRepresentation(UserSessionNoteMapper.createClaimMapper(name,
                userSessionNote,
                tokenClaimName, jsonType,
                accessToken, idToken));
    }


    public static ProtocolMapperRepresentation createUserRealmRoleMappingMapper(String realmRolePrefix,
                                                                                String name,
                                                                                String tokenClaimName,
                                                                                boolean accessToken, boolean idToken) {

        return createUserRealmRoleMappingMapper(realmRolePrefix, name, tokenClaimName, accessToken, idToken, false);
    }

    public static ProtocolMapperRepresentation createUserRealmRoleMappingMapper(String realmRolePrefix,
                                                                                String name,
                                                                                String tokenClaimName,
                                                                                boolean accessToken, boolean idToken, boolean multiValued) {

        return ModelToRepresentation.toRepresentation(UserRealmRoleMappingMapper.create(realmRolePrefix, name, tokenClaimName, accessToken, idToken, multiValued));
    }

    public static ProtocolMapperRepresentation createUserClientRoleMappingMapper(String clientId, String clientRolePrefix,
                                                                                String name,
                                                                                String tokenClaimName,
                                                                                boolean accessToken, boolean idToken) {

        return createUserClientRoleMappingMapper(clientId, clientRolePrefix, name, tokenClaimName, accessToken, idToken, false);
    }

    public static ProtocolMapperRepresentation createUserClientRoleMappingMapper(String clientId, String clientRolePrefix,
                                                                                 String name,
                                                                                 String tokenClaimName,
                                                                                 boolean accessToken, boolean idToken, boolean multiValued) {

        return ModelToRepresentation.toRepresentation(UserClientRoleMappingMapper.create(clientId, clientRolePrefix, name, tokenClaimName, accessToken, idToken, multiValued));
    }

    public static ProtocolMapperRepresentation getMapperByNameAndProtocol(ProtocolMappersResource protocolMappers, String protocol, String name) {
        for (ProtocolMapperRepresentation mapper : protocolMappers.getMappersPerProtocol(protocol)) {
            if (name.equals(mapper.getName())) {
                return mapper;
            }
        }
        return null;
    }

    public static ProtocolMapperRepresentation createScriptMapper(String name,
                                                                  String userAttribute,
                                                                  String tokenClaimName,
                                                                  String claimType,
                                                                  boolean accessToken,
                                                                  boolean idToken,
                                                                  String script,
                                                                  boolean multiValued) {

        return ModelToRepresentation.toRepresentation(
          ScriptBasedOIDCProtocolMapper.create(name, userAttribute, tokenClaimName, claimType, accessToken, idToken, script, multiValued)
        );
    }

    public static ProtocolMapperRepresentation createPairwiseMapper(String sectorIdentifierUri, String salt) {
        return SHA256PairwiseSubMapper.createPairwiseMapper(sectorIdentifierUri, salt);
    }

    public static ProtocolMapperRepresentation createAudienceMapper(String name,
                                                                    String includedClientAudience,
                                                                    String includedCustomAudience,
                                                                    boolean accessToken, boolean idToken) {

        return ModelToRepresentation.toRepresentation(
                AudienceProtocolMapper.createClaimMapper(name, includedClientAudience, includedCustomAudience, accessToken, idToken)
        );
    }
}
