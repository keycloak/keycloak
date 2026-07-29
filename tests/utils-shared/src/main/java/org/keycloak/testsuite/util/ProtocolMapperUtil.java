package org.keycloak.testsuite.util;

import java.util.EnumSet;
import java.util.Set;

import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.mappers.AddressMapper;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.protocol.oidc.mappers.HardcodedRole;
import org.keycloak.protocol.oidc.mappers.OIDCProtocolMapperBuilder.ClaimType;
import org.keycloak.protocol.oidc.mappers.OIDCProtocolMapperBuilder.IncludeIn;
import org.keycloak.protocol.oidc.mappers.RoleNameMapper;
import org.keycloak.protocol.oidc.mappers.SHA256PairwiseSubMapper;
import org.keycloak.protocol.oidc.mappers.ScriptBasedOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.UserAttributeMapper;
import org.keycloak.protocol.oidc.mappers.UserClientRoleMappingMapper;
import org.keycloak.protocol.oidc.mappers.UserRealmRoleMappingMapper;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

import static org.keycloak.protocol.oidc.mappers.OIDCProtocolMapperBuilder.IncludeIn.ACCESS_TOKEN;
import static org.keycloak.protocol.oidc.mappers.OIDCProtocolMapperBuilder.IncludeIn.ID_TOKEN;
import static org.keycloak.protocol.oidc.mappers.OIDCProtocolMapperBuilder.IncludeIn.INTROSPECTION;
import static org.keycloak.protocol.oidc.mappers.OIDCProtocolMapperBuilder.IncludeIn.USERINFO;

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
        return ModelToRepresentation.toRepresentation(RoleNameMapper.builder(name).role(role).newRoleName(newName).build());
    }

    public static ProtocolMapperRepresentation createHardcodedRole(String name,
                                                                   String role) {
        return ModelToRepresentation.toRepresentation(HardcodedRole.builder(name).role(role).build());
    }

    /**
     * This is the migration of the method present at AddressMapper
     *
     * @param idToken
     * @param accessToken
     * @return
     */
    public static ProtocolMapperRepresentation createAddressMapper(boolean idToken, boolean accessToken, boolean userInfo, boolean introspectionEndpoint) {
        return ModelToRepresentation.toRepresentation(AddressMapper.builder("address")
                .includeIn(collectTargets(accessToken, idToken, userInfo, introspectionEndpoint))
                .build());
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
                                                                    boolean accessToken, boolean idToken, boolean introspectionEndpoint) {
        return ModelToRepresentation.toRepresentation(HardcodedClaim.builder(name, hardcodedName, hardcodedValue)
                .type(claimType)
                .includeIn(collectTargets(accessToken, idToken, introspectionEndpoint))
                .build());
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
                                                                 boolean accessToken, boolean idToken, boolean introspectionEndpoint, boolean multivalued) {
        return createClaimMapper(name, userAttribute, tokenClaimName, claimType, accessToken, idToken, introspectionEndpoint, multivalued, false);
    }

    public static ProtocolMapperRepresentation createClaimMapper(String name,
                                                                 String userAttribute,
                                                                 String tokenClaimName, String claimType,
                                                                 boolean accessToken, boolean idToken, boolean introspectionEndpoint,
                                                                 boolean multivalued, boolean aggregateAttrs) {
        var targets = collectTargets(accessToken, idToken, introspectionEndpoint);
        targets.add(USERINFO);
        return ModelToRepresentation.toRepresentation(UserAttributeMapper.builder(name)
                .userAttribute(userAttribute)
                .claimName(tokenClaimName)
                .type(claimType)
                .includeIn(targets)
                .multivalued(multivalued)
                .aggregateAttributes(aggregateAttrs)
                .build());
    }

    public static ProtocolMapperRepresentation createClaimMapper(String name,
                                                                 String userSessionNote,
                                                                 String tokenClaimName, String jsonType,
                                                                 boolean accessToken, boolean idToken, boolean introspectionEndpoint) {
        return ModelToRepresentation.toRepresentation(UserSessionNoteMapper.builder(name, userSessionNote)
                .claimName(tokenClaimName)
                .type(jsonType)
                .includeIn(collectTargets(accessToken, idToken, introspectionEndpoint))
                .build());
    }

    public static ProtocolMapperRepresentation createUserRealmRoleMappingMapper(String realmRolePrefix,
                                                                                String name,
                                                                                String tokenClaimName,
                                                                                boolean accessToken, boolean idToken, boolean introspectionEndpoint) {
        return createUserRealmRoleMappingMapper(realmRolePrefix, name, tokenClaimName, accessToken, idToken, introspectionEndpoint, true);
    }

    public static ProtocolMapperRepresentation createUserRealmRoleMappingMapper(String realmRolePrefix,
                                                                                String name,
                                                                                String tokenClaimName,
                                                                                boolean accessToken, boolean idToken, boolean introspectionEndpoint, boolean multiValued) {
        return ModelToRepresentation.toRepresentation(UserRealmRoleMappingMapper.builder(name)
                .claimName(tokenClaimName)
                .type(ClaimType.STRING)
                .multivalued(multiValued)
                .realmRolePrefix(realmRolePrefix)
                .includeIn(collectTargets(accessToken, idToken, introspectionEndpoint))
                .build());
    }

    public static ProtocolMapperRepresentation createUserClientRoleMappingMapper(String clientId, String clientRolePrefix,
                                                                                String name,
                                                                                String tokenClaimName,
                                                                                boolean accessToken, boolean idToken, boolean introspectionEndpoint) {
        return createUserClientRoleMappingMapper(clientId, clientRolePrefix, name, tokenClaimName, accessToken, idToken, introspectionEndpoint, true);
    }

    public static ProtocolMapperRepresentation createUserClientRoleMappingMapper(String clientId, String clientRolePrefix,
                                                                                 String name,
                                                                                 String tokenClaimName,
                                                                                 boolean accessToken, boolean idToken, boolean introspectionEndpoint, boolean multiValued) {
        return ModelToRepresentation.toRepresentation(UserClientRoleMappingMapper.builder(name)
                .claimName(tokenClaimName)
                .type(ClaimType.STRING)
                .multivalued(multiValued)
                .clientId(clientId)
                .clientRolePrefix(clientRolePrefix)
                .includeIn(collectTargets(accessToken, idToken, introspectionEndpoint))
                .build());
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
                                                                  boolean introspectionEndpoint,
                                                                  String script,
                                                                  boolean multiValued) {
        return ModelToRepresentation.toRepresentation(ScriptBasedOIDCProtocolMapper.builder(name)
                .userAttribute(userAttribute)
                .claimName(tokenClaimName)
                .type(claimType)
                .multivalued(multiValued)
                .script(script)
                .includeIn(collectTargets(accessToken, idToken, introspectionEndpoint))
                .build());
    }

    public static ProtocolMapperRepresentation createPairwiseMapper(String sectorIdentifierUri, String salt) {
        return SHA256PairwiseSubMapper.createPairwiseMapper(sectorIdentifierUri, salt);
    }

    public static ProtocolMapperRepresentation createAudienceMapper(String name,
                                                                    String includedClientAudience,
                                                                    String includedCustomAudience,
                                                                    boolean accessToken, boolean idToken, boolean introspectionEndpoint) {
        var builder = AudienceProtocolMapper.builder(name);
        if (includedClientAudience != null) builder.clientAudience(includedClientAudience);
        if (includedCustomAudience != null) builder.customAudience(includedCustomAudience);
        return ModelToRepresentation.toRepresentation(builder
                .includeIn(collectTargets(accessToken, idToken, introspectionEndpoint))
                .build());
    }

    private static Set<IncludeIn> collectTargets(boolean accessToken, boolean idToken, boolean introspectionEndpoint) {
        return collectTargets(accessToken, idToken, false, introspectionEndpoint);
    }

    private static Set<IncludeIn> collectTargets(boolean accessToken, boolean idToken, boolean userInfo, boolean introspectionEndpoint) {
        Set<IncludeIn> targets = EnumSet.noneOf(IncludeIn.class);
        if (accessToken) targets.add(ACCESS_TOKEN);
        if (idToken) targets.add(ID_TOKEN);
        if (userInfo) targets.add(USERINFO);
        if (introspectionEndpoint) targets.add(INTROSPECTION);
        return targets;
    }
}
