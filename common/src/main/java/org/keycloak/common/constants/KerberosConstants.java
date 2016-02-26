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

package org.keycloak.common.constants;

import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosConstants {

    /**
     *  Value of HTTP Headers "WWW-Authenticate" or "Authorization" used for SPNEGO/Kerberos
     **/
    public static final String NEGOTIATE = "Negotiate";


    /**
     * OID of SPNEGO mechanism. See http://www.oid-info.com/get/1.3.6.1.5.5.2
     */
    private static final String SPNEGO_OID_STR = "1.3.6.1.5.5.2";
    public static final Oid SPNEGO_OID;


    /**
     * OID of Kerberos v5 mechanism. See http://www.oid-info.com/get/1.2.840.113554.1.2.2
     */
    private static final String KRB5_OID_STR = "1.2.840.113554.1.2.2";
    public static final Oid KRB5_OID;


    /**
     * OID of Kerberos v5 name. See http://www.oid-info.com/get/1.2.840.113554.1.2.2.1
     */
    private static final String KRB5_NAME_OID_STR = "1.2.840.113554.1.2.2.1";
    public static final Oid KRB5_NAME_OID;


    static {
        try {
            KRB5_OID = new Oid(KerberosConstants.KRB5_OID_STR);
            KRB5_NAME_OID = new Oid(KerberosConstants.KRB5_NAME_OID_STR);
            SPNEGO_OID = new Oid(KerberosConstants.SPNEGO_OID_STR);
        } catch (GSSException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Configuration federation provider model attributes.
     */
    public static final String ALLOW_KERBEROS_AUTHENTICATION = "allowKerberosAuthentication";
    public static final String KERBEROS_REALM = "kerberosRealm";
    public static final String SERVER_PRINCIPAL = "serverPrincipal";
    public static final String KEYTAB = "keyTab";
    public static final String DEBUG = "debug";
    public static final String ALLOW_PASSWORD_AUTHENTICATION = "allowPasswordAuthentication";
    public static final String UPDATE_PROFILE_FIRST_LOGIN = "updateProfileFirstLogin";
    public static final String USE_KERBEROS_FOR_PASSWORD_AUTHENTICATION = "useKerberosForPasswordAuthentication";


    /**
     * Internal attribute used in "state" map . Contains token to be passed in HTTP Response back to browser to continue handshake
     */
    public static final String RESPONSE_TOKEN = "SpnegoResponseToken";


    /**
     * Internal attribute used in "userSession.note" map and in accessToken claims . Contains credential from SPNEGO/Kerberos successful authentication
     */
    public static final String GSS_DELEGATION_CREDENTIAL = "gss_delegation_credential";

    /**
     * Display name for the above in admin console and consent screens
     */
    public static final String GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME = "gss delegation credential";

}
