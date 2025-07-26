package org.keycloak.utils;

import org.keycloak.exceptions.MetadataPolicyCombinationException;
import org.keycloak.exceptions.MetadataPolicyException;
import org.keycloak.representations.openid_federation.EntityStatement;
import org.keycloak.representations.openid_federation.MetadataPolicy;
import org.keycloak.representations.openid_federation.RPMetadata;
import org.keycloak.representations.openid_federation.RPMetadataPolicy;

public class MetadataPolicyUtils {

    public static RPMetadataPolicy combineClientPolicies(RPMetadataPolicy superior, RPMetadataPolicy inferior) throws MetadataPolicyCombinationException {

        if (inferior == null) {
            return superior;
        }

        if (superior == null) {
            return inferior;
        }

        if (superior.getApplicationType() != null) {
            superior.setApplicationType(superior.getApplicationType().combinePolicy(inferior.getApplicationType()));
        } else {
            superior.setApplicationType(inferior.getApplicationType());
        }

        if (superior.getClientIdIssuedAt() != null) {
            superior.setClientIdIssuedAt(superior.getClientIdIssuedAt().combinePolicy(inferior.getClientIdIssuedAt()));
        } else {
            superior.setClientIdIssuedAt(inferior.getClientIdIssuedAt());
        }

        if (superior.getClientName() != null) {
            superior.setClientName(superior.getClientName().combinePolicy(inferior.getClientName()));
        } else {
            superior.setClientName(inferior.getClientName());
        }

        if (superior.getClientRegistrationTypes() != null) {
            superior.setClientRegistrationTypes(superior.getClientRegistrationTypes().combinePolicy(inferior.getClientRegistrationTypes()));
        } else {
            superior.setClientRegistrationTypes(inferior.getClientRegistrationTypes());
        }

        if (superior.getClientSecretExpiresAt() != null) {
            superior.setClientSecretExpiresAt(superior.getClientSecretExpiresAt().combinePolicy(inferior.getClientSecretExpiresAt()));
        } else {
            superior.setClientSecretExpiresAt(inferior.getClientSecretExpiresAt());
        }

        if (superior.getClientUri() != null) {
            superior.setClientUri(superior.getClientUri().combinePolicy(inferior.getClientUri()));
        } else {
            superior.setClientUri(inferior.getClientUri());
        }

        if (superior.getContacts() != null) {
            superior.setContacts(superior.getContacts().combinePolicy(inferior.getContacts()));
        } else {
            superior.setContacts(inferior.getContacts());
        }

        if (superior.getDefaultAcrValues() != null) {
            superior.setDefaultAcrValues(superior.getDefaultAcrValues().combinePolicy(inferior.getDefaultAcrValues()));
        } else {
            superior.setDefaultAcrValues(inferior.getDefaultAcrValues());
        }

        if (superior.getDefaultMaxAge() != null) {
            superior.setDefaultMaxAge(superior.getDefaultMaxAge().combinePolicy(inferior.getDefaultMaxAge()));
        } else {
            superior.setDefaultMaxAge(inferior.getDefaultMaxAge());
        }

        if (superior.getGrantTypes() != null) {
            superior.setGrantTypes(superior.getGrantTypes().combinePolicy(inferior.getGrantTypes()));
        } else {
            superior.setGrantTypes(inferior.getGrantTypes());
        }

        if (superior.getIdTokenEncryptedResponseAlg() != null) {
            superior.setIdTokenEncryptedResponseAlg(superior.getIdTokenEncryptedResponseAlg().combinePolicy(inferior.getIdTokenEncryptedResponseAlg()));
        } else {
            superior.setIdTokenEncryptedResponseAlg(inferior.getIdTokenEncryptedResponseAlg());
        }

        if (superior.getIdTokenEncryptedResponseEnc() != null) {
            superior.setIdTokenEncryptedResponseEnc(superior.getIdTokenEncryptedResponseEnc().combinePolicy(inferior.getIdTokenEncryptedResponseEnc()));
        } else {
            superior.setIdTokenEncryptedResponseEnc(inferior.getIdTokenEncryptedResponseEnc());
        }

        if (superior.getIdTokenSignedResponseAlg() != null) {
            superior.setIdTokenSignedResponseAlg(superior.getIdTokenSignedResponseAlg().combinePolicy(inferior.getIdTokenSignedResponseAlg()));
        } else {
            superior.setIdTokenSignedResponseAlg(inferior.getIdTokenSignedResponseAlg());
        }

        if (superior.getInitiateLoginUri() != null) {
            superior.setInitiateLoginUri(superior.getInitiateLoginUri().combinePolicy(inferior.getInitiateLoginUri()));
        } else {
            superior.setInitiateLoginUri(inferior.getInitiateLoginUri());
        }

        if (superior.getJwksUri() != null) {
            superior.setJwksUri(superior.getJwksUri().combinePolicy(inferior.getJwksUri()));
        } else {
            superior.setJwksUri(inferior.getJwksUri());
        }

        if (superior.getLogoUri() != null) {
            superior.setLogoUri(superior.getLogoUri().combinePolicy(inferior.getLogoUri()));
        } else {
            superior.setLogoUri(inferior.getLogoUri());
        }

        if (superior.getCommonMetadataPolicy().getOrganizationName() != null) {
            superior.getCommonMetadataPolicy().setOrganizationName(superior.getCommonMetadataPolicy().getOrganizationName().combinePolicy(inferior.getCommonMetadataPolicy().getOrganizationName()));
        } else {
            superior.getCommonMetadataPolicy().setOrganizationName(inferior.getCommonMetadataPolicy().getOrganizationName());
        }

        if (superior.getPolicyUri() != null) {
            superior.setPolicyUri(superior.getPolicyUri().combinePolicy(inferior.getPolicyUri()));
        } else {
            superior.setPolicyUri(inferior.getPolicyUri());
        }

        if (superior.getPostLogoutRedirectUris() != null) {
            superior.setPostLogoutRedirectUris(superior.getPostLogoutRedirectUris().combinePolicy(inferior.getPostLogoutRedirectUris()));
        } else {
            superior.setPostLogoutRedirectUris(inferior.getPostLogoutRedirectUris());
        }

        if (superior.getRedirectUris() != null) {
            superior.setRedirectUris(superior.getRedirectUris().combinePolicy(inferior.getRedirectUris()));
        } else {
            superior.setRedirectUris(inferior.getRedirectUris());
        }

        if (superior.getRegistrationAccessToken() != null) {
            superior.setRegistrationAccessToken(superior.getRegistrationAccessToken().combinePolicy(inferior.getRegistrationAccessToken()));
        } else {
            superior.setRegistrationAccessToken(inferior.getRegistrationAccessToken());
        }

        if (superior.getRegistrationClientUri() != null) {
            superior.setRegistrationClientUri(superior.getRegistrationClientUri().combinePolicy(inferior.getRegistrationClientUri()));
        } else {
            superior.setRegistrationClientUri(inferior.getRegistrationClientUri());
        }

        if (superior.getRequestObjectEncryptionAlg() != null) {
            superior.setRequestObjectEncryptionAlg(superior.getRequestObjectEncryptionAlg().combinePolicy(inferior.getRequestObjectEncryptionAlg()));
        } else {
            superior.setRequestObjectEncryptionAlg(inferior.getRequestObjectEncryptionAlg());
        }

        if (superior.getRequestObjectEncryptionEnc() != null) {
            superior.setRequestObjectEncryptionEnc(superior.getRequestObjectEncryptionEnc().combinePolicy(inferior.getRequestObjectEncryptionEnc()));
        } else {
            superior.setRequestObjectEncryptionEnc(inferior.getRequestObjectEncryptionEnc());
        }

        if (superior.getRequestObjectSigningAlg() != null) {
            superior.setRequestObjectSigningAlg(superior.getRequestObjectSigningAlg().combinePolicy(inferior.getRequestObjectSigningAlg()));
        } else {
            superior.setRequestObjectSigningAlg(inferior.getRequestObjectSigningAlg());
        }

        if (superior.getRequestUris() != null) {
            superior.setRequestUris(superior.getRequestUris().combinePolicy(inferior.getRequestUris()));
        } else {
            superior.setRequestUris(inferior.getRequestUris());
        }

        if (superior.getRequireAuthTime() != null) {
            superior.setRequireAuthTime(superior.getRequireAuthTime().combinePolicy(inferior.getRequireAuthTime()));
        } else {
            superior.setRequireAuthTime(inferior.getRequireAuthTime());
        }

        if (superior.getResponseTypes() != null) {
            superior.setResponseTypes(superior.getResponseTypes().combinePolicy(inferior.getResponseTypes()));
        } else {
            superior.setResponseTypes(inferior.getResponseTypes());
        }

        if (superior.getScope() != null) {
            superior.setScope(superior.getScope().combinePolicy(inferior.getScope()));
        } else {
            superior.setScope(inferior.getScope());
        }

        if (superior.getSectorIdentifierUri() != null) {
            superior.setSectorIdentifierUri(superior.getSectorIdentifierUri().combinePolicy(inferior.getSectorIdentifierUri()));
        } else {
            superior.setSectorIdentifierUri(inferior.getSectorIdentifierUri());
        }

        if (superior.getSoftwareId() != null) {
            superior.setSoftwareId(superior.getSoftwareId().combinePolicy(inferior.getSoftwareId()));
        } else {
            superior.setSoftwareId(inferior.getSoftwareId());
        }

        if (superior.getSoftwareVersion() != null) {
            superior.setSoftwareVersion(superior.getSoftwareVersion().combinePolicy(inferior.getSoftwareVersion()));
        } else {
            superior.setSoftwareVersion(inferior.getSoftwareVersion());
        }

        if (superior.getSubjectType() != null) {
            superior.setSubjectType(superior.getSubjectType().combinePolicy(inferior.getSubjectType()));
        } else {
            superior.setSubjectType(inferior.getSubjectType());
        }

        if (superior.getTlsClientAuthSubjectDn() != null) {
            superior.setTlsClientAuthSubjectDn(superior.getTlsClientAuthSubjectDn().combinePolicy(inferior.getTlsClientAuthSubjectDn()));
        } else {
            superior.setTlsClientAuthSubjectDn(inferior.getTlsClientAuthSubjectDn());
        }

        if (superior.getTlsClientCertificateBoundAccessTokens() != null) {
            superior.setTlsClientCertificateBoundAccessTokens(superior.getTlsClientCertificateBoundAccessTokens().combinePolicy(inferior.getTlsClientCertificateBoundAccessTokens()));
        } else {
            superior.setTlsClientCertificateBoundAccessTokens(inferior.getTlsClientCertificateBoundAccessTokens());
        }

        if (superior.getTokenEndpointAuthMethod() != null) {
            superior.setTokenEndpointAuthMethod(superior.getTokenEndpointAuthMethod().combinePolicy(inferior.getTokenEndpointAuthMethod()));
        } else {
            superior.setTokenEndpointAuthMethod(inferior.getTokenEndpointAuthMethod());
        }

        if (superior.getTokenEndpointAuthSigningAlg() != null) {
            superior.setTokenEndpointAuthSigningAlg(superior.getTokenEndpointAuthSigningAlg().combinePolicy(inferior.getTokenEndpointAuthSigningAlg()));
        } else {
            superior.setTokenEndpointAuthSigningAlg(inferior.getTokenEndpointAuthSigningAlg());
        }

        if (superior.getTosUri() != null) {
            superior.setTosUri(superior.getTosUri().combinePolicy(inferior.getTosUri()));
        } else {
            superior.setTosUri(inferior.getTosUri());
        }

        if (superior.getUserinfoEncryptedResponseAlg() != null) {
            superior.setUserinfoEncryptedResponseAlg(superior.getUserinfoEncryptedResponseAlg().combinePolicy(inferior.getUserinfoEncryptedResponseAlg()));
        } else {
            superior.setUserinfoEncryptedResponseAlg(inferior.getUserinfoEncryptedResponseAlg());
        }

        if (superior.getUserinfoEncryptedResponseEnc() != null) {
            superior.setUserinfoEncryptedResponseEnc(superior.getUserinfoEncryptedResponseEnc().combinePolicy(inferior.getUserinfoEncryptedResponseEnc()));
        } else {
            superior.setUserinfoEncryptedResponseEnc(inferior.getUserinfoEncryptedResponseEnc());
        }

        if (superior.getUserinfoSignedResponseAlg() != null) {
            superior.setUserinfoSignedResponseAlg(superior.getUserinfoSignedResponseAlg().combinePolicy(inferior.getUserinfoSignedResponseAlg()));
        } else {
            superior.setUserinfoSignedResponseAlg(inferior.getUserinfoSignedResponseAlg());
        }

        return superior;
    }

