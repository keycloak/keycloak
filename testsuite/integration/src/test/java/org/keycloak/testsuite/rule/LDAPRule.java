package org.keycloak.testsuite.rule;

import org.junit.rules.ExternalResource;
import org.keycloak.testutils.LDAPEmbeddedServer;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPRule extends ExternalResource {

    private LDAPEmbeddedServer embeddedServer;

    @Override
    protected void before() throws Throwable {
        try {
            embeddedServer = new LDAPEmbeddedServer();
            embeddedServer.setup();
            embeddedServer.importLDIF("ldap/users.ldif");
        } catch (Exception e) {
            throw new RuntimeException("Error starting Embedded LDAP server.", e);
        }
    }

    @Override
    protected void after() {
        try {
            embeddedServer.tearDown();
            embeddedServer = null;
        } catch (Exception e) {
            throw new RuntimeException("Error tearDown Embedded LDAP server.", e);
        }
    }

    public LDAPEmbeddedServer getEmbeddedServer() {
        return embeddedServer;
    }
}
