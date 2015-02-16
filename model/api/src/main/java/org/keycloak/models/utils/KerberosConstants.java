package org.keycloak.models.utils;

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
    public static final String SPNEGO_OID = "1.3.6.1.5.5.2";

    /**
     * OID of Kerberos v5 mechanism. See http://www.oid-info.com/get/1.2.840.113554.1.2.2
     */
    public static final String KRB5_OID = "1.2.840.113554.1.2.2";

    /**
     * Configuration federation provider model attribute. It's always true for KerberosFederationProvider and configurable for LDAPFederationProvider
     */
    public static final String ALLOW_KERBEROS_AUTHENTICATION = "allowKerberosAuthentication";

    /**
     * Internal attribute used in "state" map . Contains token to be passed in HTTP Response back to browser to continue handshake
     */
    public static final String RESPONSE_TOKEN = "SpnegoResponseToken";

    /**
     * Internal attribute used in "state" map . Contains credential from SPNEGO/Kerberos successful authentication
     */
    public static final String GSS_DELEGATION_CREDENTIAL = "GssDelegationCredential";

}
