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

package org.keycloak.models;

import org.keycloak.credential.CredentialInput;
import org.keycloak.models.credential.TrustedDeviceCredentialModel;

/**
 * @author Norbert Kelemen
 * @version $Revision: 1 $
 */
public class TrustedDeviceCredentialInputModel implements CredentialInput {

    private String credentialId;
    private String challengeResponse;


    public TrustedDeviceCredentialInputModel() {
    }

    public TrustedDeviceCredentialInputModel(String credentialId, String challengeResponse) {
        this.credentialId = credentialId;
        this.challengeResponse = challengeResponse;
    }


    @Override
    public String getCredentialId() {
        return credentialId;
    }

    @Override
    public String getType() {
        return TrustedDeviceCredentialModel.TYPE;
    }

    @Override
    public String getChallengeResponse() {
        return challengeResponse;
    }
}


