package org.keycloak.example.oauth;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("customers")
public class CustomerService {

    @Context
    private HttpRequest httpRequest;

    @GET
    @Produces("application/json")
    @NoCache
    public List<String> getCustomers() {
        // Just to show how to user info from access token in REST endpoint
        KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest.getAttribute(KeycloakSecurityContext.class.getName());
        AccessToken accessToken = securityContext.getToken();
        System.out.println(String.format("User '%s' with email '%s' made request to CustomerService REST endpoint", accessToken.getPreferredUsername(), accessToken.getEmail()));

        ArrayList<String> rtn = new ArrayList<String>();
        rtn.add("Bill Burke");
        rtn.add("Stian Thorgersen");
        rtn.add("Stan Silvert");
        rtn.add("Gabriel Cardoso");
        rtn.add("Viliam Rockai");
        rtn.add("Marek Posolda");
        rtn.add("Boleslaw Dawidowicz");
        return rtn;
    }
}
