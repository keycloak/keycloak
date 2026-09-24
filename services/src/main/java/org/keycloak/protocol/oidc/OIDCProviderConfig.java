package org.keycloak.protocol.oidc;

import java.util.Map;

import org.keycloak.Config;
import org.keycloak.cache.LocalCache;

/**
 * @author <a href="mailto:patrick.weiner@prime-sign.com">Patrick Weiner</a>
 */
public class OIDCProviderConfig {

    private final Config.Scope config;

    // Populated by the constructor, either with the bounded LocalCaches created by
    // OIDCLoginProtocolFactory#postInit() (once a KeycloakSession is available to look up the LocalCacheProvider),
    // or - for the single-argument constructor used by unit tests constructing this class directly, bypassing the
    // factory lifecycle - with a no-op cache that never caches, so getMaxLengthForTheParameter() always resolves
    // the value directly.
    //
    // Kept as two separate caches (rather than one cache keyed by a "paramName + isTokenParam" composite key) so
    // that a lookup can use paramName directly as the cache key, without allocating a new String on every call,
    // including cache hits.
    private final LocalCache<String, Integer> reqParamMaxLengthCache;
    private final LocalCache<String, Integer> tokenParamMaxLengthCache;

    /**
     * Maximum default length of the standard OIDC parameter sent to the OIDC authentication or token request.
     */
    public static final int DEFAULT_REQ_PARAMS_DEFAULT_MAX_SIZE = 4000;

    private final int reqParamsDefaultMaxSize;

    /**
     * Overriden values for maximum sizes of specified standard OIDC parameters. The value for the specified parameter can be still overriden
     * by administrator in the configuration of the {@link OIDCLoginProtocolFactory}. In case that value is not overriden in the configuration or in this map,
     * then the value specified by the {@link OIDCLoginProtocolFactory#CONFIG_OIDC_REQ_PARAMS_DEFAULT_MAX_SIZE} is used
     */
    private Map<String, Integer> DEFAULT_MAX_PARAMS_SIZES = Map.of(
            OIDCLoginProtocol.LOGIN_HINT_PARAM, 255 // Aligned with user-profile configuration for username and email
    );

    /**
     * Maximum default length of the standard OIDC parameter sent to the OIDC token request in case the parameter is "token" parameter.
     * As "token" parameter is considered a parameter containing long token (for example JWT or SAML assertion) with unbounded data (For example possibly big amount of roles inside JWT).
     * Applies for example for parameters like "subject_token" sent in case of token exchange grant.
     */
    public static final int DEFAULT_REQ_TOKEN_PARAMS_DEFAULT_MAX_SIZE = 20000;

    private final int reqTokenParamsDefaultMaxSize;

    /**
     * Default value for {@link #additionalReqParamsMaxNumber} if case no configuration property is set.
     */
    public static final int DEFAULT_ADDITIONAL_REQ_PARAMS_MAX_NUMBER = 5;

    /**
     * Max number of additional request parameters copied into client session note to prevent DoS attacks.
     */
    private final int additionalReqParamsMaxNumber;

    /**
     * Default value for {@link #additionalReqParamsMaxSize} if case no configuration property is set.
     */
    public static final int DEFAULT_ADDITIONAL_REQ_PARAMS_MAX_SIZE = 2000;

    /**
     * Max size of additional request parameters value copied into client session note to prevent DoS attacks.
     */
    private final int additionalReqParamsMaxSize;

    /**
     * Default value for {@link #additionalReqParamsFailFast} in case no configuration property is set.
     */
    public static final boolean DEFAULT_ADDITIONAL_REQ_PARAMS_FAIL_FAST = false;

    /**
     * Whether the fail-fast strategy should be enforced. If <code>false</code> all additional request parameters
     * that to not meet the configuration are silently ignored. If <code>true</code> an exception will be raised.
     */
    private final boolean additionalReqParamsFailFast;

