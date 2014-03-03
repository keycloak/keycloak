package org.keycloak.example.oauth;

import org.jboss.resteasy.annotations.cache.NoCache;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("customers")
public class CustomerService {
    @GET
    @Produces("application/json")
    @NoCache
    public List<String> getCustomers() {
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
