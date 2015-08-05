package org.keycloak.testsuite.account;

import org.jboss.arquillian.graphene.page.Page;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.auth.page.account.Account;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractAccountTest extends AbstractAuthTest {

    @Page
    protected Account account;
    
}