    /**
     * Default value for {@link #additionalReqTokenParamsFailFast} in case no configuration property is set.
     */
    public static final boolean DEFAULT_ADDITIONAL_REQ_TOKEN_PARAMS_FAIL_FAST = true;

    /**
     * Whether the fail-fast strategy should be enforced for "token" parameters. If <code>false</code> all additional request parameters
     * that to not meet the configuration are silently ignored. If <code>true</code> an exception will be raised.
     *
     * As "token" parameter is considered a parameter containing long token (for example JWT or SAML assertion) with unbounded data (For example possibly big amount of roles inside JWT).
     * Applies for example for parameters like "subject_token" sent in case of token exchange grant.
     */
    private final boolean additionalReqTokenParamsFailFast;

    /**
     * Default value for {@link #additionalReqParamsMaxOverallSize} in case no configuration property is set.
     */
    public static final int DEFAULT_ADDITIONAL_REQ_PARAMS_MAX_OVERALL_SIZE = Integer.MAX_VALUE;

    /**
     * Max size of all additional request parameters value copied into client session note to prevent DoS attacks.
     */
    private final int additionalReqParamsMaxOverallSize;

    /**
     * @deprecated to be removed in Keycloak 27
     */
    public static final boolean DEFAULT_ALLOW_MULTIPLE_AUDIENCES_FOR_JWT_CLIENT_AUTHENTICATION = false;

    /**
     * Whether to allow multiple audiences for JWT client authentication
     * @deprecated To be removed in Keycloak 27
     */
    private final boolean allowMultipleAudiencesForJwtClientAuthentication;

    public static final boolean DEFAULT_ALLOW_TOKEN_INTROSPECTION_WITHOUT_AUDIENCE_CHECK = false;

    private final boolean allowTokenIntrospectionWithoutAudienceCheck;

    public static final boolean DEFAULT_ALLOW_USERINFO_WITH_LIGHTWEIGHT_ACCESS_TOKEN = false;

    private final boolean allowUserinfoWithLightweightAccessToken;

    public static final boolean DEFAULT_ALLOW_CLIENT_INITIATED_ACCOUNT_LINKING = false;

    private final boolean allowClientInitiatedAccountLinking;

    // Default - false, change to true for backward compatibility, to be removed in KC 27
    public static final boolean DEFAULT_ALLOW_OIDC_PARAMS_IN_REDIRECT_URIS = false;

    private final boolean allowOidcParamsInRedirectUris;

    public static final boolean DEFAULT_ALLOW_INITIATING_IDP_LOGOUT_PARAM = false;

    private final boolean allowInitiatingIdpLogoutParam;

    /**
     * Constructs an instance without result caching for {@link #getMaxLengthForTheParameter(String, boolean)},
     * which is then resolved directly, uncached, on every call. Intended for unit tests constructing this class
     * directly, bypassing the {@code OIDCLoginProtocolFactory} lifecycle.
     *
     * @deprecated Use {@link #OIDCProviderConfig(Config.Scope, LocalCache, LocalCache)}
     * instead, passing the two {@link LocalCache}s created by {@code OIDCLoginProtocolFactory#postInit()} (or, for
     * tests not exercising caching behavior, no-op {@link LocalCache} implementations).
     */
    @Deprecated(forRemoval = true, since = "26.8")
    public OIDCProviderConfig(Config.Scope config) {
        this(config, NoopLocalCache.getInstance(), NoopLocalCache.getInstance());
    }

