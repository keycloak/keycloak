package org.keycloak.services.managers;

import org.jboss.logging.Logger;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import java.util.Hashtable;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPConnectionTestManager {

    protected static final Logger logger = Logger.getLogger(LDAPConnectionTestManager.class);

    public static final String TEST_CONNECTION = "testConnection";
    public static final String TEST_AUTHENTICATION = "testAuthentication";

    public boolean testLDAP(String action, String connectionUrl, String bindDn, String bindCredential) {
        if (!TEST_CONNECTION.equals(action) && !TEST_AUTHENTICATION.equals(action)) {
            logger.error("Unknown action: " + action);
            return false;
        }

        Context ldapContext = null;
        try {
            Hashtable<String, Object> env = new Hashtable<String, Object>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.SECURITY_AUTHENTICATION, "simple");

            env.put(Context.PROVIDER_URL, connectionUrl);

            if (TEST_AUTHENTICATION.equals(action)) {
                env.put(Context.SECURITY_PRINCIPAL, bindDn);

                char[] bindCredentialChar = null;
                if (bindCredential != null) {
                    bindCredentialChar = bindCredential.toCharArray();
                }
                env.put(Context.SECURITY_CREDENTIALS, bindCredentialChar);
            }

            ldapContext = new InitialLdapContext(env, null);
            return true;
        } catch (Exception ne) {
            String errorMessage = (TEST_AUTHENTICATION.equals(action)) ? "Error when authenticating to LDAP: " : "Error when connecting to LDAP: ";
            logger.error(errorMessage + ne.getMessage(), ne);
            return false;
        } finally {
            if (ldapContext != null) {
                try {
                    ldapContext.close();
                } catch (NamingException ne) {
                    logger.warn("Error when closing LDAP connection", ne);
                }
            }
        }
    }
}
