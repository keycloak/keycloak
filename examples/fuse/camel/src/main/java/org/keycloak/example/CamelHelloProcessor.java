package org.keycloak.example;

import javax.servlet.http.HttpServletRequest;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.representations.AccessToken;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CamelHelloProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        HttpServletRequest req = exchange.getIn().getBody(HttpServletRequest.class);
        KeycloakPrincipal keycloakPrincipal = (KeycloakPrincipal) req.getUserPrincipal();
        AccessToken accessToken = keycloakPrincipal.getKeycloakSecurityContext().getToken();
        String username = accessToken.getPreferredUsername();
        String fullName = accessToken.getName();

        exchange.getOut().setBody("Hello " + username + "! Your full name is " + fullName + ".");
    }
}
