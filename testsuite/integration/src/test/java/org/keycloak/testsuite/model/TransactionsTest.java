package org.keycloak.testsuite.model;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.testsuite.rule.KeycloakRule;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TransactionsTest {

    @ClassRule
    public static KeycloakRule kc = new KeycloakRule();

    @Test
    public void testTransactionActive() {
        KeycloakSession session = kc.startSession();

        Assert.assertTrue(session.getTransaction().isActive());
        session.getTransaction().commit();
        Assert.assertFalse(session.getTransaction().isActive());

        session.getTransaction().begin();
        Assert.assertTrue(session.getTransaction().isActive());
        session.getTransaction().rollback();
        Assert.assertFalse(session.getTransaction().isActive());

        session.close();
    }
}
