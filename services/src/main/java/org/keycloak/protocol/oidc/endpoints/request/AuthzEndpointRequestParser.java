/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.protocol.oidc.endpoints.request;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.ErrorResponseException;

import jakarta.ws.rs.core.Response;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This endpoint parser supports, per default, up to
 * {@value #DEFAULT_ADDITIONAL_REQ_PARAMS_MAX_MUMBER} parameters with each
 * having a total size of {@value #DEFAULT_ADDITIONAL_REQ_PARAMS_MAX_SIZE}. If
 * there are more authentication request parameters, or a parameter has a size
 * than allowed, those parameters are silently ignored.
 * <p>
 * You can toggle the behavior by setting a realm specific attribute
 * ({@code additionalReqParamsFailFast}) that enables the fail-fast principle.
 * Any request parameter in violation of the configuration results in an
 * error response, e.g.,
 * <ul>
 * <li>for a Pushed Authorization Request (PAR) this results in a JSON response.</li>
 * <li>For openid/auth in an error page with an "Back to Application" button using the client's base URL. (if valid) as redirect target.</li>
 * </ul>
 *
 * <p>
 * Additionally a realm specific attribute ({@code additionalReqParamMaxOverallSize}) can be configured
 * that sets the maximum of size of all parameters combined. If not provided, {@link Integer#MAX_VALUE} will be used.
 *
 * @author <a href="mailto:manuel.schallar@prime-sign.com">Manuel Schallar</a>
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AuthzEndpointRequestParser {

    private static final Logger logger = Logger.getLogger(AuthzEndpointRequestParser.class);

    /**
     * Default value for {@link #additionalReqParamsMaxNumber} if case no realm property is set.
     */
    private static final int DEFAULT_ADDITIONAL_REQ_PARAMS_MAX_MUMBER = 5;

    /**
     * Max number of additional request parameters copied into client session note to prevent DoS attacks.
     */
    protected final int additionalReqParamsMaxNumber;
    
    /**
     * Default value for {@link #additionalReqParamsMaxSize} if case no realm property is set.
     */
    private static final int DEFAULT_ADDITIONAL_REQ_PARAMS_MAX_SIZE = 2000;
    
    /**
     * Max size of additional request parameters value copied into client session note to prevent DoS attacks.
     */
    protected final int additionalReqParamsMaxSize;
    
    /**
     * Default value for {@link #additionalReqParamsFailFast} in case no realm property is set.
     */
    private static final boolean DEFAULT_ADDITIONAL_REQ_PARAMS_FAIL_FAST = false;
    
    /**
     * Whether the fail-fast strategy should be enforced. If <code>false</code> all additional request parameters
     * that to not meet the configuration are silently ignored. If <code>true</code> an exception will be raised.
     */
    protected final boolean additionalReqParamsFailFast;
    
    /**
     * Default value for {@link #additionalReqParamsMaxOverallSize} in case no realm property is set.
     * 
     */
    private static final int DEFAULT_ADDITIONAL_REQ_PARAMS_MAX_OVERALL_SIZE = Integer.MAX_VALUE;
    
    /**
     * Max size of all additional request parameters value copied into client session note to prevent DoS attacks.
     */
    protected final int additionalReqParamsMaxOverallSize;

    public static final String AUTHZ_REQUEST_OBJECT = "ParsedRequestObject";
    public static final String AUTHZ_REQUEST_OBJECT_ENCRYPTED = "EncryptedRequestObject";

    /** Set of known protocol GET params not to be stored into additionalReqParams} */
    public static final Set<String> KNOWN_REQ_PARAMS = new HashSet<>();
    static {
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.CLIENT_ID_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.RESPONSE_TYPE_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.RESPONSE_MODE_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.REDIRECT_URI_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.STATE_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.SCOPE_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.LOGIN_HINT_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.PROMPT_PARAM);
        KNOWN_REQ_PARAMS.add(AdapterConstants.KC_IDP_HINT);
        KNOWN_REQ_PARAMS.add(Constants.KC_ACTION);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.NONCE_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.MAX_AGE_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.UI_LOCALES_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.REQUEST_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.REQUEST_URI_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.CLAIMS_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.ACR_PARAM);

        // https://tools.ietf.org/html/rfc7636#section-6.1
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.CODE_CHALLENGE_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.CODE_CHALLENGE_METHOD_PARAM);
    }

    protected AuthzEndpointRequestParser(KeycloakSession keycloakSession) {
      RealmModel realm = keycloakSession.getContext().getRealm();
      this.additionalReqParamsMaxNumber = realm.getAttribute("additionalReqParamsMaxNumber", DEFAULT_ADDITIONAL_REQ_PARAMS_MAX_MUMBER);
      this.additionalReqParamsMaxSize = realm.getAttribute("additionalReqParamsMaxSize", DEFAULT_ADDITIONAL_REQ_PARAMS_MAX_SIZE);
      this.additionalReqParamsFailFast = realm.getAttribute("additionalReqParamsFailFast", DEFAULT_ADDITIONAL_REQ_PARAMS_FAIL_FAST);
      this.additionalReqParamsMaxOverallSize = realm.getAttribute("additionalReqParamsMaxOverallSize", DEFAULT_ADDITIONAL_REQ_PARAMS_MAX_OVERALL_SIZE);
    }
    
    public void parseRequest(AuthorizationEndpointRequest request) {
        String clientId = getParameter(OIDCLoginProtocol.CLIENT_ID_PARAM);
        if (clientId != null && request.clientId != null && !request.clientId.equals(clientId)) {
            throw new IllegalArgumentException("The client_id parameter doesn't match the one from OIDC 'request' or 'request_uri'");
        }
        if (clientId != null) {
            request.clientId = clientId;
        }

        String responseType = getParameter(OIDCLoginProtocol.RESPONSE_TYPE_PARAM);
        validateResponseTypeParameter(responseType, request);
        if (responseType != null) {
            request.responseType = responseType;
        }

        request.responseMode = replaceIfNotNull(request.responseMode, getParameter(OIDCLoginProtocol.RESPONSE_MODE_PARAM));
        request.redirectUriParam = replaceIfNotNull(request.redirectUriParam, getParameter(OIDCLoginProtocol.REDIRECT_URI_PARAM));
        request.state = replaceIfNotNull(request.state, getParameter(OIDCLoginProtocol.STATE_PARAM));
        request.scope = replaceIfNotNull(request.scope, getParameter(OIDCLoginProtocol.SCOPE_PARAM));
        request.loginHint = replaceIfNotNull(request.loginHint, getParameter(OIDCLoginProtocol.LOGIN_HINT_PARAM));
        request.prompt = replaceIfNotNull(request.prompt, getParameter(OIDCLoginProtocol.PROMPT_PARAM));
        request.idpHint = replaceIfNotNull(request.idpHint, getParameter(AdapterConstants.KC_IDP_HINT));
        request.action = replaceIfNotNull(request.action, getParameter(Constants.KC_ACTION));
        request.nonce = replaceIfNotNull(request.nonce, getParameter(OIDCLoginProtocol.NONCE_PARAM));
        request.maxAge = replaceIfNotNull(request.maxAge, getIntParameter(OIDCLoginProtocol.MAX_AGE_PARAM));
        request.claims = replaceIfNotNull(request.claims, getParameter(OIDCLoginProtocol.CLAIMS_PARAM));
        request.acr = replaceIfNotNull(request.acr, getParameter(OIDCLoginProtocol.ACR_PARAM));
        request.display = replaceIfNotNull(request.display, getParameter(OAuth2Constants.DISPLAY));
        request.uiLocales = replaceIfNotNull(request.uiLocales, getParameter(OAuth2Constants.UI_LOCALES_PARAM));

        // https://tools.ietf.org/html/rfc7636#section-6.1
        request.codeChallenge = replaceIfNotNull(request.codeChallenge, getParameter(OIDCLoginProtocol.CODE_CHALLENGE_PARAM));
        request.codeChallengeMethod = replaceIfNotNull(request.codeChallengeMethod, getParameter(OIDCLoginProtocol.CODE_CHALLENGE_METHOD_PARAM));

        extractAdditionalReqParams(request.additionalReqParams);
    }

    protected void validateResponseTypeParameter(String responseTypeParameter, AuthorizationEndpointRequest request) {
        if (responseTypeParameter != null && request.responseType != null && !request.responseType.equals(responseTypeParameter)) {
            logger.warnf("The response_type parameter doesn't match the one from OIDC 'request' or 'request_uri'");
            request.setInvalidRequestMessage("Parameter response_type does not match");
        }
    }

    protected void extractAdditionalReqParams(Map<String, String> additionalReqParams) {
        int currentAdditionalReqParamMaxOverallSize = 0;
        for (String paramName : keySet()) {
          
          if (KNOWN_REQ_PARAMS.contains(paramName)) {
            logger.debugv("The additional OIDC param ''{0}'' is well known. Continue with the other additional parameters.", paramName);
            continue;
          }
          
          final String value = getParameter(paramName);
          
          if (value == null || value.trim().isEmpty()) {
            logger.debugv("The additional OIDC param ''{0}'' ignored because it's value is null or blank.", paramName);
            continue;
          }

          // Compare with ">=", as the currently processed parameter will be added at the END of this method.
          if (additionalReqParams.size() >= additionalReqParamsMaxNumber) {
            
            if (additionalReqParamsFailFast) {
              logger.infov("The maximum number of allowed parameters ({0}) is exceeded.", additionalReqParamsMaxNumber);
              throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "The maximum number of allowed parameters (" + additionalReqParamsMaxNumber + ") is exceeded.", Response.Status.BAD_REQUEST);
            } else {
              logger.debugv("The maximum number of allowed parameters ({0}) is exceeded.", additionalReqParamsMaxNumber);
              break;
            }
            
          }
          
          if (value.length() + currentAdditionalReqParamMaxOverallSize > additionalReqParamsMaxOverallSize) {

            if (additionalReqParamsFailFast) {
              logger.infov("The OIDC additional parameter '{0}''s size ({1}) exceeds the maximum allowed size of all parameters ({2}).", paramName, value.length(), additionalReqParamsMaxOverallSize);
              throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "The OIDC additional parameter '" + paramName + "'s size (" + value.length() + ") exceeds the maximum allowed size of all parameters (" + additionalReqParamsMaxOverallSize + ").", Response.Status.BAD_REQUEST);
            } else {
              logger.debugv("The OIDC additional parameter '{0}''s size exceeds ({1}) the maximum allowed size of all parameters ({2}).", paramName, value.length(), additionalReqParamsMaxOverallSize);
              break;
            }

          }

          if (value.length() > additionalReqParamsMaxSize) {
            
            if (additionalReqParamsFailFast) {
              logger.infov("The OIDC additional parameter '{0}''s size is longer ({1}) than allowed ({2}).", paramName, value.length(), additionalReqParamsMaxSize);
              throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "The OIDC additional parameter '" + paramName + "'s size is longer (" + value.length() + ") than allowed (" + additionalReqParamsMaxSize + ").", Response.Status.BAD_REQUEST);
            } else {
              logger.debugv("The OIDC additional parameter '{0}''s size is longer ({1}) than allowed ({2}).", paramName, value.length(), additionalReqParamsMaxSize);
              break;
            }
            
          }
          
          logger.debugv("Adding OIDC additional parameter ''{0}'' as additional parameter.", paramName);
          currentAdditionalReqParamMaxOverallSize += value.length();
          additionalReqParams.put(paramName, value);
        }
    }

    protected <T> T replaceIfNotNull(T previousVal, T newVal) {
        return newVal==null ? previousVal : newVal;
    }

    protected abstract String getParameter(String paramName);

    protected abstract Integer getIntParameter(String paramName);

    protected abstract Set<String> keySet();

}
