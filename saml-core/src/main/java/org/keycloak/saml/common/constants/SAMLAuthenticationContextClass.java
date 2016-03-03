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

package org.keycloak.saml.common.constants;

/**
 * <p>A enum that maps a alias for each SAML Authentication Context Class.</p>
 *
 * @author pedroigor
 */
public enum SAMLAuthenticationContextClass {

    INTERNET_PROTOCOL("internetProtocol", "urn:oasis:names:tc:SAML:2.0:ac:classes:InternetProtocol"),
    INTERNET_PROTOCOL_PASSWORD("internetProtocolPassword", "urn:oasis:names:tc:SAML:2.0:ac:classes:InternetProtocolPassword"),
    KERBEROS("kerberos", "urn:oasis:names:tc:SAML:2.0:ac:classes:Kerberos"),
    MOBILE_ONE_FACTOR_UNREGISTERED("mobileOneFactorUnregistered", "urn:oasis:names:tc:SAML:2.0:ac:classes:MobileOneFactorUnregistered"),
    MOBILE_TWO_FACTOR_UNREGISTERED("mobileTwoFactorUnregistered", "urn:oasis:names:tc:SAML:2.0:ac:classes:MobileTwoFactorUnregistered"),
    MOBILE_ONE_FACTOR_CONTRACT("mobileOneFactorContract", "urn:oasis:names:tc:SAML:2.0:ac:classes:MobileOneFactorContract"),
    MOBILE_TWO_FACTOR_CONTRACT("mobileTwoFactorContract", "urn:oasis:names:tc:SAML:2.0:ac:classes:MobileTwoFactorContract"),
    PASSWORD("password", "urn:oasis:names:tc:SAML:2.0:ac:classes:password"),
    PASSWORD_PROTECTED_TRANSPORT("passwordProtectedTransport", "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport"),
    PREVIOUS_SESSION("previousSession", "urn:oasis:names:tc:SAML:2.0:ac:classes:PreviousSession"),
    X509("X509", "urn:oasis:names:tc:SAML:2.0:ac:classes:X509"),
    PGP("PGP", "urn:oasis:names:tc:SAML:2.0:ac:classes:PGP"),
    SPKI("SPKI", "urn:oasis:names:tc:SAML:2.0:ac:classes:SPKI"),
    XMLDSig("XMLDSig", "urn:oasis:names:tc:SAML:2.0:ac:classes:XMLDSig"),
    SMARTCARD("smartcard", "urn:oasis:names:tc:SAML:2.0:ac:classes:Smartcard"),
    SMARTCARD_PKI("smartcardPKI", "urn:oasis:names:tc:SAML:2.0:ac:classes:SmartcardPKI"),
    SOFTWARE_PKI("softwarePKI", "urn:oasis:names:tc:SAML:2.0:ac:classes:SoftwarePKI"),
    TELEPHONY("telephony", "urn:oasis:names:tc:SAML:2.0:ac:classes:Telephony"),
    NOMAD_TELEPHONY("nomadTelephony", "urn:oasis:names:tc:SAML:2.0:ac:classes:NomadTelephony"),
    PERSONAL_TELEPHONY("personalTelephony", "urn:oasis:names:tc:SAML:2.0:ac:classes:PersonalTelephony"),
    AUTHENTICATED_TELEPHONY("authenticatedTelephony", "urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony"),
    SECURE_REMOTE_PASSWORD("secureRemotePassword", "urn:oasis:names:tc:SAML:2.0:ac:classes:SecureRemotePassword"),
    TLSClient("TLSClient", "urn:oasis:names:tc:SAML:2.0:ac:classes:TLSClient"),
    TIME_SYNC_TOKEN("timeSyncToken", "urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken"),
    UNSPECIFIED("unspecified", "urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified");

    private final String alias;
    private final String fqn;

    SAMLAuthenticationContextClass(final String alias, final String fqn) {
        this.alias = alias;
        this.fqn = fqn;
    }

    public String getAlias() {
        return this.alias;
    }

    public String getFqn() {
        return this.fqn;
    }

    public static SAMLAuthenticationContextClass forAlias(String alias) {
        if (alias != null) {
            for (SAMLAuthenticationContextClass contextClass: values()) {
                if (contextClass.getAlias().equals(alias.trim())) {
                    return contextClass;
                }
            }
        }

        return null;
    }
}
