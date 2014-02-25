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
@Path("products")
public class ProductService {
    @GET
    @Produces("application/json")
    @NoCache
    public List<String> getProducts() {
        ArrayList<String> rtn = new ArrayList<String>();
        rtn.add("iphone");
        rtn.add("ipad");
        rtn.add("ipod");
        return rtn;
    }
}
