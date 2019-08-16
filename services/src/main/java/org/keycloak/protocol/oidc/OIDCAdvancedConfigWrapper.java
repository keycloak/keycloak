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

package org.keycloak.protocol.oidc;

import org.keycloak.jose.jws.Algorithm;
import org.keycloak.models.ClientModel;
import org.keycloak.representations.idm.ClientRepresentation;

import java.util.HashMap;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCAdvancedConfigWrapper {

    private final ClientModel clientModel;
    private final ClientRepresentation clientRep;

    private OIDCAdvancedConfigWrapper(ClientModel client, ClientRepresentation clientRep) {
        this.clientModel = client;
        this.clientRep = clientRep;
    }


    public static OIDCAdvancedConfigWrapper fromClientModel(ClientModel client) {
        return new OIDCAdvancedConfigWrapper(client, null);
    }

    public static OIDCAdvancedConfigWrapper fromClientRepresentation(ClientRepresentation clientRep) {
        return new OIDCAdvancedConfigWrapper(null, clientRep);
    }


    public Algorithm getUserInfoSignedResponseAlg() {
        String alg = getAttribute(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG);
        return alg==null ? null : Enum.valueOf(Algorithm.class, alg);
    }

    public void setUserInfoSignedResponseAlg(Algorithm alg) {
        String algStr = alg==null ? null : alg.toString();
        setAttribute(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, algStr);
    }

    public boolean isUserInfoSignatureRequired() {
        return getUserInfoSignedResponseAlg() != null;
    }

    public Algorithm getRequestObjectSignatureAlg() {
        String alg = getAttribute(OIDCConfigAttributes.REQUEST_OBJECT_SIGNATURE_ALG);
        return alg==null ? null : Enum.valueOf(Algorithm.class, alg);
    }

    public void setRequestObjectSignatureAlg(Algorithm alg) {
        String algStr = alg==null ? null : alg.toString();
        setAttribute(OIDCConfigAttributes.REQUEST_OBJECT_SIGNATURE_ALG, algStr);
    }
    
    public String getRequestObjectRequired() {
        return getAttribute(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED);
    }
    
    public void setRequestObjectRequired(String requestObjectRequired) {
        setAttribute(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED, requestObjectRequired);
    }

    public boolean isUseJwksUrl() {
        String useJwksUrl = getAttribute(OIDCConfigAttributes.USE_JWKS_URL);
        return Boolean.parseBoolean(useJwksUrl);
    }

    public void setUseJwksUrl(boolean useJwksUrl) {
        String val = String.valueOf(useJwksUrl);
        setAttribute(OIDCConfigAttributes.USE_JWKS_URL, val);
    }

    public String getJwksUrl() {
        return getAttribute(OIDCConfigAttributes.JWKS_URL);
    }

    public void setJwksUrl(String jwksUrl) {
        setAttribute(OIDCConfigAttributes.JWKS_URL, jwksUrl);
    }

    public boolean isExcludeSessionStateFromAuthResponse() {
        String excludeSessionStateFromAuthResponse = getAttribute(OIDCConfigAttributes.EXCLUDE_SESSION_STATE_FROM_AUTH_RESPONSE);
        return Boolean.parseBoolean(excludeSessionStateFromAuthResponse);
    }

    public void setExcludeSessionStateFromAuthResponse(boolean excludeSessionStateFromAuthResponse) {
        String val = String.valueOf(excludeSessionStateFromAuthResponse);
        setAttribute(OIDCConfigAttributes.EXCLUDE_SESSION_STATE_FROM_AUTH_RESPONSE, val);
    }

    // KEYCLOAK-6771 Certificate Bound Token
    // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-6.5
    public boolean isUseMtlsHokToken() {
        String useUtlsHokToken = getAttribute(OIDCConfigAttributes.USE_MTLS_HOK_TOKEN);
        return Boolean.parseBoolean(useUtlsHokToken);
    }

    public void setUseMtlsHoKToken(boolean useUtlsHokToken) {
        String val = String.valueOf(useUtlsHokToken);
        setAttribute(OIDCConfigAttributes.USE_MTLS_HOK_TOKEN, val);
    }

    public String getPkceCodeChallengeMethod() {
        return getAttribute(OIDCConfigAttributes.PKCE_CODE_CHALLENGE_METHOD);
    }

    public void setPkceCodeChallengeMethod(String codeChallengeMethodName) {
        setAttribute(OIDCConfigAttributes.PKCE_CODE_CHALLENGE_METHOD, codeChallengeMethodName);
    }

    public String getIdTokenSignedResponseAlg() {
        return getAttribute(OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG);
    }
    public void setIdTokenSignedResponseAlg(String algName) {
        setAttribute(OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG, algName);
    }

    public String getIdTokenEncryptedResponseAlg() {
        return getAttribute(OIDCConfigAttributes.ID_TOKEN_ENCRYPTED_RESPONSE_ALG);
    }

    public void setIdTokenEncryptedResponseAlg(String algName) {
        setAttribute(OIDCConfigAttributes.ID_TOKEN_ENCRYPTED_RESPONSE_ALG, algName);
    }

    public String getIdTokenEncryptedResponseEnc() {
        return getAttribute(OIDCConfigAttributes.ID_TOKEN_ENCRYPTED_RESPONSE_ENC);
    }

    public void setIdTokenEncryptedResponseEnc(String encName) {
        setAttribute(OIDCConfigAttributes.ID_TOKEN_ENCRYPTED_RESPONSE_ENC, encName);
    }

    private String getAttribute(String attrKey) {
        if (clientModel != null) {
            return clientModel.getAttribute(attrKey);
        } else {
            return clientRep.getAttributes()==null ? null : clientRep.getAttributes().get(attrKey);
        }
    }

    private void setAttribute(String attrKey, String attrValue) {
        if (clientModel != null) {
            if (attrValue != null) {
                clientModel.setAttribute(attrKey, attrValue);
            } else {
                clientModel.removeAttribute(attrKey);
            }
        } else {
            if (attrValue != null) {
                if (clientRep.getAttributes() == null) {
                    clientRep.setAttributes(new HashMap<>());
                }
                clientRep.getAttributes().put(attrKey, attrValue);
            } else {
                if (clientRep.getAttributes() != null) {
                    clientRep.getAttributes().put(attrKey, null);
                }
            }
        }
    }
}
