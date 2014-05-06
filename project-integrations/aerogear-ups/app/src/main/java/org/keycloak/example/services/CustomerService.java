package org.keycloak.example.services;

import org.jboss.resteasy.annotations.cache.NoCache;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Path("customers")
public class CustomerService {

    @Inject
    private CustomerDataProvider provider;

    @GET
    @Produces("application/json")
    @NoCache
    public List<String> getCustomers() {
          return provider.getCustomers();
    }
}
