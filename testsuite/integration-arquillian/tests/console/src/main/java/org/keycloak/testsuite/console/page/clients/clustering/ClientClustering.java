package org.keycloak.testsuite.console.page.clients.clustering;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.console.page.clients.Client;

/**
 *
 * @author tkyjovsk
 */
public class ClientClustering extends Client {

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/clustering";
    }
    
    @Page
    private ClientClusteringForm clientClusteringForm;
    
    public ClientClusteringForm form() {
        return clientClusteringForm;
    }

}
