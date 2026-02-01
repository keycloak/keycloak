package org.keycloak.protocol.oauth2.cimd.clientpolicy.executor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oauth2.cimd.provider.ClientIdMetadataDocumentProvider;
import org.keycloak.protocol.oauth2.cimd.provider.PersistentClientIdMetadataDocumentProviderFactory;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.PreAuthorizationRequestContext;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.jboss.logging.Logger;

/**
 * The abstract class implements OAuth Client ID Metadata Document specification (Internet Draft v00).
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-client-id-metadata-document-00">OAuth Client ID Metadata Document (CIMD) [Internet Draft]]</a>
 *
 * <p>Moreover, the abstract class implements Authorization part of Model Context Protocol (MCP) specification (version 2025-11-25).
 * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/basic/authorization">Model Context Protocol (MCP) [2025-11-25]]</a>
 *
 * <p>The abstract class satisfies the following requirements of CIMD and MCP:
 * <ul>
 *     <li>Requirements whose requirement level is MUST or SHOULD.</li>
 *     <li>Requirements in Security Consideration.</li>
 * </ul>
 *
 * <p>The abstract class provides the following features:
 * <ul>
 *     <li>Client ID Verification: if {client_id} parameter satisfies the requirements of the specifications</li>
 *     <li>Client ID Validation: if {{client_id} parameter is valid according to the policy.</li>
 *     <li>Fetching Client Metadata: fetch a client metadata by accessing {client_id} URL.</li>
 *     <li>Client Metadata Verification: if a client metadata satisfies the requirements of the specifications.</li>
 *     <li>Client Metadata Validation: if a client metadata is valid according to the policy.</li>
 *     <li>Client Metadata Augmentation in {@link OIDCClientRepresentation}: augment a fetched client metadata.</li>
 * </ul>
 *
 * <p>Roles of the abstract class and its concrete class:
 * The abstract class covers the basic checks and processes by following the CIMD and MCP specifications while
 * the concrete class of the abstract class provides additional checks or processes.
 *
 * <p>For example, regarding Client ID Validation and Client Metadata Validation, the CIMD and MCP specifications allow
 * an authorization server to implement policies that determine the valid {client_id} parameter value and client metadata.
 * The CIMD and MCP specification show some examples of the policies roughly, but what policies are implemented in detail
 * is up to the authorization server implementation.
 * Therefore, the abstract class provides some of the examples and the concrete class can implement additional policies.
 *
 * <p>Client Metadata Caching:
 * The abstract class does not treat the following processes. It delegates them to {@link ClientIdMetadataDocumentProvider}:
 * <ul>
 *     <li>determining if (re-)fetching a client metadata is needed</li>
 *     <li>concrete process of caching a client metadata: create and update</li>
 *     <li>update cache expiry time</li>
 *     <li>augment a client metadata in {@code ClientRepresentation}</li>
 * </ul>
 * For example, {@code PersistentClientIdMetadataDocumentProvider} persists client metadata.
 * In the future, the provider for non-persisting a client metadata can be provided.
 *
 * <p>Client Metadata Format:
 * According to the CIMD specification, the client metadata format is the same as for Dynamic Client Registration except for {@code client_id} property.
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7591>OAuth 2.0 Dynamic Client Registration Protocol [RFC 7591]]</a>
 * Therefore, {@link OIDCClientRepresentation} is used for the client metadata format.
 * The CIMD specification allows the use of additional properties (MAY requirement level), but the class does not treat them.
 *
 * <p>Client Metadata Augmentation in {@code OIDCClientRepresentation}:
 * To successfully convert a fetched client metadata to {@code ClientRepresentation}, intentionally augment it.
 * The actual example is a public client. The CIMD and MCP specification allows a public client.
 * {@code }DescriptionConverter.toInternal} recognize a client as a public client if token_endpoint_auth_method is "none"
 * If a client metadata lacks token_endpoint_auth_method, it is converted to "none", meaning it is treated as a public client.
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public abstract class AbstractClientIdMetadataDocumentExecutor<CONFIG extends AbstractClientIdMetadataDocumentExecutor.Configuration> implements ClientPolicyExecutorProvider<CONFIG> {

    protected final KeycloakSession session;
    protected CONFIG configuration;
    protected ClientIdMetadataDocumentProvider<CONFIG> provider;

    protected abstract Logger getLogger();

    protected AbstractClientIdMetadataDocumentExecutor(KeycloakSession session) {
        this.session = session;
    }

    protected ClientIdMetadataDocumentProvider<CONFIG> getProvider() {
        return provider;
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

        // Client ID Metadata Document Provider Name
        @JsonProperty(AbstractClientIdMetadataDocumentExecutorFactory.CIMD_PROVIDER_NAME)
        protected String cimdProviderName = PersistentClientIdMetadataDocumentProviderFactory.PROVIDER_ID;

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

        public String getCimdProviderName() {
            return cimdProviderName;
        }

        public void setCimdProviderName(String cimdProviderName) {
            this.cimdProviderName = cimdProviderName;
        }
    }

    /**
     * The CIMD and MCP specification requires an authorization server to cache a client metadata.
     * The inner class put together a client metadata and Cache-Control header's value.
     */
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

    /**
     * The CIMD and MCP specification requires an authorization server to cache a client metadata.
     * The inner class covers the Cache-Control header's directives:
     * <ul>
     *     <li>only consider directives of a response.</li>
     *     <li>do not consider whether private or public.</li>
     *     <li>consider max-age and s-maxage directives showing the lifetime of client metadata.</li>
     *     <li>s-maxage takes precedence over max-age</li>
     *     <li>consider no-cache and no-store directives showing no caching client metadata.</li>
     *     <li>do not consider other directives.</li>
     * </ul>
     */
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

    /**
     * CREATE: a client metadata is not created, so fetching it creating it is needed.
     * UPDATE: a client metadata has been already created but it has expired, so re-fetching it and updating it is needed.
     * NO_UPDATE: a client metadata has been already created and it does not expire, so fetching it is not needed.
     */
    public enum FetchOperation {
        CREATE,
        UPDATE,
        NO_UPDATE
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
        provider = session.getProvider(ClientIdMetadataDocumentProvider.class, getConfiguration().getCimdProviderName());
        provider.setConfiguration(getConfiguration());

        String clientId = preAuthorizationRequestContext.getClientId();

        // Authorization Request verification
        URI redirectUriURI = verifyAuthorizationRequest(preAuthorizationRequestContext);

        // Client ID verification
        URI clientIdURI = verifyClientId(clientId);

        // Client ID validation
        validateClientId(clientIdURI);

        // determine if (re-)fetching a client metadata is needed
        FetchOperation fetchOp = provider.determineFetchOperation(clientId);
        if (fetchOp == FetchOperation.NO_UPDATE) {
            // no update
            return;
        }

        // fetch Client ID Metadata
        OIDCClientRepresentationWithCacheControl clientOIDCWithCacheControl = fetchClientMetadata(clientIdURI, fetchOp == FetchOperation.UPDATE, provider);
        if (clientOIDCWithCacheControl == null) {
            // fetched but no update
            return;
        }

        // Client Metadata verification
        verifyClientMetadata(clientIdURI, redirectUriURI, clientOIDCWithCacheControl.getOidcClientRepresentation());

        // Client Metadata validation
        validateClientMetadata(clientIdURI, redirectUriURI, clientOIDCWithCacheControl.getOidcClientRepresentation());

        if (fetchOp == FetchOperation.CREATE) {
            // Update Client Metadata
            provider.createClientMetadata(clientOIDCWithCacheControl);
        } else if (fetchOp == FetchOperation.UPDATE) {
            // Create Client Metadata
            provider.updateClientMetadata(clientOIDCWithCacheControl);
        }
    }

    // Authorization Request Verification Errors
    public static final String ERR_INVALID_PARAMETER = "Invalid Authorization Request: it does not include redirect_uri parameter";

    // Client ID Verification Errors
    public static final String ERR_CLIENTID_MALFORMED_URL = "Invalid Client ID: malformed URL.";
    public static final String ERR_CLIENTID_INVALID_SCHEME = "Invalid Client ID: invalid scheme.";
    public static final String ERR_CLIENTID_EMPTY_PATH = "Invalid Client ID: empty path.";
    public static final String ERR_CLIENTID_PATH_TRAVERSAL = "Invalid Client ID: path traverse segment included.";
    public static final String ERR_CLIENTID_FRAGMENT = "Invalid Client ID: fragment included.";
    public static final String ERR_CLIENTID_USERINFO = "Invalid Client ID: userinfo included.";
    public static final String ERR_CLIENTID_QUERY = "Invalid Client ID: query included.";
    public static final String ERR_CLIENTID_UNRESOLVED = "Invalid Client ID: host unresolved.";
    public static final String ERR_CLIENTID_LOOPBACK_ADDRESS = "Invalid Client ID: loopback address is not allowed.";
    public static final String ERR_CLIENTID_PRIVATE_ADDRESS = "Invalid Client ID: private address is not allowed.";
    //public static final String ERR_CLIENTID_LINKLOCAL_ADDRESS = "Invalid Client ID: link local address is not allowed.";

    // Client ID Validation Errors
    public static final String ERR_NOTALLOWED_DOMAIN = "Invalid Client ID: domain not allowed.";

    // Client Metadata Verification Errors
    public static final String ERR_METADATA_NOCONTENT = "Invalid Client Metadata: no content.";
    public static final String ERR_METADATA_NOCLIENTID = "Invalid Client Metadata: no client_id.";
    public static final String ERR_METADATA_CLIENTID_UNMATCH = "Invalid Client Metadata: client_id property does not exactly match client_id parameter.";
    public static final String ERR_METADATA_NOTALLOWED_CLIENTAUTH = "Invalid Client Metadata: token_endpoint_auth_method property in client metadata is not-allowed authentication method..";
    public static final String ERR_METADATA_CLIENTSECRET = "Invalid Client Metadata: client_secret or client_secret_expires_at property in client metadata is must not included.";
    public static final String ERR_METADATA_REDIRECTURI = "Invalid Client Metadata: redirect_uri parameter does not exactly match the one of redirect_uris property in client metadata.";
    public static final String ERR_METADATA_MALFORMED_URL = "Invalid Client Metadata: malformed URL.";
    public static final String ERR_METADATA_UNRESOLVED = "Invalid Client Metadata: URL unresolved.";
    public static final String ERR_METADATA_LOOPBACK_ADDRESS = "Invalid Client Metadata: loopback address is not allowed.";
    public static final String ERR_METADATA_PRIVATE_ADDRESS = "Invalid Client Metadata: private address is not allowed.";
    //public static final String ERR_METADATA_LINKLOCAL_ADDRESS = "Invalid Clent Metadata: link local address is not allowed.";

    // Client Metadata Validation Errors
    public static final String ERR_METADATA_URIS_SAMEDOMAIN = "Invalid Client Metadata: client_id parameter, redirect_uri parameter and at least one of redirect_uris properties in client metadata should be under the same domain.";
    public static final String ERR_METADATA_NO_REQUIRED_PROPERTIES = "Invalid Client Metadata: it does not include all required properties.";

    // Implementation
    // Fetch Client Metadata
    public static final String ERR_METADATA_FETCH_FAILED = "Client Metadata fetch failed";

    /**
     * Verifies an authorization request to check if the request includes required parameters and follows the expected format.
     *
     * @param preAuthorizationRequestContext an authorization request
     * @return {@code URI} {@code redirect_uri} parameter value as {@link URI}
     * @throws ClientPolicyException when verification of an authorization request fails.
     */
    protected URI verifyAuthorizationRequest(PreAuthorizationRequestContext preAuthorizationRequestContext) throws ClientPolicyException {
        if (preAuthorizationRequestContext.getRequestParameters() == null) {
            getLogger().warn("authorization request does not include any parameter.");
            throw invalidClientId(ERR_INVALID_PARAMETER);
        }

        if (preAuthorizationRequestContext.getRequestParameters().getFirst(OIDCLoginProtocol.CLIENT_ID_PARAM) == null) {
            getLogger().warn("authorization request does not include client_id.");
            throw invalidClientId(ERR_INVALID_PARAMETER);
        }

        String redirectUri = preAuthorizationRequestContext.getRequestParameters().getFirst(OIDCLoginProtocol.REDIRECT_URI_PARAM);
        if (redirectUri == null) {
            getLogger().warn("authorization request does not include redirect_uri parameter.");
            throw invalidClientId(ERR_INVALID_PARAMETER);
        }

        final URI uri;
        try {
            uri = new URI(redirectUri);
        } catch (URISyntaxException e) {
            getLogger().warnv("Malformed URL: redirectUri = {0}", redirectUri);
            throw invalidClientId(ERR_INVALID_PARAMETER);
        }

        return uri;
    }

    /**
     * Verifies a value of {@code client_id} parameter of an authorization request
     * to check if the value satisfies the requirements of the CIMD and MCP specifications.
     *
     * @param clientId a value of {@code client_id} parameter of an authorization request
     * @return {@code URI} {@code client_uri} parameter value as {@link URI}
     * @throws ClientPolicyException when verification of an authorization request fails.
     */
    protected URI verifyClientId(final String clientId) throws ClientPolicyException {
        getLogger().debugv("verifyClientId: clientId = {0}", clientId);

        // Client identifier MUST be a URL.
        final URI uri;
        try {
            uri = new URI(clientId);
        } catch (URISyntaxException e) {
            getLogger().warnv("Malformed URL: clientId = {0}", clientId);
            throw invalidClientId(ERR_CLIENTID_MALFORMED_URL);
        }

        // Client identifier URLs MUST have an "https" scheme.
        if (!getConfiguration().isAllowHttpScheme() && !"https".equals(uri.getScheme())) {
            getLogger().warnv("Invalid URL Scheme: scheme = {0}", uri.getScheme());
            throw invalidClientId(ERR_CLIENTID_INVALID_SCHEME);
        }

        // Client identifier URLs MUST contain a path component.
        if (uri.getPath() == null || uri.getPath().isEmpty()) {
            getLogger().warn("Empty path:");
            throw invalidClientId(ERR_CLIENTID_EMPTY_PATH);
        }

        // Client identifier URLs MUST NOT contain single-dot or double-dot path segments.
        if (isUnsafeUriPath(uri)) {
            getLogger().warnv("traverse path segment: raw path = {0}", uri.getRawPath());
            throw invalidClientId(ERR_CLIENTID_PATH_TRAVERSAL);
        }

        // Client identifier URLs MUST NOT contain a fragment component.
        if (uri.getFragment() != null) {
            getLogger().warnv("url fragment: fragment = {0}", uri.getFragment());
            throw invalidClientId(ERR_CLIENTID_FRAGMENT);
        }

        // Client identifier URLs MUST NOT contain a username or password.
        if (uri.getUserInfo() != null) {
            getLogger().warnv("user information: userinfo = {0}", uri.getUserInfo());
            throw invalidClientId(ERR_CLIENTID_USERINFO);
        }

        // Client identifier URLs SHOULD NOT include a query string component.
        if (uri.getQuery() != null) {
            getLogger().warnv("url query: query = {0}", uri.getQuery());
            throw invalidClientId(ERR_CLIENTID_QUERY);
        }

        // Client identifier URLs MAY contain a port.
        // -> no check, a port is allowed.

        // A short Client identifier URL is RECOMMENDED.
        // -> no check

        // A stable URL that does not frequently change for the client is RECOMMENDED.
        // -> no check

        // SSRF attack countermeasure
        // Client identifier is not a loopback address
        // Client identifier is not a private address
        // TODO for integration test, loopback address should be allowed, so its configuration is needed like SecureRedirectUrisEnforcerExecutor
        InetAddress addr;
        try {
            addr = InetAddress.getByName(uri.getHost());
        } catch (UnknownHostException e) {
            getLogger().warnv("unknown host: host = {0}", uri.getHost());
            throw invalidClientId(ERR_CLIENTID_UNRESOLVED);
        }
        if (!getConfiguration().isAllowLoopbackAddress() && addr.isLoopbackAddress()) {
            getLogger().warnv("loopback address: host = {0}", uri.getHost());
            throw invalidClientId(ERR_CLIENTID_LOOPBACK_ADDRESS);
        }
        if (!getConfiguration().isAllowPrivateAddress() && addr.isSiteLocalAddress()) {
            getLogger().warnv("private address: address = {0}", addr.toString());
            throw invalidClientId(ERR_CLIENTID_PRIVATE_ADDRESS);
        }
        //if (addr.isLinkLocalAddress()) {
        //    getLogger().warnv("link local address: address = {0}", addr.toString());
        //    throw invalidClientId(ERR_CLIENTIDLINKLOCAL_ADDRESS);
        //}

        return uri;
    }

    /**
     * Validate a value of {@code client_id} parameter of an authorization request
     * to check if the value meets the policies.
     *
     * @param clientIdURI a value of {@code client_id} parameter of an authorization request in {@link URI}
     * @throws ClientPolicyException when validation of an authorization request fails.
     */
    protected void validateClientId(final URI clientIdURI) throws ClientPolicyException {
        // The authorization server MAY choose to have its own heuristics and policies around the trust of domain names used as client IDs.

        // allow trusted domain policy
        List<String> allowList = convertContentFilledList(getConfiguration().getAllowPermittedDomains());
        if (allowList != null && !allowList.isEmpty()) {
            if (allowList.stream().noneMatch(i->checkTrustedDomain(clientIdURI.getHost(), i))) {
                getLogger().warnv("not allowed domain: host = {0}", clientIdURI.getHost());
                throw invalidClientId(ERR_NOTALLOWED_DOMAIN);
            }
        }
    }

    // apply the same logic in TrustedHostClientRegistrationPolicy.
    protected boolean checkTrustedDomain(String hostname, String trustedDomain) {
        if (trustedDomain.startsWith("*.")) {
            String domain = trustedDomain.substring(2);
            return hostname.equals(domain) || hostname.endsWith("." + domain);
        }
        return hostname.equals(trustedDomain);
    }

    /**
     * fetch a client metadata and update cache expiry time if the client metadata has been already created.
     *
     * @param clientIdURI a value of {@code client_id} parameter of an authorization request in {@link URI}
     * @param isUpdate indicates the client metadata has been already created
     * @param provider {@link ClientIdMetadataDocumentProvider} for updating cache expiry time
     * @return {@code OIDCClientRepresentationWithCacheControl} a combination of a client metadata and Cache-Control header value accompanied by the metadata response.
     * {@code null} if a client metadata was re-fetched but the HTTP response status code is 307 Not Modified.
     * @throws ClientPolicyException when fetching a client metadata fails.
     */
    protected OIDCClientRepresentationWithCacheControl fetchClientMetadata(final URI clientIdURI, final boolean isUpdate,
                                                                           ClientIdMetadataDocumentProvider provider) throws ClientPolicyException {
        String clientId = clientIdURI.toString();

        SimpleHttpRequest simpleHttp = SimpleHttp.create(session).doGet(clientId);

        OIDCClientRepresentation clientOIDC;
        try (SimpleHttpResponse response = simpleHttp.asResponse()) {
            if (!isUpdate && response.getStatus() != Response.Status.OK.getStatusCode()) {
                getLogger().warnv("fetching client metadata for the first time failed: clientId = {0}", clientId);
                throw invalidClientId(ERR_METADATA_FETCH_FAILED);
            }
            if (isUpdate && response.getStatus() != Response.Status.OK.getStatusCode() && response.getStatus() != Response.Status.NOT_MODIFIED.getStatusCode()) {
                getLogger().warnv("fetching client metadata for updating failed: clientId = {0}", clientId);
                throw invalidClientId(ERR_METADATA_FETCH_FAILED);
            }

            Header[] headers = response.getAllHeaders();
            String headerKey = Arrays.stream(headers).map(NameValuePair::getName).filter(HttpHeaders.CACHE_CONTROL::equalsIgnoreCase).findFirst().orElse(null); // both header and value are case-insensitive
            String cacheControlHeaderValue = headerKey != null ? Arrays.stream(headers).filter(i->headerKey.equals(i.getName())).findFirst().get().getValue() : null;
            ClientMetadataCacheControl clientMetadataCacheControl = new ClientMetadataCacheControl(cacheControlHeaderValue);

            if (isUpdate) {
                // it is better to compare the fetched client metadata with the existing client metadata.
                // however, it is difficult to do that because the existing client metadata included additional properties when it was registered.
                // therefore, such the comparing is not executed.
                if (response.getStatus() == Response.Status.NOT_MODIFIED.getStatusCode()) {
                    ClientModel clientModel = session.getContext().getRealm().getClientByClientId(clientId);
                    // update cache expiry time
                    provider.setCacheExpiryTimeToClientMetadata(clientModel, clientMetadataCacheControl.getCacheExpiryTimeInSec());
                    return null;
                }
            }

            clientOIDC = response.asJson(OIDCClientRepresentation.class);

            // to successfully convert it to Client Representation, intentionally augment it.
            augmentClientOIDC(clientOIDC);

            return new OIDCClientRepresentationWithCacheControl(clientOIDC, cacheControlHeaderValue);
        } catch (IOException e) {
            getLogger().warnv("HTTP connection failure: {0}", e);
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, ERR_METADATA_FETCH_FAILED);
        }
    }

    /**
     * Verify a client metadata to check if it satisfies the requirements of the CIMD and MCP specifications.
     *
     * @param clientIdURI a value of {client_id} parameter of an authorization request in {@link URI}
     * @param redirectUriURI a value of {redirect_uri} parameter of an authorization request in {@link URI}
     * @param clientOIDC a client metadata
     * @return {@code URI} {@code client_id} property of a client metadata in {@link URI}
     * @throws ClientPolicyException when verifying a client metadata fails.
     */
    protected URI verifyClientMetadata(final URI clientIdURI, final URI redirectUriURI, final OIDCClientRepresentation clientOIDC) throws ClientPolicyException {
        String clientId = clientIdURI.toString();
        String redirectUri = redirectUriURI.toString();

        if (clientOIDC == null) {
            getLogger().warn("client metadata does not have its content.");
            throw invalidClientId(ERR_METADATA_NOCONTENT);
        }

        // The client metadata document MUST contain a client_id property.
        if (clientOIDC.getClientId() == null) {
            getLogger().warn("client metadata does not include client_id property.");
            throw invalidClientId(ERR_METADATA_NOCLIENTID);
        }

        // The client_id property's value MUST match the URL of the document
        // using simple string comparison as defined in [RFC3986] Section 6.2.1.
        if (!clientOIDC.getClientId().equals(clientId)) {
            getLogger().warnv("client_id property in client metadata does not exactly match client_id parameter in authorization request. property = {0}, parameter = {1}", clientOIDC.getClientId(), clientId);
            throw invalidClientId(ERR_METADATA_CLIENTID_UNMATCH);
        }

        // The token_endpoint_auth_method property MUST NOT include
        // client_secret_post, client_secret_basic, client_secret_jwt,
        // or any other method based around a shared symmetric secret.
        if (clientOIDC.getTokenEndpointAuthMethod() != null && NOTALLOWED_ALGORITHMS.contains(clientOIDC.getTokenEndpointAuthMethod())) {
            getLogger().warnv("not allowed client auth method: token_endpoint_auth_method = {0}", clientOIDC.getTokenEndpointAuthMethod());
            throw invalidClientId(ERR_METADATA_NOTALLOWED_CLIENTAUTH);
        }

        // The client_secret and client_secret_expires_at properties MUST NOT be used.
        if (clientOIDC.getClientSecret() != null || clientOIDC.getClientSecretExpiresAt() != null) {
            getLogger().warn("client metadata includes client_secret or client_secret_expires_at.");
            throw invalidClientId(ERR_METADATA_CLIENTSECRET);
        }

        // An authorization server MUST validate redirect URIs presented in an authorization request
        // against those in the metadata document.
        if (clientOIDC.getRedirectUris() == null || !clientOIDC.getRedirectUris().contains(redirectUri)) {
            getLogger().warnv("redirect_uri parameter does not exactly match the one of redirect_uris property in client metadata: redirectUri = {0}", redirectUri);
            throw invalidClientId(ERR_METADATA_REDIRECTURI);
        }

        // SSRF attack countermeasure
        // It checks if an address resolved from a property whose value is URI is loopback address.
        // It checks if an address resolved from a property whose value is URI is private address.
        // RFC 7591: logo_uri, client_uri, tos_uri, policy_uri, jwks_uri.
        if (clientOIDC.getLogoUri() != null) {
            verifyUri(clientOIDC.getLogoUri(), (error, logMessageTemplate) -> {
                getLogger().warnv(logMessageTemplate, "logo_uri", clientOIDC.getLogoUri());
                throw invalidClientId(error);
            });
        }
        if (clientOIDC.getClientUri() != null) {
            verifyUri(clientOIDC.getClientUri(), (error, logMessageTemplate) -> {
                getLogger().warnv(logMessageTemplate, "client_uri", clientOIDC.getClientUri());
                throw invalidClientId(error);
            });
        }
        if (clientOIDC.getTosUri() != null) {
            verifyUri(clientOIDC.getTosUri(), (error, logMessageTemplate) -> {
                getLogger().warnv(logMessageTemplate, "tos_uri", clientOIDC.getTosUri());
                throw invalidClientId(error);
            });
        }
        if (clientOIDC.getPolicyUri() != null) {
            verifyUri(clientOIDC.getPolicyUri(), (error, logMessageTemplate) -> {
                getLogger().warnv(logMessageTemplate, "policy_uri", clientOIDC.getPolicyUri());
                throw invalidClientId(error);
            });
        }
        if (clientOIDC.getJwksUri() != null) {
            verifyUri(clientOIDC.getJwksUri(), (error, logMessageTemplate) -> {
                getLogger().warnv(logMessageTemplate, "jwks_uri", clientOIDC.getJwksUri());
                throw invalidClientId(error);
            });
        }

        URI clientIdURIfromMetadata;
        try {
            clientIdURIfromMetadata = new URI(clientOIDC.getClientId());
        } catch (URISyntaxException e) {
            // never reach here
            getLogger().warnv("Malformed URL: clientId in metadata = {0}", clientOIDC.getClientId());
            throw invalidClientId(ERR_CLIENTID_MALFORMED_URL);
        }

        return clientIdURIfromMetadata;
    }

    // any access to parent folder /../ or current /./ is unsafe with or without encoding
    private final static Pattern UNSAFE_PATH_PATTERN = Pattern.compile(
            "(/|%2[fF]|%5[cC]|\\\\)(%2[eE]|\\.){1,2}(/|%2[fF]|%5[cC]|\\\\)|(/|%2[fF]|%5[cC]|\\\\)(%2[eE]|\\.){1,2}$");

    private boolean isUnsafeUriPath(URI redirectUri) {
        return UNSAFE_PATH_PATTERN.matcher(redirectUri.getRawPath()).find();
    }

    private void verifyUri(String uriString, ErrorHandler errorHandler) throws ClientPolicyException {
        final URI uri;
        try {
            uri = new URI(uriString);
        } catch (URISyntaxException e) {
            errorHandler.onError(ERR_METADATA_MALFORMED_URL, "Malformed URL: {0} property in metadata = {1}");
            return;
        }
        InetAddress addr;
        try {
            addr = InetAddress.getByName(uri.getHost());
        } catch (UnknownHostException e) {
            errorHandler.onError(ERR_METADATA_UNRESOLVED, "Unresolved URL: {0} property in metadata = {1}");
            return;
        }
        if (!getConfiguration().isAllowLoopbackAddress() && addr.isLoopbackAddress()) {
            errorHandler.onError(ERR_METADATA_LOOPBACK_ADDRESS, "loopback address: {0} property in metadata = {1}");
            return;
        }
        if (!getConfiguration().isAllowPrivateAddress() && addr.isSiteLocalAddress()) {
            errorHandler.onError(ERR_METADATA_PRIVATE_ADDRESS, "private address: {0} property in metadata = {1}");
            return;
        }
        if (addr.isLinkLocalAddress()) {
            errorHandler.onError(ERR_METADATA_PRIVATE_ADDRESS, "link local address: {0} property in metadata = {1}");
        }
    }

    public interface ErrorHandler {
        void onError(String error, String logMessageTemplate) throws ClientPolicyException;
    }

    /**
     * Validate a client metadata to check if the value meets the policies.
     *
     * @param clientIdURI a value of {client_id} parameter of an authorization request in {@link URI}
     * @param redirectUriURI a value of {redirect_uri} parameter of an authorization request in {@link URI}
     * @param clientOIDC a client metadata
     * @throws ClientPolicyException when validating a client metadata fails.
     */
    protected void validateClientMetadata(final URI clientIdURI, final URI redirectUriURI, final OIDCClientRepresentation clientOIDC) throws ClientPolicyException {
        // An authorization server MAY impose restrictions or relationships
        // between the redirect_uris and the client_id or client_uri properties

        // same domain policy
        List<String> trustedDomains = convertContentFilledList(getConfiguration().getAllowPermittedDomains());
        if (getConfiguration().isRestrictSameDomain() && trustedDomains != null && !trustedDomains.isEmpty()) {
            // Client Metadata verification ensures that
            //  - client_id parameter value in an authorization request exactly matches client_id property in metadata
            //  - redirect_uri parameter value in an authorization request exactly matches one of client_uris property in metadata
            // Therefore, only considering domain parts of client_id parameter value, redirect_uri parameter value matches one of permitted domains configuration.
            if (trustedDomains.stream().noneMatch(i->checkTrustedDomain(clientIdURI.getHost(), i) && checkTrustedDomain(redirectUriURI.getHost(), i))) {
                getLogger().warnv("client_id and redirect_uri domain not match: client_id host part = {0}, redirect_uri host part = {1}", clientIdURI.getHost(), redirectUriURI.getHost());
                throw invalidClientId(ERR_METADATA_URIS_SAMEDOMAIN);
            }
        }

        // required properties policy
        List<String> requiredProperties = convertContentFilledList(getConfiguration().getRequiredProperties());
        if (requiredProperties != null && !requiredProperties.isEmpty()) {
            JsonNode jn = JsonSerialization.writeValueAsNode(clientOIDC);
            if (requiredProperties.stream().filter(i->!i.isBlank()).anyMatch(i->jn.get(i) == null)) {
                getLogger().warn("metadata does not include required properties");
                throw invalidClientId(ERR_METADATA_NO_REQUIRED_PROPERTIES);
            }
        }
    }

    // to accept a public client in CIMD, intentionally "none" is not included
    protected static final Set<String> NOTALLOWED_ALGORITHMS = new LinkedHashSet<>(Arrays.asList(
            OIDCLoginProtocol.CLIENT_SECRET_POST,
            OIDCLoginProtocol.CLIENT_SECRET_BASIC,
            OIDCLoginProtocol.CLIENT_SECRET_JWT
    ));

    protected List<String> convertContentFilledList(List<String> list) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list.stream().filter(Objects::nonNull).filter(i->!i.isBlank()).distinct().toList();
    }

    // to successfully convert it to Client Representation, intentionally augment it.

    /**
     * Augments a re-fetched client metadata to successfully convert it to {@code ClientRepresentation}.
     *
     * @param oidcClient a fetched client metadata
     */
    protected void augmentClientOIDC(OIDCClientRepresentation oidcClient) {
        // Allowing a public client:
        // DescriptionConverter.toInternal recognize a client as a public client if token_endpoint_auth_method is "none"
        // If a client metadata lacks token_endpoint_auth_method, it is converted to "none", meaning it is treated as a public client.
        if (oidcClient.getTokenEndpointAuthMethod() == null) {
            oidcClient.setTokenEndpointAuthMethod("none");
        }
    }

    protected static ClientPolicyException invalidClientId(String errorDetail) {
        return new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, errorDetail);
    }

}