    /**
     * @param reqParamMaxLengthCache Bounded {@link LocalCache} used to memoize {@link #getMaxLengthForTheParameter(String, boolean)}
     *                               lookups for non-token parameters
     * @param tokenParamMaxLengthCache Bounded {@link LocalCache} used to memoize {@link #getMaxLengthForTheParameter(String, boolean)}
     *                                 lookups for token parameters
     */
    public OIDCProviderConfig(Config.Scope config, LocalCache<String, Integer> reqParamMaxLengthCache, LocalCache<String, Integer> tokenParamMaxLengthCache) {
        this.config = config;
        this.reqParamMaxLengthCache = reqParamMaxLengthCache;
        this.tokenParamMaxLengthCache = tokenParamMaxLengthCache;

        this.reqParamsDefaultMaxSize = config.getInt(OIDCLoginProtocolFactory.CONFIG_OIDC_REQ_PARAMS_DEFAULT_MAX_SIZE, DEFAULT_REQ_PARAMS_DEFAULT_MAX_SIZE);
        this.reqTokenParamsDefaultMaxSize = config.getInt(OIDCLoginProtocolFactory.CONFIG_OIDC_REQ_TOKEN_PARAMS_DEFAULT_MAX_SIZE, DEFAULT_REQ_TOKEN_PARAMS_DEFAULT_MAX_SIZE);
        this.additionalReqParamsMaxNumber = config.getInt(OIDCLoginProtocolFactory.CONFIG_OIDC_ADD_REQ_PARAMS_MAX_NUMBER, DEFAULT_ADDITIONAL_REQ_PARAMS_MAX_NUMBER);
        this.additionalReqParamsMaxSize = config.getInt(OIDCLoginProtocolFactory.CONFIG_OIDC_ADD_REQ_PARAMS_MAX_SIZE, DEFAULT_ADDITIONAL_REQ_PARAMS_MAX_SIZE);
        this.additionalReqParamsMaxOverallSize = config.getInt(OIDCLoginProtocolFactory.CONFIG_OIDC_ADD_REQ_PARAMS_MAX_OVERALL_SIZE, DEFAULT_ADDITIONAL_REQ_PARAMS_MAX_OVERALL_SIZE);
        this.additionalReqParamsFailFast = config.getBoolean(OIDCLoginProtocolFactory.CONFIG_OIDC_ADD_REQ_PARAMS_FAIL_FAST, DEFAULT_ADDITIONAL_REQ_PARAMS_FAIL_FAST);
        this.additionalReqTokenParamsFailFast = config.getBoolean(OIDCLoginProtocolFactory.CONFIG_OIDC_ADD_REQ_TOKEN_PARAMS_FAIL_FAST, DEFAULT_ADDITIONAL_REQ_TOKEN_PARAMS_FAIL_FAST);

        this.allowMultipleAudiencesForJwtClientAuthentication = config.getBoolean(OIDCLoginProtocolFactory.CONFIG_OIDC_ALLOW_MULTIPLE_AUDIENCES_FOR_JWT_CLIENT_AUTHENTICATION, DEFAULT_ALLOW_MULTIPLE_AUDIENCES_FOR_JWT_CLIENT_AUTHENTICATION);
        this.allowTokenIntrospectionWithoutAudienceCheck = config.getBoolean(OIDCLoginProtocolFactory.CONFIG_ALLOW_TOKEN_INTROSPECTION_WITHOUT_AUDIENCE_CHECK, DEFAULT_ALLOW_TOKEN_INTROSPECTION_WITHOUT_AUDIENCE_CHECK);
        this.allowUserinfoWithLightweightAccessToken = config.getBoolean(OIDCLoginProtocolFactory.CONFIG_ALLOW_USERINFO_WITH_LIGHTWEIGHT_ACCESS_TOKEN, DEFAULT_ALLOW_USERINFO_WITH_LIGHTWEIGHT_ACCESS_TOKEN);
        this.allowClientInitiatedAccountLinking = config.getBoolean(OIDCLoginProtocolFactory.CONFIG_ALLOW_CLIENT_INITIATED_ACCOUNT_LINKING, DEFAULT_ALLOW_CLIENT_INITIATED_ACCOUNT_LINKING);
        this.allowOidcParamsInRedirectUris = config.getBoolean(OIDCLoginProtocolFactory.CONFIG_ALLOW_OIDC_PARAMS_IN_REDIRECT_URIS, DEFAULT_ALLOW_OIDC_PARAMS_IN_REDIRECT_URIS);
        this.allowInitiatingIdpLogoutParam = config.getBoolean(OIDCLoginProtocolFactory.CONFIG_ALLOW_INITIATING_IDP_LOGOUT_PARAM, DEFAULT_ALLOW_INITIATING_IDP_LOGOUT_PARAM);
    }

