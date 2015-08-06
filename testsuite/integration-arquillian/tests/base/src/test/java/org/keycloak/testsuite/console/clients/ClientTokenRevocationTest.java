package org.keycloak.testsuite.console.clients;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Ignore;
import org.keycloak.testsuite.console.page.clients.ClientRevocation;

/**
 *
 * @author tkyjovsk
 */
@Ignore
public class ClientTokenRevocationTest extends AbstractClientTest {
    
    @Page
    private ClientRevocation clientRevocation;
    
}
