package org.keycloak.services.clientpolicy.executor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.PreAuthorizationRequestContext;
import org.keycloak.services.clientregistration.ErrorCodes;
import org.keycloak.services.clientregistration.oidc.DescriptionConverter;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.ClientResource;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public abstract class AbstractClientIdMetadataDocumentExecutor<CONFIG extends AbstractClientIdMetadataDocumentExecutor.Configuration> implements ClientPolicyExecutorProvider<CONFIG> {

    protected final KeycloakSession session;
    protected CONFIG configuration;

    abstract protected Logger getLogger();

    protected AbstractClientIdMetadataDocumentExecutor(KeycloakSession session) {
        this.session = session;
    }

    public CONFIG getConfiguration() {
        return configuration;
    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {
        // Client ID Verification
        @JsonProperty(AbstractClientIdMetadataDocumentExecutorFactory.ALLOW_PRIVATE_ADDRESS)
        protected boolean allowPrivateAddress = false;
        @JsonProperty(AbstractClientIdMetadataDocumentExecutorFactory.ALLOW_LOOPBACK_ADDRESS)
        protected boolean allowLoopbackAddress = false;
        @JsonProperty(AbstractClientIdMetadataDocumentExecutorFactory.ALLOW_HTTP_SCHEME)
        protected boolean allowHttpScheme = false;

        // Client ID Validation
        @JsonProperty(AbstractClientIdMetadataDocumentExecutorFactory.ALLOW_PERMITTED_DOMAINS)
        protected List<String> allowPermittedDomains = null;

        // Client Metadata Validation
        @JsonProperty(AbstractClientIdMetadataDocumentExecutorFactory.RESTRICT_SAME_DOMAIN)
        protected boolean restrictSameDomain = false;
        @JsonProperty(AbstractClientIdMetadataDocumentExecutorFactory.REQUIRED_PROPERTIES)
        protected List<String> requiredProperties = null;
        @JsonProperty(AbstractClientIdMetadataDocumentExecutorFactory.CONSENT_REQUIRED)
        protected boolean consentRequired = true;
        @JsonProperty(AbstractClientIdMetadataDocumentExecutorFactory.FULL_SCOPE_DISABLED)
        protected boolean fullScopeDisabled = true;

        public Configuration() {
        }

        public boolean isAllowPrivateAddress() {
            return allowPrivateAddress;
        }

        public void setAllowPrivateAddress(boolean allowPrivateAddress) {
            this.allowPrivateAddress = allowPrivateAddress;
        }

        public boolean isAllowLoopbackAddress() {
            return allowLoopbackAddress;
        }

        public void setAllowLoopbackAddress(boolean allowLoopbackAddress) {
            this.allowLoopbackAddress = allowLoopbackAddress;
        }

        public boolean isAllowHttpScheme() {
            return allowHttpScheme;
        }

        public void setAllowHttpScheme(boolean allowHttpScheme) {
            this.allowHttpScheme = allowHttpScheme;
        }

        public List<String> getAllowPermittedDomains() {
            return allowPermittedDomains;
        }

        public void setAllowPermittedDomains(List<String> permittedDomains) {
            this.allowPermittedDomains = permittedDomains;
        }

        public boolean isRestrictSameDomain() {
            return restrictSameDomain;
        }

        public void setRestrictSameDomain(boolean restrictSameDomain) {
            this.restrictSameDomain = restrictSameDomain;
        }

        public List<String> getRequiredProperties() {
            return requiredProperties;
        }

        public void setRequiredProperties(List<String> requiredProperties) {
            this.requiredProperties = requiredProperties;
        }

        public boolean isConsentRequired() {
            return consentRequired;
        }

        public void setConsentRequired(boolean consentRequired) {
            this.consentRequired = consentRequired;
        }

        public boolean isFullScopeDisabled() {
            return fullScopeDisabled;
        }

        public void setFullScopeDisabled(boolean fullScopeDisabled) {
            this.fullScopeDisabled = fullScopeDisabled;
        }
    }

    public static class OIDCClientRepresentationWithCacheControl {
        private final OIDCClientRepresentation oidcClientRepresentation;
        private final ClientMetadataCacheControl clientMetadataCacheControl;

        public OIDCClientRepresentationWithCacheControl(OIDCClientRepresentation oidcClientRepresentation, String rawCacheControlHeaderValue) {
            this.oidcClientRepresentation = oidcClientRepresentation;
            this.clientMetadataCacheControl = new ClientMetadataCacheControl(rawCacheControlHeaderValue);
        }

        public OIDCClientRepresentation getOidcClientRepresentation() {
            return oidcClientRepresentation;
        }

        public ClientMetadataCacheControl getClientMetadataCacheControl() {
            return clientMetadataCacheControl;
        }
    }

    public static class ClientMetadataCacheControl {
        private boolean noCache = false;
        private boolean noStore = false;
        private int maxAgeValue = -1;
        private int sMaxAgeValue = -1;
        private final String normalizedCacheControlHeaderValue;

        public ClientMetadataCacheControl(String rawCacheControlHeaderValue) {
            if (rawCacheControlHeaderValue == null) {
                normalizedCacheControlHeaderValue = null;
            } else {
                normalizedCacheControlHeaderValue = rawCacheControlHeaderValue.toLowerCase().replaceAll("\\s", "");
                List<String> sList  = Arrays.asList(normalizedCacheControlHeaderValue.split(",", 0));
                if (sList.contains("no-cache")) {
                    noCache = true;
                }
                if (sList.contains("no-store")) {
                    noStore = true;
                }
                if (sList.stream().filter(i->i.startsWith("max-age=")).count() == 1) {
                    maxAgeValue = deriveExpiryValue("max-age", sList);
                }
                if (sList.stream().filter(i->i.startsWith("s-maxage=")).count() == 1) {
                    sMaxAgeValue = deriveExpiryValue("s-maxage", sList);
                }
            }
        }

        public boolean isNoCache() {
            return noCache;
        }

        public boolean isNoStore() {
            return noStore;
        }

        public boolean isMaxAge() {
            return maxAgeValue >= 0;
        }

        public int getMaxAgeValue() {
            return maxAgeValue;
        }

        public boolean isSmaxAge() {
            return sMaxAgeValue >= 0;
        }

        public int getSmaxAgeValue() {
            return sMaxAgeValue;
        }

        public final String getNormalizedCacheControlHeaderValue() {
            return normalizedCacheControlHeaderValue;
        }

        public int getCacheExpiryTimeInSec() {
            if (isNoCache() || isNoStore()) {
                return Time.currentTime();
            }
            if (isSmaxAge()) { // s-maxage takes precedence over max-age
                return Time.currentTime() + getSmaxAgeValue();
            }
            if (isMaxAge()) {
                return Time.currentTime() + getMaxAgeValue();
            }
            return Time.currentTime();
        }

        private int deriveExpiryValue(final String key, List<String> sList) {
            String[] sAry = sList.stream().filter(i->i.startsWith(key + "=")).findFirst().get().split("=");
            try {
                if (sAry.length == 2) {
                    return Integer.parseInt(sAry[1]);
                }
            } catch (NumberFormatException e) {
                // no-op
            }
            return -1;
        }
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        if (!Profile.isFeatureEnabled(Profile.Feature.CIMD)) {
            getLogger().warnf("CIMD executor is used, but CIMD feature is disabled. So CIMD is not enforced for the clients. " +
                    "Please enable CIMD feature in order to be able to have CIMD applied.");
            return;
        }

        switch (context.getEvent()) {
            case PRE_AUTHORIZATION_REQUEST:
                PreAuthorizationRequestContext preAuthorizationRequestContext = (PreAuthorizationRequestContext)context;
                process(preAuthorizationRequestContext);
                break;
            default:
        }
    }

    private void process(PreAuthorizationRequestContext preAuthorizationRequestContext) throws ClientPolicyException {
        String clientId = preAuthorizationRequestContext.getClientId();

        // Authorization Request verification
        URI redirectUriURI = verifyAuthorizationRequest(preAuthorizationRequestContext);

        // Client ID verification
        URI clientIdURI = verifyClientId(clientId);

        // Client ID validation
        validateClientId(clientIdURI);

        // check if the client has already been registered
        RealmModel realm = session.getContext().getRealm();
        boolean isUpdate = false;
        ClientModel cm = realm.getClientByClientId(clientId);
        if (cm != null) {
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
            if (cm.getAttribute(CIMD_CACHE_EXPIRY_TIME_IN_SEC) != null) {
                int i = Integer.parseInt(cm.getAttribute(CIMD_CACHE_EXPIRY_TIME_IN_SEC));
                if (Time.currentTime() > i) {
                    isUpdate = true;
                } else {
                    // persisted client metadata is still effective
                    getLogger().debugv("client no need to update: clientId = {0}", clientId);
                    return;
                }
            }
        }

        // fetch Client ID Metadata
        OIDCClientRepresentationWithCacheControl clientOIDCWithCacheControl = fetchClientMetadata(clientIdURI);

        // Client Metadata verification
        URI clientIdURIfromMetadata = verifyClientMetadata(clientIdURI, redirectUriURI, clientOIDCWithCacheControl.getOidcClientRepresentation());

        // Client Metadata validation
        validateClientMetadata(clientIdURI, redirectUriURI, clientOIDCWithCacheControl.getOidcClientRepresentation());

        ClientModel clientModel;
        if (isUpdate) {
            // Update Persisted Client Metadata
            clientModel = updateClientMetadata(realm, clientOIDCWithCacheControl);
        } else {
            // Persist Client Metadata
            clientModel = persistClientMetadata(realm, clientOIDCWithCacheControl);
        }
    }

    // Authorization Request Verification Errors
    public static final String ERR_INVALID_PARAMETER = "Invalid Authorization Request: it does not include redirect_uri parameter";

    // Client ID Verification Errors
    public static final String ERR_MALFORMED_URL = "Invalid Client ID: malformed URL.";
    public static final String ERR_INVALID_URL_SCHEME = "Invalid Client ID: invalid scheme.";
    public static final String ERR_EMPTY_URL_PATH = "Invalid Client ID: empty path.";
    public static final String ERR_URL_PATH_TRAVERSAL = "Invalid Client ID: path traverse segment included.";
    public static final String ERR_URL_FRAGMENT = "Invalid Client ID: fragment included.";
    public static final String ERR_URL_USERINFO = "Invalid Client ID: userinfo included.";
    public static final String ERR_URL_QUERY = "Invalid Client ID: query included.";
    public static final String ERR_HOST_UNRESOLVED = "Invalid Client ID: host unresolved.";
    public static final String ERR_LOOPBACK_ADDRESS = "Invalid Client ID: loopback address is not allowed.";
    public static final String ERR_PRIVATE_ADDRESS = "Invalid Client ID: private address is not allowed.";
    //public static final String ERR_LINKLOCAL_ADDRESS = "Invalid Client ID: link local address is not allowed.";

    // Client ID Validation Errors
    public static final String ERR_NOTALLOWED_DOMAIN = "Invalid Client ID: domain not allowed.";

    // Client Metadata Verification Errors
    public static final String ERR_METADATA_NOCONTENT = "Invalid Client Metadata: no content.";
    public static final String ERR_METADATA_NOCLIENTID = "Invalid Client Metadata: no client_id.";
    public static final String ERR_METADATA_CLIENTID_UNMATCH = "Invalid Client Metadata: client_id property does not exactly match client_id parameter.";
    public static final String ERR_METADATA_NOTALLOWED_CLIENTAUTH = "Invalid Client Metadata: token_endpoint_auth_method property in client metadata is not-allowed authentication method..";
    public static final String ERR_METADATA_CLIENTSECRET = "Invalid Client Metadata: client_secret or client_secret_expires_at property in client metadata is must not included.";
    public static final String ERR_METADATA_REDIRECTURI = "Invalid Client Metadata: redirect_uri parameter does not exactly match the one of redirect_uris property in client metadata.";

    // Client Metadata Validation Errors
    public static final String ERR_METADATA_URIS_SAMEDOMAIN = "Invalid Client Metadata: client_id parameter, redirect_uri parameter and at least one of redirect_uris properties in client metadata should be under the same domain.";
    public static final String ERR_METADATA_NO_REQUIRED_PROPERTIES = "Invalid Client Metadata: it does not include all required properties.";

    // Persist ClientMetadata
    public static final String CIMD_CACHE_EXPIRY_TIME_IN_SEC = "cimd.cache.expiry.time.in.sec";

    protected URI verifyAuthorizationRequest(PreAuthorizationRequestContext preAuthorizationRequestContext) throws ClientPolicyException {
        if (preAuthorizationRequestContext.getRequestParameters() == null) {
            getLogger().debug("authorization request does not include any parameter.");
            throw invalidClientId(ERR_INVALID_PARAMETER);
        }

        if (preAuthorizationRequestContext.getRequestParameters().getFirst(OIDCLoginProtocol.CLIENT_ID_PARAM) == null) {
            getLogger().debug("authorization request does not include client_id.");
            throw invalidClientId(ERR_INVALID_PARAMETER);
        }

        String redirectUri = preAuthorizationRequestContext.getRequestParameters().getFirst(OIDCLoginProtocol.REDIRECT_URI_PARAM);
        if (redirectUri == null) {
            getLogger().debug("authorization request does not include redirect_uri parameter.");
            throw invalidClientId(ERR_INVALID_PARAMETER);
        }

        final URI uri;
        try {
            uri = new URI(redirectUri);
        } catch (URISyntaxException e) {
            getLogger().debugv("Malformed URL: redirectUri = {0}", redirectUri);
            throw invalidClientId(ERR_INVALID_PARAMETER);
        }

        return uri;
    }

    protected URI verifyClientId(final String clientId) throws ClientPolicyException {
        getLogger().debugv("verifyClientId: clientId = {0}", clientId);

        // Client identifier MUST be a URL.
        final URI uri;
        try {
            uri = new URI(clientId);
        } catch (URISyntaxException e) {
            getLogger().debugv("Malformed URL: clientId = {0}", clientId);
            throw invalidClientId(ERR_MALFORMED_URL);
        }

        // Client identifier URLs MUST have an "https" scheme.
        if (!getConfiguration().isAllowHttpScheme() && !"https".equals(uri.getScheme())) {
            getLogger().debugv("Invalid URL Scheme: scheme = {0}", uri.getScheme());
            throw invalidClientId(ERR_INVALID_URL_SCHEME);
        }

        // Client identifier URLs MUST contain a path component.
        if (uri.getPath() == null || uri.getPath().isEmpty()) {
            getLogger().debug("Empty path:");
            throw invalidClientId(ERR_EMPTY_URL_PATH);
        }

        // Client identifier URLs MUST NOT contain single-dot or double-dot path segments.
        if (uri.getRawPath().contains("/./") || uri.getRawPath().contains("/../")) {
            getLogger().debugv("traverse path segment: raw path = {0}", uri.getRawPath());
            throw invalidClientId(ERR_URL_PATH_TRAVERSAL);
        }

        // Client identifier URLs MUST NOT contain a fragment component.
        if (uri.getFragment() != null) {
            getLogger().debugv("url fragment: fragment = {0}", uri.getFragment());
            throw invalidClientId(ERR_URL_FRAGMENT);
        }

        // Client identifier URLs MUST NOT contain a username or password.
        if (uri.getUserInfo() != null) {
            getLogger().debugv("user information: userinfo = {0}", uri.getUserInfo());
            throw invalidClientId(ERR_URL_USERINFO);
        }

        // Client identifier URLs SHOULD NOT include a query string component.
        if (uri.getQuery() != null) {
            getLogger().debugv("url query: query = {0}", uri.getQuery());
            throw invalidClientId(ERR_URL_QUERY);
        }

        // Client identifier URLs MAY contain a port.
        // -> no check, a port is allowed.

        // A short Client identifier URL is RECOMMENDED.
        // -> no check

        // A stable URL that does not frequently change for the client is RECOMMENDED.
        // -> no check

        // Client identifier is not a loopback address
        // Client identifier is not a private address
        // TODO for integration test, loopback address should be allowed, so its configuration is needed like SecureRedirectUrisEnforcerExecutor
        InetAddress addr;
        try {
            addr = InetAddress.getByName(uri.getHost());
        } catch (UnknownHostException e) {
            getLogger().debugv("unknown host: host = {0}", uri.getHost());
            throw invalidClientId(ERR_HOST_UNRESOLVED);
        }
        if (!getConfiguration().isAllowLoopbackAddress() && addr.isLoopbackAddress()) {
            getLogger().debugv("ipv4 loopback address: host = {0}", uri.getHost());
            throw invalidClientId(ERR_LOOPBACK_ADDRESS);
        }
        if (!getConfiguration().isAllowPrivateAddress() && addr.isSiteLocalAddress()) {
            getLogger().debugv("private address: address = {0}", addr.toString());
            throw invalidClientId(ERR_PRIVATE_ADDRESS);
        }
        //if (addr.isLinkLocalAddress()) {
        //    getLogger().debugv("link local address: address = {0}", addr.toString());
        //    throw invalidClientId(ERR_LINKLOCAL_ADDRESS);
        //}

        return uri;
    }

    protected void validateClientId(final URI clientIdURI) throws ClientPolicyException {
        // The authorization server MAY choose to have its own heuristics and policies
        // around the trust of domain names used as client IDs.
        List<String> allowList = getConfiguration().getAllowPermittedDomains();
        if (allowList != null && !allowList.isEmpty()) {
            if (allowList.stream().noneMatch(i->checkTrustedDomain(clientIdURI.getHost(), i))) {
                getLogger().debugv("not allowed domain: host = {0}", clientIdURI.getHost());
                throw invalidClientId(ERR_NOTALLOWED_DOMAIN);
            }
        }
    }

    private boolean checkTrustedDomain(String hostname, String trustedDomain) {
        if (trustedDomain.startsWith("*.")) {
            String domain = trustedDomain.substring(2);
            return hostname.equals(domain) || hostname.endsWith("." + domain);
        }
        return hostname.equals(trustedDomain);
    }

    protected OIDCClientRepresentationWithCacheControl fetchClientMetadata(final URI clientIdURI) throws ClientPolicyException {
        String clientId = clientIdURI.toString();

        SimpleHttpRequest simpleHttp = SimpleHttp.create(session).doGet(clientId);

        OIDCClientRepresentation clientOIDC;
        try (SimpleHttpResponse response = simpleHttp.asResponse()) {
            clientOIDC = response.asJson(OIDCClientRepresentation.class);
            Header[] headers = response.getAllHeaders();
            String headerKey = Arrays.stream(headers).map(NameValuePair::getName).filter(HttpHeaders.CACHE_CONTROL::equalsIgnoreCase).findFirst().orElse(null); // both header and value are case-insensitive
            String cacheControlHeaderValue = headerKey != null ? Arrays.stream(headers).filter(i->headerKey.equals(i.getName())).findFirst().get().getValue() : null;
            return new OIDCClientRepresentationWithCacheControl(clientOIDC, cacheControlHeaderValue);
        } catch (IOException e) {
            getLogger().warnv("HTTP connection failure: {0}", e);
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "checking intent bound with client failed");
        }
    }

    protected URI verifyClientMetadata(final URI clientIdURI, final URI redirectUriURI, final OIDCClientRepresentation clientOIDC) throws ClientPolicyException {
        String clientId = clientIdURI.toString();
        String redirectUri = redirectUriURI.toString();

        if (clientOIDC == null) {
            getLogger().debug("client metadata does not have its content.");
            throw invalidClientId(ERR_METADATA_NOCONTENT);
        }

        // The client metadata document MUST contain a client_id property.
        if (clientOIDC.getClientId() == null) {
            getLogger().debug("client metadata does not include client_id property.");
            throw invalidClientId(ERR_METADATA_NOCLIENTID);
        }

        // The client_id property's value MUST match the URL of the document
        // using simple string comparison as defined in [RFC3986] Section 6.2.1.
        if (!clientOIDC.getClientId().equals(clientId)) {
            getLogger().debugv("client_id property in client metadata does not exactly match client_id parameter in authorization request. property = {0}, parameter = {1}", clientOIDC.getClientId(), clientId);
            throw invalidClientId(ERR_METADATA_CLIENTID_UNMATCH);
        }

        // The token_endpoint_auth_method property MUST NOT include
        // client_secret_post, client_secret_basic, client_secret_jwt,
        // or any other method based around a shared symmetric secret.
        if (clientOIDC.getTokenEndpointAuthMethod() != null && !ALLOWED_ALGORITHMS.contains(clientOIDC.getTokenEndpointAuthMethod())) {
            getLogger().debugv("not allowed client auth method: token_endpoint_auth_method = {0}", clientOIDC.getTokenEndpointAuthMethod());
            throw invalidClientId(ERR_METADATA_NOTALLOWED_CLIENTAUTH);
        }

        // The client_secret and client_secret_expires_at properties MUST NOT be used.
        if (clientOIDC.getClientSecret() != null || clientOIDC.getClientSecretExpiresAt() != null) {
            getLogger().debug("client metadata includes client_secret or client_secret_expires_at.");
            throw invalidClientId(ERR_METADATA_CLIENTSECRET);
        }

        // An authorization server MUST validate redirect URIs presented in an authorization request
        // against those in the metadata document.
        if (clientOIDC.getRedirectUris() == null || !clientOIDC.getRedirectUris().contains(redirectUri)) {
            getLogger().debugv("redirect_uri parameter does not exactly match the one of redirect_uris property in client metadata: redirectUri = {0}", redirectUri);
            throw invalidClientId(ERR_METADATA_REDIRECTURI);
        }

        URI clientIdURIfromMetadata;
        try {
            clientIdURIfromMetadata = new URI(clientOIDC.getClientId());
        } catch (URISyntaxException e) {
            // never reach here
            getLogger().debugv("Malformed URL: clientId in metadata = {0}", clientOIDC.getClientId());
            throw invalidClientId(ERR_MALFORMED_URL);
        }

        return clientIdURIfromMetadata;
    }

    protected void validateClientMetadata(final URI clientIdURI, final URI redirectUriURI, final OIDCClientRepresentation clientOIDC) throws ClientPolicyException {
        // An authorization server MAY impose restrictions or relationships
        // between the redirect_uris and the client_id or client_uri properties
        //
        // same domain
        List<String> trustedDomains = getConfiguration().getAllowPermittedDomains();
        if (getConfiguration().isRestrictSameDomain() && !Optional.ofNullable(trustedDomains).orElse(Collections.emptyList()).isEmpty()) {
            // Client Metadata verification ensures that
            //  - client_id parameter value in an authorization request exactly matches client_id property in metadata
            //  - redirect_uri parameter value in an authorization request exactly matches one of client_uris property in metadata
            // Therefore, only considering domain parts of client_id parameter value, redirect_uri parameter value matches one of permitted domains configuration.
            if (trustedDomains.stream().noneMatch(i->checkTrustedDomain(clientIdURI.getHost(), i) && checkTrustedDomain(redirectUriURI.getHost(), i))) {
                getLogger().debugv("client_id and redirect_uri domain not match: client_id host part = {0}, redirect_uri host part = {1}", clientIdURI.getHost(), redirectUriURI.getHost());
                throw invalidClientId(ERR_METADATA_URIS_SAMEDOMAIN);
            }
        }

        // required properties
        List<String> requiredProperties = getConfiguration().getRequiredProperties();
        if (!Optional.ofNullable(requiredProperties).orElse(Collections.emptyList()).isEmpty()) {
            JsonNode jn = JsonSerialization.writeValueAsNode(clientOIDC);
            if (requiredProperties.stream().anyMatch(i->jn.get(i) == null)) {
                getLogger().debug("metadata does not include required properties");
                throw invalidClientId(ERR_METADATA_NO_REQUIRED_PROPERTIES);
            }
        }

    }

    protected ClientModel persistClientMetadata(RealmModel realm, OIDCClientRepresentationWithCacheControl clientOIDCWithCacheControl) throws ClientPolicyException {
        // do the same thing as in dynamic client registration
        try {
            ClientRepresentation clientRep = DescriptionConverter.toInternal(session, clientOIDCWithCacheControl.getOidcClientRepresentation());

            // set cache expiry time
            clientRep.getAttributes().put(CIMD_CACHE_EXPIRY_TIME_IN_SEC, Integer.toString(clientOIDCWithCacheControl.getClientMetadataCacheControl().getCacheExpiryTimeInSec()));

            if (clientRep.getOptionalClientScopes() != null && clientRep.getDefaultClientScopes() == null) {
                clientRep.setDefaultClientScopes(List.of(OIDCLoginProtocolFactory.BASIC_SCOPE));
            }

            clientRep.setConsentRequired(getConfiguration().isConsentRequired());
            clientRep.setFullScopeAllowed(!getConfiguration().isFullScopeDisabled());

            EventBuilder event = new EventBuilder(realm, session, session.getContext().getConnection());
            event.event(EventType.CLIENT_REGISTER);

            ClientModel clientModel = ClientManager.createClient(session, realm, clientRep);

            if (clientRep.getDefaultRoles() != null) {
                for (String name : clientRep.getDefaultRoles()) {
                    addDefaultRole(clientModel, name);
                }
            }

            if (clientModel.isServiceAccountsEnabled()) {
                new ClientManager(new RealmManager(session)).enableServiceAccount(clientModel);
            }

            if (Boolean.TRUE.equals(clientRep.getAuthorizationServicesEnabled())) {
                RepresentationToModel.createResourceServer(clientModel, session, true);
            }

            session.getContext().setClient(clientModel);

            clientRep = ModelToRepresentation.toRepresentation(clientModel, session);

            clientRep.setDirectAccessGrantsEnabled(clientModel.isDirectAccessGrantsEnabled());

            Stream<String> defaultRolesNames = getDefaultRolesStream(clientModel);
            if (defaultRolesNames != null) {
                clientRep.setDefaultRoles(defaultRolesNames.toArray(String[]::new));
            }

            event.client(clientRep.getClientId()).success();

            return realm.getClientByClientId(clientRep.getClientId());
        } catch (ModelDuplicateException e) {
            getLogger().warnv("ModelDuplicateException: {0}", e);
            throw new ClientPolicyException(ErrorCodes.INVALID_CLIENT_METADATA, "Client Identifier in use");
        } catch (Exception e) {
            getLogger().warnv("Exception: {0}", e);
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "invalid request");
        }
    }

    protected ClientModel updateClientMetadata(RealmModel realm, OIDCClientRepresentationWithCacheControl clientOIDCWithCacheControl) throws ClientPolicyException {
        // do the same thing as in dynamic client registration
        try {
            OIDCClientRepresentation clientOIDC = clientOIDCWithCacheControl.getOidcClientRepresentation();
            ClientRepresentation clientRep = DescriptionConverter.toInternal(session, clientOIDC);
            String clientId = clientOIDC.getClientId();

            if (clientOIDC.getScope() != null) {
                ClientModel oldClient = realm.getClientByClientId(clientId);
                Collection<String> defaultClientScopes = oldClient.getClientScopes(true).keySet();
                clientRep.setDefaultClientScopes(new ArrayList<>(defaultClientScopes));
            }

            EventBuilder event = new EventBuilder(realm, session, session.getContext().getConnection());
            event.event(EventType.CLIENT_UPDATE).client(clientId);

            ClientModel clientModel = realm.getClientByClientId(clientId);

            if (!clientModel.getClientId().equals(clientRep.getClientId())) {
                throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Client Identifier modified");
            }

            ClientResource.updateClientServiceAccount(session, clientModel, clientRep.isServiceAccountsEnabled());
            RepresentationToModel.updateClient(clientRep, clientModel, session);
            RepresentationToModel.updateClientProtocolMappers(clientRep, clientModel);
            RepresentationToModel.updateClientScopes(clientRep, clientModel);

            clientRep = ModelToRepresentation.toRepresentation(clientModel, session);

            Stream<String> defaultRolesNames = getDefaultRolesStream(clientModel);
            if (defaultRolesNames != null) {
                clientRep.setDefaultRoles(defaultRolesNames.toArray(String[]::new));
            }

            event.client(clientRep.getClientId()).success();

            session.getContext().setClient(clientModel);

            return realm.getClientByClientId(clientRep.getClientId());
        } catch (Exception e) {
            getLogger().warnv("Exception: {0}", e);
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "invalid request");
        }
    }

    protected static final Set<String> ALLOWED_ALGORITHMS = new LinkedHashSet<>(Arrays.asList(
            OIDCLoginProtocol.PRIVATE_KEY_JWT,
            OIDCLoginProtocol.TLS_CLIENT_AUTH
    ));

    protected static final Set<String> NOTALLOWED_ALGORITHMS = new LinkedHashSet<>(Arrays.asList(
            OIDCLoginProtocol.CLIENT_SECRET_POST,
            OIDCLoginProtocol.CLIENT_SECRET_BASIC,
            OIDCLoginProtocol.CLIENT_SECRET_JWT,
            "none"
    ));

    protected static ClientPolicyException invalidClientId(String errorDetail) {
        return new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, errorDetail);
    }

    private void addDefaultRole(ClientModel client, String name) {
        client.getRealm().getDefaultRole().addCompositeRole(getOrAddRoleId(client, name));
    }

    private RoleModel getOrAddRoleId(ClientModel client, String name) {
        RoleModel role = client.getRole(name);
        if (role == null) {
            role = client.addRole(name);
        }
        return role;
    }

    private Stream<String> getDefaultRolesStream(ClientModel client) {
        return client.getRealm().getDefaultRole().getCompositesStream()
                .filter(role -> role.isClientRole() && Objects.equals(role.getContainerId(), client.getId()))
                .map(RoleModel::getName);
    }
}
