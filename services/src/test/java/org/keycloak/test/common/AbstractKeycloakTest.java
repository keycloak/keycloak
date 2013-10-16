package org.keycloak.test.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@RunWith(Parameterized.class)
public abstract class AbstractKeycloakTest {

    protected static final SessionFactoryTestContext[] TEST_CONTEXTS;

    private final SessionFactoryTestContext testContext;
    protected KeycloakSessionFactory factory;
    protected KeycloakSession identitySession;
    protected RealmManager realmManager;

    // STATIC METHODS

    static
    {
        // TODO: MongoDB disabled by default
        TEST_CONTEXTS = new SessionFactoryTestContext[] {
                new PicketlinkSessionFactoryTestContext(),
                // new MongoDBSessionFactoryTestContext()
        };
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> parameters() {
        List<Object[]> params = new ArrayList<Object[]>();

        for (SessionFactoryTestContext testContext : TEST_CONTEXTS) {
            params.add(new Object[] {testContext});
        }
        return params;
    }

    @BeforeClass
    public static void baseBeforeClass() {
        for (SessionFactoryTestContext testContext : TEST_CONTEXTS) {
            testContext.beforeTestClass();
        }
    }

    @AfterClass
    public static void baseAfterClass() {
        for (SessionFactoryTestContext testContext : TEST_CONTEXTS) {
            testContext.afterTestClass();
        }
    }

    // NON-STATIC METHODS

    public AbstractKeycloakTest(SessionFactoryTestContext testContext) {
        this.testContext = testContext;
    }

    @Before
    public void before() throws Exception {
        testContext.initEnvironment();
        factory = KeycloakApplication.buildSessionFactory();
        identitySession = factory.createSession();
        identitySession.getTransaction().begin();
        realmManager = new RealmManager(identitySession);
    }

    @After
    public void after() throws Exception {
        identitySession.getTransaction().commit();
        identitySession.close();
        factory.close();
    }

    protected RealmManager getRealmManager() {
        return realmManager;
    }

    protected KeycloakSession getIdentitySession() {
        return identitySession;
    }

}