    public int getAdditionalReqParamsMaxNumber() {
        return additionalReqParamsMaxNumber;
    }

    public int getAdditionalReqParamsMaxSize() {
        return additionalReqParamsMaxSize;
    }

    public boolean isAdditionalReqParamsFailFast(boolean isTokenParam) {
        return isTokenParam ? additionalReqTokenParamsFailFast : additionalReqParamsFailFast;
    }

    public int getAdditionalReqParamsMaxOverallSize() {
        return additionalReqParamsMaxOverallSize;
    }

    public boolean isAllowMultipleAudiencesForJwtClientAuthentication() {
        return allowMultipleAudiencesForJwtClientAuthentication;
    }

    public boolean isAllowTokenIntrospectionWithoutAudienceCheck() {
        return allowTokenIntrospectionWithoutAudienceCheck;
    }

    public boolean isAllowOidcParamsInRedirectUris() {
        return allowOidcParamsInRedirectUris;
    }

    public boolean isAllowUserinfoWithLightweightAccessToken() {
        return allowUserinfoWithLightweightAccessToken;
    }

    public boolean isAllowClientInitiatedAccountLinking() {
        return allowClientInitiatedAccountLinking;
    }

    public boolean isAllowInitiatingIdpLogoutParam() {
        return allowInitiatingIdpLogoutParam;
    }

    /**
     * @param paramName Parameter name. Expected to be one of the known OIDC parameters
     * @param isTokenParam If this parameter represents token (like for example JWT)
     *
     * @return maximum length for the specified OIDC parameter
     */
    public int getMaxLengthForTheParameter(String paramName, boolean isTokenParam) {
        LocalCache<String, Integer> cache = isTokenParam ? tokenParamMaxLengthCache : reqParamMaxLengthCache;

        // The resolved value can only change following a server restart, so it is safe to cache for the
        // lifetime of this (singleton) instance, avoiding the underlying configuration resolution on every call.
        Integer paramMaxSize = cache.get(paramName);
        if (paramMaxSize == null) {
            paramMaxSize = computeMaxLengthForTheParameter(paramName, isTokenParam);
            cache.put(paramName, paramMaxSize);
        }

        return paramMaxSize;
    }

    private int computeMaxLengthForTheParameter(String paramName, boolean isTokenParam) {
        // Configured value for the particular OIDC parameter
        Integer paramMaxSize = config.getInt(OIDCLoginProtocolFactory.CONFIG_OIDC_REQ_PARAMS_MAX_SIZE_PREFIX + "--" + paramName);

        // Stick to default. See if we have default value overriden
        if (paramMaxSize == null) {
            paramMaxSize = DEFAULT_MAX_PARAMS_SIZES.get(paramName);
        }

        // Fallback to default for all standard OIDC parameters
        if (paramMaxSize == null) {
            paramMaxSize = isTokenParam ? reqTokenParamsDefaultMaxSize : reqParamsDefaultMaxSize;
        }

        return paramMaxSize;
    }

    /**
     * A {@link LocalCache} that never caches anything, used by the single-argument constructor so that
     * {@link #getMaxLengthForTheParameter(String, boolean)} always resolves the value directly, without needing a
     * null-check for a cache that was never wired in.
     */
    private static final class NoopLocalCache<K, V> implements LocalCache<K, V> {

        private static final NoopLocalCache<?, ?> INSTANCE = new NoopLocalCache<>();

        @SuppressWarnings("unchecked")
        static <K, V> LocalCache<K, V> getInstance() {
            return (LocalCache<K, V>) INSTANCE;
        }

        @Override
        public V get(K key) {
            return null;
        }

        @Override
        public void put(K key, V value) {
            // no-op
        }

        @Override
        public void invalidate(K key) {
            // no-op
        }

        @Override
        public void close() {
            // no-op
        }
    }
}