    public static EntityStatement applyPoliciesToRPStatement(EntityStatement entity, RPMetadataPolicy policy) throws MetadataPolicyException, MetadataPolicyCombinationException {

        if (entity.getMetadata().getRelyingPartyMetadata() == null) {
            throw new MetadataPolicyException("Try to enforce metapolicy for RP to an entity statement without RP");
        }

        if (policy == null) {
            return entity;
        }

        RPMetadata rp = entity.getMetadata().getRelyingPartyMetadata();

        if (policy.getApplicationType() != null) {
            rp.setApplicationType(policy.getApplicationType().enforcePolicy(rp.getApplicationType(), "ApplicationType"));
        }

        if (policy.getClientIdIssuedAt() != null) {
            rp.setClientIdIssuedAt(policy.getClientIdIssuedAt().enforcePolicy(rp.getClientIdIssuedAt(), "ClientIdIssuedAt"));
        }

        if (policy.getClientName() != null) {
            rp.setClientName(policy.getClientName().enforcePolicy(rp.getClientName(), "ClientName"));
        }

        if (policy.getClientRegistrationTypes() != null) {
            rp.setClientRegistrationTypes(policy.getClientRegistrationTypes().enforcePolicy(rp.getClientRegistrationTypes(), "Client_registration_types"));
        }

        if (policy.getClientSecretExpiresAt() != null) {
            rp.setClientSecretExpiresAt(policy.getClientSecretExpiresAt().enforcePolicy(rp.getClientSecretExpiresAt(), "ClientSecretExpiresAt"));
        }

        if (policy.getClientUri() != null) {
            rp.setClientUri(policy.getClientUri().enforcePolicy(rp.getClientUri(), "ClientUri"));
        }

        if (policy.getContacts() != null) {
            rp.setContacts(policy.getContacts().enforcePolicy(rp.getContacts(), "Contacts"));
        }

        if (policy.getDefaultAcrValues() != null) {
            rp.setDefaultAcrValues(policy.getDefaultAcrValues().enforcePolicy(rp.getDefaultAcrValues(), "DefaultAcrValues"));
        }

        if (policy.getDefaultMaxAge() != null) {
            rp.setDefaultMaxAge(policy.getDefaultMaxAge().enforcePolicy(rp.getDefaultMaxAge(), "DefaultMaxAge"));
        }

        if (policy.getGrantTypes() != null) {
            rp.setGrantTypes(policy.getGrantTypes().enforcePolicy(rp.getGrantTypes(), "GrantTypes"));
        }

        if (policy.getIdTokenEncryptedResponseAlg() != null) {
            rp.setIdTokenEncryptedResponseAlg(policy.getIdTokenEncryptedResponseAlg().enforcePolicy(rp.getIdTokenEncryptedResponseAlg(), "IdTokenEncryptedResponseAlg"));
        }

        if (policy.getIdTokenEncryptedResponseEnc() != null) {
            rp.setIdTokenEncryptedResponseEnc(policy.getIdTokenEncryptedResponseEnc().enforcePolicy(rp.getIdTokenEncryptedResponseEnc(), "IdTokenEncryptedResponseEnc"));
        }

        if (policy.getIdTokenSignedResponseAlg() != null) {
            rp.setIdTokenSignedResponseAlg(policy.getIdTokenSignedResponseAlg().enforcePolicy(rp.getIdTokenSignedResponseAlg(), "IdTokenSignedResponseAlg"));
        }

        if (policy.getInitiateLoginUri() != null) {
            rp.setInitiateLoginUri(policy.getInitiateLoginUri().enforcePolicy(rp.getInitiateLoginUri(), "InitiateLoginUri"));
        }

        if (policy.getJwksUri() != null) {
            rp.setJwksUri(policy.getJwksUri().enforcePolicy(rp.getJwksUri(), "JwksUri"));
        }

        if (policy.getLogoUri() != null) {
            rp.setLogoUri(policy.getLogoUri().enforcePolicy(rp.getLogoUri(), "LogoUri"));
        }

        if (policy.getPolicyUri() != null) {
            rp.setPolicyUri(policy.getPolicyUri().enforcePolicy(rp.getPolicyUri(), "PolicyUri"));
        }

        if (policy.getPostLogoutRedirectUris() != null) {
            rp.setPostLogoutRedirectUris(policy.getPostLogoutRedirectUris().enforcePolicy(rp.getPostLogoutRedirectUris(), "PostLogoutRedirectUris"));
        }

        if (policy.getRedirectUris() != null) {
            rp.setRedirectUris(policy.getRedirectUris().enforcePolicy(rp.getRedirectUris(), "RedirectUris"));
        }

        if (policy.getRegistrationAccessToken() != null) {
            rp.setRegistrationAccessToken(policy.getRegistrationAccessToken().enforcePolicy(rp.getRegistrationAccessToken(), "RegistrationAccessToken"));
        }

        if (policy.getRegistrationClientUri() != null) {
            rp.setRegistrationClientUri(policy.getRegistrationClientUri().enforcePolicy(rp.getRegistrationClientUri(), "RegistrationClientUri"));
        }

        if (policy.getRequestObjectEncryptionAlg() != null) {
            rp.setRequestObjectEncryptionAlg(policy.getRequestObjectEncryptionAlg().enforcePolicy(rp.getRequestObjectEncryptionAlg(), "RequestObjectEncryptionAlg"));
        }

        if (policy.getRequestObjectEncryptionEnc() != null) {
            rp.setRequestObjectEncryptionEnc(policy.getRequestObjectEncryptionEnc().enforcePolicy(rp.getRequestObjectEncryptionEnc(), "RequestObjectEncryptionEnc"));
        }

        if (policy.getRequestObjectSigningAlg() != null) {
            rp.setRequestObjectSigningAlg(policy.getRequestObjectSigningAlg().enforcePolicy(rp.getRequestObjectSigningAlg(), "RequestObjectSigningAlg"));
        }

        if (policy.getRequestUris() != null) {
            rp.setRequestUris(policy.getRequestUris().enforcePolicy(rp.getRequestUris(), "RequestUris"));
        }

        if (policy.getRequireAuthTime() != null) {
            rp.setRequireAuthTime(policy.getRequireAuthTime().enforcePolicy(rp.getRequireAuthTime(), "RequireAuthTime"));
        }

        if (policy.getResponseTypes() != null) {
            rp.setResponseTypes(policy.getResponseTypes().enforcePolicy(rp.getResponseTypes(), "ResponseTypes"));
        }

        if (policy.getScope() != null) {
            rp.setScope(policy.getScope().enforcePolicy(rp.getScope(), "Scope"));
        }

        if (policy.getSectorIdentifierUri() != null) {
            rp.setSectorIdentifierUri(policy.getSectorIdentifierUri().enforcePolicy(rp.getSectorIdentifierUri(), "SectorIdentifierUri"));
        }

        if (policy.getSoftwareId() != null) {
            rp.setSoftwareId(policy.getSoftwareId().enforcePolicy(rp.getSoftwareId(), "SoftwareId"));
        }

        if (policy.getSoftwareVersion() != null) {
            rp.setSoftwareVersion(policy.getSoftwareVersion().enforcePolicy(rp.getSoftwareVersion(), "SoftwareVersion"));
        }

        if (policy.getSubjectType() != null) {
            rp.setSubjectType(policy.getSubjectType().enforcePolicy(rp.getSubjectType(), "SubjectType"));
        }

        if (policy.getTlsClientAuthSubjectDn() != null) {
            rp.setTlsClientAuthSubjectDn(policy.getTlsClientAuthSubjectDn().enforcePolicy(rp.getTlsClientAuthSubjectDn(), "TlsClientAuthSubjectDn"));
        }

        if (policy.getTlsClientCertificateBoundAccessTokens() != null) {
            rp.setTlsClientCertificateBoundAccessTokens(policy.getTlsClientCertificateBoundAccessTokens().enforcePolicy(rp.getTlsClientCertificateBoundAccessTokens(), "TlsClientCertificateBoundAccessTokens"));
        }

        if (policy.getTokenEndpointAuthMethod() != null) {
            rp.setTokenEndpointAuthMethod(policy.getTokenEndpointAuthMethod().enforcePolicy(rp.getTokenEndpointAuthMethod(), "TokenEndpointAuthMethod"));
        }

        if (policy.getTokenEndpointAuthSigningAlg() != null) {
            rp.setTokenEndpointAuthSigningAlg(policy.getTokenEndpointAuthSigningAlg().enforcePolicy(rp.getTokenEndpointAuthSigningAlg(), "TokenEndpointAuthSigningAlg"));
        }

        if (policy.getTosUri() != null) {
            rp.setTosUri(policy.getTosUri().enforcePolicy(rp.getTosUri(), "TosUri"));
        }

        if (policy.getUserinfoEncryptedResponseAlg() != null) {
            rp.setUserinfoEncryptedResponseAlg(policy.getUserinfoEncryptedResponseAlg().enforcePolicy(rp.getUserinfoEncryptedResponseAlg(), "UserinfoEncryptedResponseAlg"));
        }

        if (policy.getUserinfoEncryptedResponseEnc() != null) {
            rp.setUserinfoEncryptedResponseEnc(policy.getUserinfoEncryptedResponseEnc().enforcePolicy(rp.getUserinfoEncryptedResponseEnc(), "UserinfoEncryptedResponseEnc"));
        }

        if (policy.getUserinfoSignedResponseAlg() != null) {
            rp.setUserinfoSignedResponseAlg(policy.getUserinfoSignedResponseAlg().enforcePolicy(rp.getUserinfoSignedResponseAlg(), "UserinfoSignedResponseAlg"));
        }

        MetadataPolicy metadataPolicy = new MetadataPolicy();
        metadataPolicy.setRelyingPartyMetadataPolicy(policy);
        entity.setMetadataPolicy(metadataPolicy);
        return entity;
    }
}
