/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Data associated with the oauth2 code.
 *
 * Those data are typically valid just for the very short time - they're created at the point before we redirect to the application
 * after successful and they're removed when application sends requests to the token endpoint (code-to-token endpoint) to exchange the
 * single-use OAuth2 code parameter for those data.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OAuth2Code {

    private static final String ID_NOTE = "id";
    private static final String EXPIRATION_NOTE = "exp";
    private static final String NONCE_NOTE = "nonce";
    private static final String SCOPE_NOTE = "scope";
    private static final String REDIRECT_URI_PARAM_NOTE = "redirectUri";
    private static final String CODE_CHALLENGE_NOTE = "code_challenge";
    private static final String CODE_CHALLENGE_METHOD_NOTE = "code_challenge_method";

    private final String id;

    private final int expiration;

    private final String nonce;

    private final String scope;

    private final String redirectUriParam;

    private final String codeChallenge;

    private final String codeChallengeMethod;


    public OAuth2Code(String id, int expiration, String nonce, String scope, String redirectUriParam,
                      String codeChallenge, String codeChallengeMethod) {
        this.id = id;
        this.expiration = expiration;
        this.nonce = nonce;
        this.scope = scope;
        this.redirectUriParam = redirectUriParam;
        this.codeChallenge = codeChallenge;
        this.codeChallengeMethod = codeChallengeMethod;
    }


    private OAuth2Code(Map<String, String> data) {
        id = data.get(ID_NOTE);
        expiration = Integer.parseInt(data.get(EXPIRATION_NOTE));
        nonce = data.get(NONCE_NOTE);
        scope = data.get(SCOPE_NOTE);
        redirectUriParam = data.get(REDIRECT_URI_PARAM_NOTE);
        codeChallenge = data.get(CODE_CHALLENGE_NOTE);
        codeChallengeMethod = data.get(CODE_CHALLENGE_METHOD_NOTE);
    }


    public static final OAuth2Code deserializeCode(Map<String, String> data) {
        return new OAuth2Code(data);
    }


    public Map<String, String> serializeCode() {
        Map<String, String> result = new HashMap<>();

        result.put(ID_NOTE, id.toString());
        result.put(EXPIRATION_NOTE, String.valueOf(expiration));
        result.put(NONCE_NOTE, nonce);
        result.put(SCOPE_NOTE, scope);
        result.put(REDIRECT_URI_PARAM_NOTE, redirectUriParam);
        result.put(CODE_CHALLENGE_NOTE, codeChallenge);
        result.put(CODE_CHALLENGE_METHOD_NOTE, codeChallengeMethod);

        return result;
    }


    public String getId() {
        return id;
    }

    public int getExpiration() {
        return expiration;
    }

    public String getNonce() {
        return nonce;
    }

    public String getScope() {
        return scope;
    }

    public String getRedirectUriParam() {
        return redirectUriParam;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public String getCodeChallengeMethod() {
        return codeChallengeMethod;
    }
}
