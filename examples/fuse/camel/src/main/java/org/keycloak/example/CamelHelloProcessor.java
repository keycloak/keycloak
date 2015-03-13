package org.keycloak.example;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.security.sasl.Sasl;
import javax.servlet.http.HttpServletRequest;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.ietf.jgss.GSSCredential;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.constants.KerberosConstants;
import org.keycloak.representations.AccessToken;
import org.keycloak.util.KerberosSerializationUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CamelHelloProcessor implements Processor {

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void process(Exchange exchange) throws Exception {
        HttpServletRequest req = exchange.getIn().getBody(HttpServletRequest.class);
        KeycloakPrincipal keycloakPrincipal = (KeycloakPrincipal) req.getUserPrincipal();
        AccessToken accessToken = keycloakPrincipal.getKeycloakSecurityContext().getToken();
        String username = accessToken.getPreferredUsername();

        // send a html response with fullName from LDAP
        exchange.getOut().setBody("<html><body>Hello " + username + "! It's " + sdf.format(new Date()) + "</body></html>");
    }
}
