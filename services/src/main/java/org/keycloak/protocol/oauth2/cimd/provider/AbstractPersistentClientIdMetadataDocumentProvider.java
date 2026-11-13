package org.keycloak.protocol.oauth2.cimd.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.common.util.UriUtils;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.OAuth2DeviceConfig;
import org.keycloak.models.ParConfig;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.context.CimdClientRegisterContext;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.context.CimdClientRegisteredContext;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.context.CimdClientUpdateContext;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.context.CimdClientUpdatedContext;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.AbstractClientIdMetadataDocumentExecutor;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.mappers.AbstractPairwiseSubMapper;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;
import org.keycloak.protocol.oidc.mappers.SHA256PairwiseSubMapper;
import org.keycloak.protocol.oidc.utils.SubjectType;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientregistration.ErrorCodes;
import org.keycloak.services.clientregistration.oidc.DescriptionConverter;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.ClientResource;
import org.keycloak.services.util.CertificateInfoHelper;
import org.keycloak.validation.ClientValidationContext;
import org.keycloak.validation.ClientValidationProvider;
import org.keycloak.validation.ValidationContext;
import org.keycloak.validation.ValidationResult;

import org.jboss.logging.Logger;

/**
 * The abstract class persists client metadata.
 *
 * <p>Creating and updating a client metadata:
 * The class does almost the same process in Dynamic Client Registration (DCR)in {@code OIDCClientRegistrationProvider}.
 *
 * <p>The reason is that a client sends its metadata to an authorization server in DCR by RFC 7591
 * while an authorization server fetches a client metadata in CIMD,
 * which means that only the method of getting a client metadata is different.
 *
 * <p>The reason why not directly calling methods of {@code OIDCClientRegistrationProvider} is as follows:
 * <ul>
 *     <li>{@code client_id} property is not allowed in DCR while it is mandatory in CIMD.
 *     {@code OIDCClientRegistrationProvider} does not allow client metadata including {@code client_id}. </li>
 *     <li>A registration access token is issued in DCR
 *     (to say more precisely, RFC 7592 OAuth 2.0 Dynamic Client Registration Management Protocol) while it is not needed in CIMD.
 *     {@code OIDCClientRegistrationProvider} issues the registration access token.</li>
 * </ul>
 *
 * <p>Cache expiry time:
 * The provider stores the cache expiry time in an attribute of {@link ClientRepresentation}/{@link ClientModel}.
 *
 * <p>Process when a cache expires</p>
 * Do nothing. After keycloak supports workflow for clients, it would be used to delete a client metadata.
 *
 * <p>Roles of the abstract class and its concrete class:
 * The abstract class itself covers all about persisting a client metadata while the concrete class can see
 * a configuration of the concrete class of ({@link AbstractClientIdMetadataDocumentExecutor}) and augment a client metadata based on it.
 * Moreover, the concrete class can add or modify the abstract class, which makes it easy to implement custom persistent CIMD provider.
 *
 * 
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public abstract class AbstractPersistentClientIdMetadataDocumentProvider<CONFIG extends AbstractClientIdMetadataDocumentExecutor.Configuration> implements ClientIdMetadataDocumentProvider<CONFIG> {

    protected KeycloakSession session;
    protected CONFIG configuration;

    public static final String CIMD_CACHE_EXPIRY_TIME_IN_SEC = "cimd.cache.expiry.time.in.sec";

    protected abstract Logger getLogger();

    protected AbstractPersistentClientIdMetadataDocumentProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setCacheExpiryTimeToClientMetadata(ClientRepresentation clientRep, int cacheExpiryTimeInSec) {
        clientRep.getAttributes().put(CIMD_CACHE_EXPIRY_TIME_IN_SEC, Integer.toString(cacheExpiryTimeInSec));
    }

    @Override
    public void setCacheExpiryTimeToClientMetadata(ClientModel clientModel, int cacheExpiryTimeInSec) {
        clientModel.setAttribute(CIMD_CACHE_EXPIRY_TIME_IN_SEC, Integer.toString(cacheExpiryTimeInSec));
    }

    @Override
    public AbstractClientIdMetadataDocumentExecutor.FetchOperation determineFetchOperation(String clientId) {
        RealmModel realm = session.getContext().getRealm();
        ClientModel existingClientModel = realm.getClientByClientId(clientId);
        if (existingClientModel != null) {
            getLogger().debugv("client already exist: clientId = {0}", clientId);
            // Client Metadata Caching
            // if the client metadata remains effective, return
            // otherwise
            //   fetch Client ID Metadata
            //   Client Metadata verification
            //   Client Metadata validation
            //   Persist Client Metadata (overwrite)
            //  TODO: if an error occurs, the client metadata should be removed or remain persisted?
            //        -> it remains persisted. If client metadata removal by workflow is implemented, the client metadata is automatically removed.
            //        -> therefore, only returns an error response.
            if (existingClientModel.getAttribute(CIMD_CACHE_EXPIRY_TIME_IN_SEC) != null) {
                int i = Integer.parseInt(existingClientModel.getAttribute(CIMD_CACHE_EXPIRY_TIME_IN_SEC));
                if (Time.currentTime() > i) {
                    getLogger().debugv("client need to update: clientId = {0}", clientId);
                    return AbstractClientIdMetadataDocumentExecutor.FetchOperation.UPDATE;
                } else {
                    // persisted client metadata is still effective
                    getLogger().debugv("client no need to update: clientId = {0}", clientId);
                    return AbstractClientIdMetadataDocumentExecutor.FetchOperation.NO_UPDATE;
                }
            }
        }
        getLogger().debugv("client need to create: clientId = {0}", clientId);
        return AbstractClientIdMetadataDocumentExecutor.FetchOperation.CREATE;
    }

    @Override
    public ClientModel createClientMetadata(AbstractClientIdMetadataDocumentExecutor.OIDCClientRepresentationWithCacheControl clientOIDCWithCacheControl) throws ClientPolicyException {
        // do the same thing as in dynamic client registration except for:
        //   - not set client registration token
        RealmModel realm = session.getContext().getRealm();
        try {
            OIDCClientRepresentation clientOIDC = clientOIDCWithCacheControl.getOidcClientRepresentation();
            ClientRepresentation clientRep = DescriptionConverter.toInternal(session, clientOIDC);

            // set cache expiry time
            setCacheExpiryTimeToClientMetadata(clientRep, clientOIDCWithCacheControl.getClientMetadataCacheControl().getCacheExpiryTimeInSec());

            // augment client depending on the configuration of the CIMD executor
            augmentClientMetadata(clientRep);

            if (clientRep.getOptionalClientScopes() == null) {
                // an omitted scope grants no optional client scopes, the same as on a refresh
                clientRep.setOptionalClientScopes(new ArrayList<>());
                clientRep.setDefaultClientScopes(getRealmDefaultClientScopeNames(realm));
            } else if (clientRep.getDefaultClientScopes() == null) {
                clientRep.setDefaultClientScopes(List.of(OIDCLoginProtocolFactory.BASIC_SCOPE));
            }

            EventBuilder event = new EventBuilder(realm, session, session.getContext().getConnection());
            event.event(EventType.CLIENT_REGISTER);
            session.clientPolicy().triggerOnEvent(new CimdClientRegisterContext(clientRep));

            validateAuthorizationServices(clientRep);

            ClientModel clientModel = ClientManager.createClient(session, realm, clientRep);

            if (clientModel.isServiceAccountsEnabled()) {
                new ClientManager(new RealmManager(session)).enableServiceAccount(clientModel);
            }

            if (Boolean.TRUE.equals(clientRep.getAuthorizationServicesEnabled())) {
                RepresentationToModel.createResourceServer(clientModel, session, true);
            }

            session.getContext().setClient(clientModel);
            session.clientPolicy().triggerOnEvent(new CimdClientRegisteredContext(clientModel));

            clientRep = ModelToRepresentation.toRepresentation(clientModel, session);

            clientRep.setDirectAccessGrantsEnabled(clientModel.isDirectAccessGrantsEnabled());

            Stream<String> defaultRolesNames = getDefaultRolesStream(clientModel);
            if (defaultRolesNames != null) {
                clientRep.setDefaultRoles(defaultRolesNames.toArray(String[]::new));
            }

            event.client(clientRep.getClientId()).success();

            clientModel = realm.getClientByClientId(clientRep.getClientId());
            updatePairwiseSubMappers(clientModel, SubjectType.parse(clientOIDC.getSubjectType()), clientOIDC.getSectorIdentifierUri());
            updateClientRepWithProtocolMappers(clientModel, clientRep);

            validateClient(clientModel, clientOIDC, true);

            return clientModel;
        } catch (ModelDuplicateException e) {
            getLogger().warnv("ModelDuplicateException: {0}", e);
            rollback();
            throw new ClientPolicyException(ErrorCodes.INVALID_CLIENT_METADATA, "Client Identifier in use");
        } catch (ClientPolicyException e) {
            rollback();
            throw e; // intentionally
        } catch (Exception e) {
            getLogger().warnv("Exception: {0}", e);
            rollback();
            throw invalidClientMetadata("invalid request");
        }
    }

    @Override
    public ClientModel updateClientMetadata(AbstractClientIdMetadataDocumentExecutor.OIDCClientRepresentationWithCacheControl clientOIDCWithCacheControl) throws ClientPolicyException {
        // do the same thing as in dynamic client registration except for:
        //   - not set client registration token
        RealmModel realm = session.getContext().getRealm();

        try {
            OIDCClientRepresentation clientOIDC = clientOIDCWithCacheControl.getOidcClientRepresentation();
            ClientRepresentation clientRep = DescriptionConverter.toInternal(session, clientOIDC);
            String clientId = clientOIDC.getClientId();

            // set cache expiry time
            setCacheExpiryTimeToClientMetadata(clientRep, clientOIDCWithCacheControl.getClientMetadataCacheControl().getCacheExpiryTimeInSec());

            // augment client depending on configuration
            augmentClientMetadata(clientRep);

            EventBuilder event = new EventBuilder(realm, session, session.getContext().getConnection());
            event.event(EventType.CLIENT_UPDATE).client(clientId);

            ClientModel clientModel = realm.getClientByClientId(clientId);
            if (clientModel == null) {
                throw invalidClientMetadata("Client not found");
            }

            if (!clientModel.getClientId().equals(clientRep.getClientId())) {
                throw invalidClientMetadata("Client Identifier modified");
            }

            // The fetched client metadata replaces the persisted one as a whole.
            // The helpers below only update supplied values, so make the omitted values explicit.
            reconcileClientScopes(clientRep, clientModel);
            reconcileGrantTypes(clientOIDC, clientRep);
            reconcileWebOrigins(clientRep);
            // before the client policies, so that they validate the attributes that are actually persisted
            // instead of falling back to the persisted values of omitted attributes
            removeOmittedClientMetadataAttributes(clientRep);

            session.clientPolicy().triggerOnEvent(new CimdClientUpdateContext(clientRep, clientModel));

            validateAuthorizationServices(clientRep);

            RepresentationToModel.updateClient(clientRep, clientModel, session);
            RepresentationToModel.updateClientProtocolMappers(clientRep, clientModel);
            RepresentationToModel.updateClientScopes(clientRep, clientModel);
            // after the client scopes, because enabling or disabling the service account adds or removes the service_account client scope,
            // which updateClientScopes would otherwise revert to the client scopes taken before the update
            ClientResource.updateClientServiceAccount(session, clientModel, clientRep.isServiceAccountsEnabled());
            reconcileAuthorizationServices(clientRep, clientModel);

            clientRep = ModelToRepresentation.toRepresentation(clientModel, session);

            Stream<String> defaultRolesNames = getDefaultRolesStream(clientModel);
            if (defaultRolesNames != null) {
                clientRep.setDefaultRoles(defaultRolesNames.toArray(String[]::new));
            }

            event.client(clientRep.getClientId()).success();

            session.getContext().setClient(clientModel);
            session.clientPolicy().triggerOnEvent(new CimdClientUpdatedContext(clientModel));

            clientModel = realm.getClientByClientId(clientRep.getClientId());
            updatePairwiseSubMappers(clientModel, SubjectType.parse(clientOIDC.getSubjectType()), clientOIDC.getSectorIdentifierUri());
            updateClientRepWithProtocolMappers(clientModel, clientRep);

            validateClient(clientModel, clientOIDC, false);

            return clientModel;
        } catch (ClientPolicyException e) {
            rollback();
            throw e; // intentionally
        } catch (Exception e) {
            getLogger().warnv("Exception: {0}", e);
            rollback();
            throw invalidClientMetadata("invalid request");
        }
    }

    /**
     * Discards the client changes made so far, so that a failed creation or update is not partially persisted
     * (e.g. an updated cache expiry time preventing the client metadata from being fetched again).
     */
    private void rollback() {
        session.getTransactionManager().setRollbackOnly();
    }

    /**
     * Derives the web origins from the redirect URIs in the client metadata, in the same way as a newly created client.
     * Otherwise, the web origins derived from the previous redirect URIs would remain allowed for CORS.
     */
    protected void reconcileWebOrigins(ClientRepresentation clientRep) {
        if (clientRep.getWebOrigins() != null) {
            return;
        }
        List<String> redirectUris = Optional.ofNullable(clientRep.getRedirectUris()).orElse(List.of());
        clientRep.setWebOrigins(redirectUris.stream()
                .filter(uri -> uri != null && uri.startsWith("http"))
                .map(UriUtils::getOrigin)
                .distinct()
                .collect(Collectors.toList()));
    }

    /**
     * Keeps the default client scopes as they are and, when the client metadata omits {@code scope},
     * removes all optional client scopes, the same as for a newly created client.
     * Otherwise, optional client scopes removed from the client metadata would remain assigned to the client.
     */
    protected void reconcileClientScopes(ClientRepresentation clientRep, ClientModel clientModel) {
        clientRep.setDefaultClientScopes(new ArrayList<>(clientModel.getClientScopes(true).keySet()));
        if (clientRep.getOptionalClientScopes() == null) {
            clientRep.setOptionalClientScopes(new ArrayList<>());
        }
    }

    private static List<String> getRealmDefaultClientScopeNames(RealmModel realm) {
        return realm.getDefaultClientScopesStream(true)
                .filter(clientScope -> OIDCLoginProtocol.LOGIN_PROTOCOL.equals(clientScope.getProtocol()))
                .map(ClientScopeModel::getName)
                .collect(Collectors.toList());
    }

    /**
     * Rejects client metadata that includes the UMA grant without the {@code client_credentials} grant.
     * Authorization services need a service account to obtain a protection API token,
     * and the client metadata would not grant it.
     */
    protected void validateAuthorizationServices(ClientRepresentation clientRep) throws ClientPolicyException {
        if (Boolean.TRUE.equals(clientRep.getAuthorizationServicesEnabled()) && !Boolean.TRUE.equals(clientRep.isServiceAccountsEnabled())) {
            throw invalidClientMetadata(ERR_UMA_GRANT_WITHOUT_CLIENT_CREDENTIALS_GRANT);
        }
    }

    public static final String ERR_UMA_GRANT_WITHOUT_CLIENT_CREDENTIALS_GRANT = "Invalid Client Metadata: the UMA grant requires the client_credentials grant.";

    /**
     * When the client metadata omits {@code grant_types}, disables the capabilities that a newly created client does not get.
     * The grant-related attributes are removed by {@link #removeOmittedClientMetadataAttributes(ClientRepresentation)}.
     */
    protected void reconcileGrantTypes(OIDCClientRepresentation clientOIDC, ClientRepresentation clientRep) {
        if (clientOIDC.getGrantTypes() == null) {
            clientRep.setServiceAccountsEnabled(false);
            clientRep.setAuthorizationServicesEnabled(false);
        }
    }

    /**
     * Removes the client attributes derived from client metadata that the fetched client metadata no longer includes.
     * A {@code null} value makes {@link RepresentationToModel#updateClient} remove the attribute.
     */
    protected void removeOmittedClientMetadataAttributes(ClientRepresentation clientRep) {
        if (clientRep.getAttributes() == null) {
            clientRep.setAttributes(new HashMap<>());
        }
        Map<String, String> attributes = clientRep.getAttributes();
        CLIENT_METADATA_ATTRIBUTES.forEach(attribute -> attributes.putIfAbsent(attribute, null));
    }

    /**
     * Enables authorization services when the client metadata includes the UMA grant, and otherwise
     * removes the resource server and the {@code uma_protection} role.
     */
    protected void reconcileAuthorizationServices(ClientRepresentation clientRep, ClientModel clientModel) {
        if (!Profile.isFeatureEnabled(Profile.Feature.AUTHORIZATION)) {
            return;
        }
        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
        ResourceServerStore resourceServerStore = authorization.getStoreFactory().getResourceServerStore();
        boolean enabled = resourceServerStore.findByClient(clientModel) != null;

        if (Boolean.TRUE.equals(clientRep.getAuthorizationServicesEnabled())) {
            if (!enabled) {
                RepresentationToModel.createResourceServer(clientModel, session, true);
            }
            grantUmaProtectionRoleToServiceAccount(clientModel);
            return;
        }

        if (enabled) {
            resourceServerStore.delete(clientModel);
        }
        RoleModel umaProtectionRole = clientModel.getRole(Constants.AUTHZ_UMA_PROTECTION);
        if (umaProtectionRole != null) {
            clientModel.removeRole(umaProtectionRole);
        }
    }

    // createResourceServer grants the role only to an existing service account, and only when it creates the resource server
    private void grantUmaProtectionRoleToServiceAccount(ClientModel clientModel) {
        RoleModel umaProtectionRole = clientModel.getRole(Constants.AUTHZ_UMA_PROTECTION);
        if (umaProtectionRole == null) {
            umaProtectionRole = clientModel.addRole(Constants.AUTHZ_UMA_PROTECTION);
        }
        UserModel serviceAccount = session.users().getServiceAccount(clientModel);
        if (serviceAccount != null && !serviceAccount.hasDirectRole(umaProtectionRole)) {
            serviceAccount.grantRole(umaProtectionRole);
        }
    }

    // client attributes that DescriptionConverter derives from client metadata and does not always set
    // (attributes set to null by DescriptionConverter are not put into the representation while it has no attributes yet)
    private static final Set<String> CLIENT_METADATA_ATTRIBUTES = Set.of(
            // attributes DescriptionConverter sets to null when the client metadata omits them
            OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG,
            OIDCConfigAttributes.AUTHORIZATION_SIGNED_RESPONSE_ALG,
            OIDCConfigAttributes.AUTHORIZATION_ENCRYPTED_RESPONSE_ALG,
            OIDCConfigAttributes.AUTHORIZATION_ENCRYPTED_RESPONSE_ENC,
            OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG,
            OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL,
            OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI,
            // grant_types
            CibaConfig.OIDC_CIBA_GRANT_ENABLED,
            OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED,
            OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_ENABLED,
            OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_ENABLED,
            OIDCConfigAttributes.USE_REFRESH_TOKEN,
            // jwks, jwks_uri
            OIDCConfigAttributes.USE_JWKS_URL,
            OIDCConfigAttributes.JWKS_URL,
            OIDCConfigAttributes.USE_JWKS_STRING,
            OIDCConfigAttributes.JWKS_STRING,
            JWTClientAuthenticator.ATTR_PREFIX + "." + CertificateInfoHelper.PUBLIC_KEY,
            JWTClientAuthenticator.ATTR_PREFIX + "." + CertificateInfoHelper.KID,
            JWTClientAuthenticator.ATTR_PREFIX + "." + CertificateInfoHelper.X509CERTIFICATE,
            // userinfo, request object, id token
            OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG,
            OIDCConfigAttributes.USER_INFO_ENCRYPTED_RESPONSE_ALG,
            OIDCConfigAttributes.USER_INFO_ENCRYPTED_RESPONSE_ENC,
            OIDCConfigAttributes.REQUEST_OBJECT_SIGNATURE_ALG,
            OIDCConfigAttributes.ID_TOKEN_ENCRYPTED_RESPONSE_ALG,
            OIDCConfigAttributes.ID_TOKEN_ENCRYPTED_RESPONSE_ENC,
            OIDCConfigAttributes.REQUEST_URIS,
            // sender-constrained access tokens, tls_client_auth
            OIDCConfigAttributes.USE_MTLS_HOK_TOKEN,
            OIDCConfigAttributes.DPOP_BOUND_ACCESS_TOKENS,
            X509ClientAuthenticator.ATTR_SUBJECT_DN,
            X509ClientAuthenticator.ATTR_ALLOW_REGEX_PATTERN_COMPARISON,
            // logo_uri, policy_uri, tos_uri, sector_identifier_uri, post_logout_redirect_uris, default_acr_values
            OIDCConfigAttributes.LOGO_URI,
            OIDCConfigAttributes.POLICY_URI,
            OIDCConfigAttributes.TOS_URI,
            OIDCConfigAttributes.SECTOR_IDENTIFIER_URI,
            OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS,
            Constants.DEFAULT_ACR_VALUES,
            // CIBA, PAR
            CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE_PER_CLIENT,
            CibaConfig.CIBA_BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT,
            CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG,
            ParConfig.REQUIRE_PUSHED_AUTHORIZATION_REQUESTS
    );

    private static ClientPolicyException invalidClientMetadata(String errorDetail) {
        return new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, errorDetail);
    }

    // the same as AbstractClientRegistrationProvider.getDefaultRolesStream
    private Stream<String> getDefaultRolesStream(ClientModel client) {
        return client.getRealm().getDefaultRole().getCompositesStream()
                .filter(role -> role.isClientRole() && Objects.equals(role.getContainerId(), client.getId()))
                .map(RoleModel::getName);
    }

    // the same as OIDCClientRegistrationProvider.updatePairwiseSubMappers
    private void updatePairwiseSubMappers(ClientModel clientModel, SubjectType subjectType, String sectorIdentifierUri) {
        if (subjectType == SubjectType.PAIRWISE) {

            // See if we have existing pairwise mapper and update it. Otherwise create new
            AtomicBoolean foundPairwise = new AtomicBoolean(false);

            clientModel.getProtocolMappersStream().filter((ProtocolMapperModel mapping) -> {
                if (mapping.getProtocolMapper().endsWith(AbstractPairwiseSubMapper.PROVIDER_ID_SUFFIX)) {
                    foundPairwise.set(true);
                    return true;
                } else {
                    return false;
                }
            }).toList().forEach((ProtocolMapperModel mapping) -> {
                PairwiseSubMapperHelper.setSectorIdentifierUri(mapping, sectorIdentifierUri);
                clientModel.updateProtocolMapper(mapping);
            });

            // We don't have existing pairwise mapper. So create new
            if (!foundPairwise.get()) {
                ProtocolMapperRepresentation newPairwise = SHA256PairwiseSubMapper.createPairwiseMapper(sectorIdentifierUri, null);
                clientModel.addProtocolMapper(RepresentationToModel.toModel(newPairwise));
            }

        } else {
            // Rather find and remove all pairwise mappers
            clientModel.getProtocolMappersStream()
                    .filter(mapperRep -> mapperRep.getProtocolMapper().endsWith(AbstractPairwiseSubMapper.PROVIDER_ID_SUFFIX))
                    .toList()
                    .forEach(clientModel::removeProtocolMapper);
        }
    }

    // the same as OIDCClientRegistrationProvider.updateClientRepWithProtocolMappers
    private void updateClientRepWithProtocolMappers(ClientModel clientModel, ClientRepresentation rep) {
        List<ProtocolMapperRepresentation> mappings =
                clientModel.getProtocolMappersStream().map(ModelToRepresentation::toRepresentation).collect(Collectors.toList());
        rep.setProtocolMappers(mappings);
    }

    // the same as AbstractClientRegistrationProvider.updateClientRepWithProtocolMappers except for error handling
    private void validateClient(ClientModel client, OIDCClientRepresentation oidcClient, boolean create) throws ClientPolicyException{
        ClientValidationProvider provider = session.getProvider(ClientValidationProvider.class);
        if (provider != null) {
            ValidationContext.Event event = create ? ValidationContext.Event.CREATE : ValidationContext.Event.UPDATE;
            ValidationResult result;

            if (oidcClient != null) {
                result = provider.validate(new ClientValidationContext.OIDCContext(event, session, client, oidcClient));
            }
            else {
                result = provider.validate(new ClientValidationContext(event, session, client));
            }

            if (!result.isValid()) {
                getLogger().warnv("validateClient failed: {0}", result.getAllErrorsAsString());
                session.getTransactionManager().setRollbackOnly();
                throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "invalid request");
            }
        }
    }
}
