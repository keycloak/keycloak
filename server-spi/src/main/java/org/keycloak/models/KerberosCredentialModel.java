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


/*
Adding information for the kerberos ticket if multiple user storage to check
A ticket can only be validated once:
    GSSException: Failure unspecified at GSS-API level (Mechanism level: Request is a replay (34))

 */
public class KerberosCredentialModel extends UserCredentialModel {

    private String kerberosUsername;
    private String kerberosRealm;
    private String kerberosDelegationCredential;
    private boolean kerberosAuthenticated = false;
    private String kerberosResponseToken = null;

    public KerberosCredentialModel(String credentialId, String type, String challengeResponse) {
        super(credentialId, type, challengeResponse);
    }

    public String getKerberosUsername() {
        return kerberosUsername;
    }

    public void setKerberosUsername(String kerberosUsername) {
        this.kerberosUsername = kerberosUsername;
    }

    public String getKerberosRealm() {
        return kerberosRealm;
    }

    public void setKerberosRealm(String kerberosRealm) {
        this.kerberosRealm = kerberosRealm;
    }

    public String getKerberosDelegationCredential() {
        return kerberosDelegationCredential;
    }

    public void setKerberosDelegationCredential(String kerberosDelegationCredential) {
        this.kerberosDelegationCredential = kerberosDelegationCredential;
    }

    public boolean isKerberosAuthenticated() {
        return this.kerberosAuthenticated;
    }

    public void setKerberosAuthenticated(boolean authenticated) {
        this.kerberosAuthenticated = authenticated;
    }

    public boolean isExpectedRealm(String kerberosRealm) {
        if (null == kerberosRealm || kerberosRealm.trim().isEmpty()) {
            return false;
        }
        return kerberosRealm.equals(this.kerberosRealm);
    }

    public String getKerberosResponseToken() {
        return kerberosResponseToken;
    }

    public void setKerberosResponseToken(String kerberosResponseToken) {
        this.kerberosResponseToken = kerberosResponseToken;
    }
}


