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
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.PasswordCredentialModel;

import java.util.UUID;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserCredentialModel implements CredentialInput {

    @Deprecated /** Use PasswordCredentialModel.TYPE instead **/
    public static final String PASSWORD = PasswordCredentialModel.TYPE;

    @Deprecated /** Use PasswordCredentialModel.PASSWORD_HISTORY instead **/
    public static final String PASSWORD_HISTORY = PasswordCredentialModel.PASSWORD_HISTORY;

    @Deprecated /**  Use OTPCredentialModel.TOTP instead **/
    public static final String TOTP = OTPCredentialModel.TOTP;

    @Deprecated /**  Use OTPCredentialModel.TOTP instead **/
    public static final String HOTP = OTPCredentialModel.HOTP;

    public static final String SECRET = CredentialModel.SECRET;
    public static final String KERBEROS = CredentialModel.KERBEROS;
    public static final String CLIENT_CERT = CredentialModel.CLIENT_CERT;

    private final String credentialId;
    private final String type;
    private final String challengeResponse;
    private final boolean adminRequest;

    public UserCredentialModel(String credentialId, String type, String challengeResponse) {
        this.credentialId = credentialId;
        this.type = type;
        this.challengeResponse = challengeResponse;
        this.adminRequest = false;
    }

    public UserCredentialModel(String credentialId, String type, String challengeResponse, boolean adminRequest) {
        this.credentialId = credentialId;
        this.type = type;
        this.challengeResponse = challengeResponse;
        this.adminRequest = adminRequest;
    }

    public static UserCredentialModel password(String password) {
        return password(password, false);
    }

    public static UserCredentialModel password(String password, boolean adminRequest) {
        return new UserCredentialModel("", PasswordCredentialModel.TYPE, password, adminRequest);
    }

    public static UserCredentialModel secret(String password) {
        return new UserCredentialModel("", SECRET, password);
    }

    public static UserCredentialModel kerberos(String token) {
        return new UserCredentialModel("", KERBEROS, token);
    }

    public static UserCredentialModel generateSecret() {
        return new UserCredentialModel("", SECRET, UUID.randomUUID().toString());
    }

    @Override
    public String getCredentialId() {
        return credentialId;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getChallengeResponse() {
        return challengeResponse;
    }

    public boolean isAdminRequest() {
        return adminRequest;
    }
}


