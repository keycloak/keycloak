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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
abstract class AuthzEndpointRequestParser {

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
    public static final int ADDITIONAL_REQ_PARAMS_MAX_SIZE = 200;

    /** Set of known protocol GET params not to be stored into additionalReqParams} */
    private static final Set<String> KNOWN_REQ_PARAMS = new HashSet<>();
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

    public void parseRequest(AuthorizationEndpointRequest request) {
        String clientId = getParameter(OIDCLoginProtocol.CLIENT_ID_PARAM);

        if (request.clientId != null && !request.clientId.equals(clientId)) {
            throw new IllegalArgumentException("The client_id parameter doesn't match the one from OIDC 'request' or 'request_uri'");
        }

        request.clientId = clientId;
        request.responseType = replaceIfNotNull(request.responseType, getParameter(OIDCLoginProtocol.RESPONSE_TYPE_PARAM));
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

    protected abstract String getParameter(String paramName);

    protected abstract Integer getIntParameter(String paramName);

    protected abstract Set<String> keySet();

}
