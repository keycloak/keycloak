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
import org.keycloak.protocol.oidc.utils.SubjectType;
import org.keycloak.representations.idm.ClientRepresentation;

import java.util.HashMap;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCAdvancedConfigWrapper {

    private static final String USER_INFO_RESPONSE_SIGNATURE_ALG = "user.info.response.signature.alg";

    private static final String REQUEST_OBJECT_SIGNATURE_ALG = "request.object.signature.alg";

    private static final String SUBJECT_TYPE = "oidc.subject_type";
    private static final String SECTOR_IDENTIFIER_URI = "oidc.sector_identifier_uri";
    private static final String PUBLIC = "public";
    private static final String PAIRWISE = "pairwise";

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
        String alg = getAttribute(USER_INFO_RESPONSE_SIGNATURE_ALG);
        return alg==null ? null : Enum.valueOf(Algorithm.class, alg);
    }

    public void setUserInfoSignedResponseAlg(Algorithm alg) {
        String algStr = alg==null ? null : alg.toString();
        setAttribute(USER_INFO_RESPONSE_SIGNATURE_ALG, algStr);
    }

    public boolean isUserInfoSignatureRequired() {
        return getUserInfoSignedResponseAlg() != null;
    }

    public Algorithm getRequestObjectSignatureAlg() {
        String alg = getAttribute(REQUEST_OBJECT_SIGNATURE_ALG);
        return alg==null ? null : Enum.valueOf(Algorithm.class, alg);
    }

    public void setRequestObjectSignatureAlg(Algorithm alg) {
        String algStr = alg==null ? null : alg.toString();
        setAttribute(REQUEST_OBJECT_SIGNATURE_ALG, algStr);
    }

    public void setSubjectType(SubjectType subjectType) {
        if (subjectType == null) {
            setAttribute(SUBJECT_TYPE, SubjectType.PUBLIC.toString());
            return;
        }
        setAttribute(SUBJECT_TYPE, subjectType.toString());
    }

    public SubjectType getSubjectType() {
        String subjectType = getAttribute(SUBJECT_TYPE);
        return subjectType == null ? SubjectType.PUBLIC : Enum.valueOf(SubjectType.class, subjectType);
    }

    public void setSectorIdentifierUri(String sectorIdentifierUri) {
        setAttribute(SECTOR_IDENTIFIER_URI, sectorIdentifierUri);
    }

    public String getSectorIdentifierUri() {
        return getAttribute(SECTOR_IDENTIFIER_URI);
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
