package org.keycloak.broker.kerberos;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosConstants {

    /**
     *  Value of HTTP Headers "WWW-Authenticate" or "Authorization" used for SPNEGO/Kerberos
     **/
    public static final String NEGOTIATE = "Negotiate";


    /**
     * Helper parameter for relay state
     */
    public static final String RELAY_STATE_PARAM = "RelayState";


    /**
     * OID of SPNEGO mechanism. See http://www.oid-info.com/get/1.3.6.1.5.5.2
     */
    public static final String SPNEGO_OID = "1.3.6.1.5.5.2";

    /**
     * OID of Kerberos v5 mechanism. See http://www.oid-info.com/get/1.2.840.113554.1.2.2
     */
    public static final String KRB5_OID = "1.2.840.113554.1.2.2";

}
