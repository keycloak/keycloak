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
import org.keycloak.constants.AdapterConstants;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AuthzEndpointRequestParser {

    private static final Logger logger = Logger.getLogger(AuthzEndpointRequestParser.class);

    /**
     * Max number of additional req params copied into client session note to prevent DoS attacks
     *
     */
    public static final int ADDITIONAL_REQ_PARAMS_MAX_MUMBER = 5;

    /**
     * Max size of additional req param value copied into client session note to prevent DoS attacks - params with longer value are ignored
     *
     */
    public static final int ADDITIONAL_REQ_PARAMS_MAX_SIZE = 2000;

    public static final String AUTHZ_REQUEST_OBJECT = "ParsedRequestObject";
    public static final String AUTHZ_REQUEST_OBJECT_ENCRYPTED = "EncryptedRequestObject";

    /**
     * Set of known protocol request params that support repeated parameters.
     */
    public static final Set<String> KNOWN_MULTI_PARAMS;

    /** Set of known protocol GET params not to be stored into {@code additionalReqParams} */
    public static final Set<String> KNOWN_REQ_PARAMS;
    static {
        Set<String> knownReqParams = new HashSet<>();
        knownReqParams.add(OIDCLoginProtocol.CLIENT_ID_PARAM);
        knownReqParams.add(OIDCLoginProtocol.RESPONSE_TYPE_PARAM);
        knownReqParams.add(OIDCLoginProtocol.RESPONSE_MODE_PARAM);
        knownReqParams.add(OIDCLoginProtocol.REDIRECT_URI_PARAM);
        knownReqParams.add(OIDCLoginProtocol.STATE_PARAM);
        knownReqParams.add(OIDCLoginProtocol.SCOPE_PARAM);
        knownReqParams.add(OIDCLoginProtocol.LOGIN_HINT_PARAM);
        knownReqParams.add(OIDCLoginProtocol.PROMPT_PARAM);
        knownReqParams.add(AdapterConstants.KC_IDP_HINT);
        knownReqParams.add(Constants.KC_ACTION);
        knownReqParams.add(OIDCLoginProtocol.NONCE_PARAM);
        knownReqParams.add(OIDCLoginProtocol.MAX_AGE_PARAM);
        knownReqParams.add(OIDCLoginProtocol.UI_LOCALES_PARAM);
        knownReqParams.add(OIDCLoginProtocol.REQUEST_PARAM);
        knownReqParams.add(OIDCLoginProtocol.REQUEST_URI_PARAM);
        knownReqParams.add(OIDCLoginProtocol.CLAIMS_PARAM);
        knownReqParams.add(OIDCLoginProtocol.ACR_PARAM);

        // https://tools.ietf.org/html/rfc7636#section-6.1
        knownReqParams.add(OIDCLoginProtocol.CODE_CHALLENGE_PARAM);
        knownReqParams.add(OIDCLoginProtocol.CODE_CHALLENGE_METHOD_PARAM);

        // https://datatracker.ietf.org/doc/html/rfc9449#section-12.3
        knownReqParams.add(OIDCLoginProtocol.DPOP_JKT);

        // Those are not OAuth/OIDC parameters, but they should never be added to the additionalRequestParameters
        knownReqParams.add(OAuth2Constants.CLIENT_ASSERTION_TYPE);
        knownReqParams.add(OAuth2Constants.CLIENT_ASSERTION);
        knownReqParams.add(OAuth2Constants.CLIENT_SECRET);

        // https://www.rfc-editor.org/rfc/rfc8707#section-2
        knownReqParams.add(OAuth2Constants.RESOURCE);

        KNOWN_REQ_PARAMS = Collections.unmodifiableSet(knownReqParams);

        Set<String> knownMultiReqParams= new HashSet<>();
        knownMultiReqParams.add(OAuth2Constants.RESOURCE);

        KNOWN_MULTI_PARAMS = Collections.unmodifiableSet(knownMultiReqParams);
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

        request.dpopJkt = replaceIfNotNull(request.dpopJkt, getParameter(OIDCLoginProtocol.DPOP_JKT));

        request.resources = replaceIfNotNull(request.resources, getMultiParameter(OAuth2Constants.RESOURCE));

        extractAdditionalReqParams(request.additionalReqParams);
    }

    protected void validateResponseTypeParameter(String responseTypeParameter, AuthorizationEndpointRequest request) {
        if (responseTypeParameter != null && request.responseType != null && !request.responseType.equals(responseTypeParameter)) {
            logger.warnf("The response_type parameter doesn't match the one from OIDC 'request' or 'request_uri'");
            request.setInvalidRequestMessage("Parameter response_type does not match");
        }
    }

    protected void extractAdditionalReqParams(Map<String, String> additionalReqParams) {
        for (String paramName : keySet()) {
            if (!KNOWN_REQ_PARAMS.contains(paramName)) {
                String value = getParameter(paramName);
                if (value != null && value.trim().isEmpty()) {
                    value = null;
                }
                if (value != null && value.length() <= ADDITIONAL_REQ_PARAMS_MAX_SIZE) {
                    if (additionalReqParams.size() >= ADDITIONAL_REQ_PARAMS_MAX_MUMBER) {
                        logger.debug("Maximal number of additional OIDC params (" + ADDITIONAL_REQ_PARAMS_MAX_MUMBER + ") exceeded, ignoring rest of them!");
                        break;
                    }
                    additionalReqParams.put(paramName, value);
                } else {
                    logger.debug("OIDC Additional param " + paramName + " ignored because value is empty or longer than " + ADDITIONAL_REQ_PARAMS_MAX_SIZE);
                }
            }

        }
    }

    protected <T> T replaceIfNotNull(T previousVal, T newVal) {
        return newVal==null ? previousVal : newVal;
    }

    protected abstract Set<String> getMultiParameter(String paramName);

    protected abstract String getParameter(String paramName);

    protected abstract Integer getIntParameter(String paramName);

    protected abstract Set<String> keySet();

}
