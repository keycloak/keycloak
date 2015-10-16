package org.keycloak.testsuite.federation;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.security.sasl.Sasl;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ietf.jgss.GSSCredential;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.common.util.KerberosSerializationUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosCredDelegServlet extends HttpServlet {

    public static final String CRED_DELEG_TEST_PATH = "/cred-deleg-test";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String ldapData = null;

        if (req.getRequestURI().endsWith(CRED_DELEG_TEST_PATH)) {

            try {
                // Retrieve kerberos credential from accessToken and deserialize it
                KeycloakPrincipal keycloakPrincipal = (KeycloakPrincipal) req.getUserPrincipal();
                String serializedGssCredential = (String) keycloakPrincipal.getKeycloakSecurityContext().getToken().getOtherClaims().get(KerberosConstants.GSS_DELEGATION_CREDENTIAL);
                GSSCredential gssCredential = KerberosSerializationUtils.deserializeCredential(serializedGssCredential);

                // First try to invoke without gssCredential. It should fail
                try {
                    invokeLdap(null);
                    throw new RuntimeException("Not expected to authenticate to LDAP without credential");
                } catch (NamingException nse) {
                    System.out.println("Expected exception: " + nse.getMessage());
                }

                ldapData = invokeLdap(gssCredential);
            } catch (KerberosSerializationUtils.KerberosSerializationException kse) {
                System.err.println("KerberosSerializationUtils.KerberosSerializationException: " + kse.getMessage());
                ldapData = "ERROR";
            } catch (Exception e) {
                e.printStackTrace();
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }

        resp.setContentType("text/html");
        PrintWriter pw = resp.getWriter();
        pw.printf("<html><head><title>%s</title></head><body>", "Kerberos Test");
        pw.printf("Kerberos servlet secured content<br>");

        if (ldapData != null) {
            pw.printf("LDAP Data: " + ldapData + "<br>");
        }

        pw.print("</body></html>");
        pw.flush();


    }

    private String invokeLdap(GSSCredential gssCredential) throws NamingException {
        Hashtable env = new Hashtable(11);
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://localhost:10389");

        if (gssCredential != null) {
            env.put(Context.SECURITY_AUTHENTICATION, "GSSAPI");
            env.put(Sasl.CREDENTIALS, gssCredential);
        }

        DirContext ctx = new InitialDirContext(env);
        try {
            Attributes attrs = ctx.getAttributes("uid=hnelson,ou=People,dc=keycloak,dc=org");
            String cn = (String) attrs.get("cn").get();
            String sn = (String) attrs.get("sn").get();
            return cn + " " + sn;
        } finally {
            ctx.close();
        }
    }

}
