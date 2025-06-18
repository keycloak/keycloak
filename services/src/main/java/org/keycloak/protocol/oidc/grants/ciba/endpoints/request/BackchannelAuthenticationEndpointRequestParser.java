/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.protocol.oidc.grants.ciba.endpoints.request;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public abstract class BackchannelAuthenticationEndpointRequestParser {

    private static final Logger logger = Logger.getLogger(BackchannelAuthenticationEndpointRequestParser.class);

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

    public static final String CIBA_SIGNED_AUTHENTICATION_REQUEST = "ParsedSignedAuthenticationRequest";

    /** Set of known protocol POST params not to be stored into additionalReqParams} */
    public static final Set<String> KNOWN_REQ_PARAMS = new HashSet<>();
    static {
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.REQUEST_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.REQUEST_URI_PARAM);

        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.SCOPE_PARAM);

        // CIBA
        KNOWN_REQ_PARAMS.add(CibaGrantType.CLIENT_NOTIFICATION_TOKEN);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.ACR_PARAM);
        KNOWN_REQ_PARAMS.add(CibaGrantType.LOGIN_HINT_TOKEN);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.ID_TOKEN_HINT);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.LOGIN_HINT_PARAM);
        KNOWN_REQ_PARAMS.add(CibaGrantType.BINDING_MESSAGE);
        KNOWN_REQ_PARAMS.add(CibaGrantType.USER_CODE);
        KNOWN_REQ_PARAMS.add(CibaGrantType.REQUESTED_EXPIRY);

        // OIDC
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.PROMPT_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.NONCE_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.MAX_AGE_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.UI_LOCALES_PARAM);
        KNOWN_REQ_PARAMS.add(OIDCLoginProtocol.CLAIMS_PARAM);

        // these parameters are not included in Authentication Channel Request
        // if these are included in Backchannel Authentication Request's body part for "client_secret_post" client authentication
        KNOWN_REQ_PARAMS.add(OAuth2Constants.CLIENT_ID);
        KNOWN_REQ_PARAMS.add(OAuth2Constants.CLIENT_SECRET);
    }

    public void parseRequest(BackchannelAuthenticationEndpointRequest request) {
        request.scope = replaceIfNotNull(request.scope, getParameter(OIDCLoginProtocol.SCOPE_PARAM));

        request.clientNotificationToken = replaceIfNotNull(request.clientNotificationToken, getParameter(CibaGrantType.CLIENT_NOTIFICATION_TOKEN));
        request.acr = replaceIfNotNull(request.acr, getParameter(OIDCLoginProtocol.ACR_PARAM));
        request.loginHintToken = replaceIfNotNull(request.loginHintToken, getParameter(CibaGrantType.LOGIN_HINT_TOKEN));
        request.idTokenHint = replaceIfNotNull(request.idTokenHint, getParameter(OIDCLoginProtocol.ID_TOKEN_HINT));
        request.loginHint = replaceIfNotNull(request.loginHint, getParameter(OIDCLoginProtocol.LOGIN_HINT_PARAM));
        request.bindingMessage = replaceIfNotNull(request.bindingMessage, getParameter(CibaGrantType.BINDING_MESSAGE));
        request.userCode = replaceIfNotNull(request.userCode, getParameter(CibaGrantType.USER_CODE));
        request.requestedExpiry = replaceIfNotNull(request.requestedExpiry, getIntParameter(CibaGrantType.REQUESTED_EXPIRY));

        request.prompt = replaceIfNotNull(request.prompt, getParameter(OIDCLoginProtocol.PROMPT_PARAM));
        request.nonce = replaceIfNotNull(request.nonce, getParameter(OIDCLoginProtocol.NONCE_PARAM));
        request.maxAge = replaceIfNotNull(request.maxAge, getIntParameter(OIDCLoginProtocol.MAX_AGE_PARAM));
        request.uiLocales = replaceIfNotNull(request.uiLocales, getParameter(OIDCLoginProtocol.UI_LOCALES_PARAM));
        request.claims = replaceIfNotNull(request.claims, getParameter(OIDCLoginProtocol.CLAIMS_PARAM));

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
                        logger.debug("Maximal number of additional OIDC CIBA params (" + ADDITIONAL_REQ_PARAMS_MAX_MUMBER + ") exceeded, ignoring rest of them!");
                        break;
                    }
                    additionalReqParams.put(paramName, value);
                } else {
                    logger.debug("OIDC CIBA Additional param " + paramName + " ignored because value is empty or longer than " + ADDITIONAL_REQ_PARAMS_MAX_SIZE);
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
