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

package org.keycloak.credential;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Used just in cases when we want to "directly" update or retrieve the hash or salt of user credential (For example during export/import)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CredentialModel implements Serializable {

    @Deprecated /** Use PasswordCredentialModel.TYPE instead **/
    public static final String PASSWORD = "password";

    @Deprecated /** Use PasswordCredentialModel.PASSWORD_HISTORY instead **/
    public static final String PASSWORD_HISTORY = "password-history";

    @Deprecated /** Use OTPCredentialModel.TYPE instead **/
    public static final String OTP = "otp";

    @Deprecated /** Use OTPCredentialModel.TOTP instead **/
    public static final String TOTP = "totp";

    @Deprecated /** Use OTPCredentialModel.HOTP instead **/
    public static final String HOTP = "hotp";

    // Secret is same as password but it is not hashed
    public static final String SECRET = "secret";
    public static final String CLIENT_CERT = "cert";
    public static final String KERBEROS = "kerberos";


    private String id;
    private String type;
    private String userLabel;
    private Long createdDate;

    private String secretData;
    private String credentialData;

    public CredentialModel shallowClone() {
        CredentialModel res = new CredentialModel();
        res.id = id;
        res.type = type;
        res.userLabel = userLabel;
        res.createdDate = createdDate;
        res.secretData = secretData;
        res.credentialData = credentialData;
        return res;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public String getUserLabel() {
        return userLabel;
    }
    public void setUserLabel(String userLabel) {
        this.userLabel = userLabel;
    }

    public Long getCreatedDate() {
        return createdDate;
    }
    public void setCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
    }

    public String getSecretData() {
        return secretData;
    }
    public void setSecretData(String secretData) {
        this.secretData = secretData;
    }

    public String getCredentialData() {
        return credentialData;
    }
    public void setCredentialData(String credentialData) {
        this.credentialData = credentialData;
    }

    public static Comparator<CredentialModel> comparingByStartDateDesc() {
        return (o1, o2) -> { // sort by date descending
            Long o1Date = o1.getCreatedDate() == null ? Long.MIN_VALUE : o1.getCreatedDate();
            Long o2Date = o2.getCreatedDate() == null ? Long.MIN_VALUE : o2.getCreatedDate();
            return (-o1Date.compareTo(o2Date));
        };
    }
}
