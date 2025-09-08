package org.keycloak.protocol.oidc;

import java.util.Map;

import org.keycloak.Config;

/**
 * @author <a href="mailto:patrick.weiner@prime-sign.com">Patrick Weiner</a>
 */
public class OIDCProviderConfig {

    private final Config.Scope config;

    /**
     * Maximum default length of the standard OIDC parameter sent to the OIDC authentication request.
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



    public OIDCProviderConfig(Config.Scope config) {
        this.config = config;

        this.reqParamsDefaultMaxSize = config.getInt(OIDCLoginProtocolFactory.CONFIG_OIDC_REQ_PARAMS_DEFAULT_MAX_SIZE, DEFAULT_REQ_PARAMS_DEFAULT_MAX_SIZE);
        this.additionalReqParamsMaxNumber = config.getInt(OIDCLoginProtocolFactory.CONFIG_OIDC_ADD_REQ_PARAMS_MAX_NUMBER, DEFAULT_ADDITIONAL_REQ_PARAMS_MAX_NUMBER);
        this.additionalReqParamsMaxSize = config.getInt(OIDCLoginProtocolFactory.CONFIG_OIDC_ADD_REQ_PARAMS_MAX_SIZE, DEFAULT_ADDITIONAL_REQ_PARAMS_MAX_SIZE);
        this.additionalReqParamsMaxOverallSize = config.getInt(OIDCLoginProtocolFactory.CONFIG_OIDC_ADD_REQ_PARAMS_MAX_OVERALL_SIZE, DEFAULT_ADDITIONAL_REQ_PARAMS_MAX_OVERALL_SIZE);
        this.additionalReqParamsFailFast = config.getBoolean(OIDCLoginProtocolFactory.CONFIG_OIDC_ADD_REQ_PARAMS_FAIL_FAST, DEFAULT_ADDITIONAL_REQ_PARAMS_FAIL_FAST);

        this.allowMultipleAudiencesForJwtClientAuthentication = config.getBoolean(OIDCLoginProtocolFactory.CONFIG_OIDC_ALLOW_MULTIPLE_AUDIENCES_FOR_JWT_CLIENT_AUTHENTICATION, DEFAULT_ALLOW_MULTIPLE_AUDIENCES_FOR_JWT_CLIENT_AUTHENTICATION);
    }

    public int getAdditionalReqParamsMaxNumber() {
        return additionalReqParamsMaxNumber;
    }

    public int getAdditionalReqParamsMaxSize() {
        return additionalReqParamsMaxSize;
    }

    public boolean isAdditionalReqParamsFailFast() {
        return additionalReqParamsFailFast;
    }

    public int getAdditionalReqParamsMaxOverallSize() {
        return additionalReqParamsMaxOverallSize;
    }

    public boolean isAllowMultipleAudiencesForJwtClientAuthentication() {
        return allowMultipleAudiencesForJwtClientAuthentication;
    }

    /**
     * @param paramName Parameter name. Expected to be one of the known OIDC parameters
     *
     * @return maximum length for the specified OIDC parameter
     */
    public int getMaxLengthForTheParameter(String paramName) {
        // Configured value for the particular OIDC parameter
        Integer paramMaxSize = config.getInt(OIDCLoginProtocolFactory.CONFIG_OIDC_REQ_PARAMS_MAX_SIZE_PREFIX + "--" + paramName);

        // Stick to default. See if we have default value overriden
        if (paramMaxSize == null) {
            paramMaxSize = DEFAULT_MAX_PARAMS_SIZES.get(paramName);
        }

        // Fallback to default for all standard OIDC parameters
        if (paramMaxSize == null) {
            paramMaxSize = reqParamsDefaultMaxSize;
        }

        return paramMaxSize;
    }
}
