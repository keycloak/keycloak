package org.keycloak.testsuite.performance;

import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class BaseJMeterPerformanceTest extends AbstractJavaSamplerClient {


    private static FutureTask<KeycloakSessionFactory> factoryProvider = new FutureTask<KeycloakSessionFactory>(new Callable() {

        @Override
        public KeycloakSessionFactory call() throws Exception {
            KeycloakSessionFactory factory = KeycloakApplication.createSessionFactory();

            // TODO: Workaround due to bouncycastle classpath issues. Should be fixed properly
            // new ApplianceBootstrap().bootstrap(factory, "/auth");
            bootstrapAdminRealm(factory, "/auth");

            return factory;
        }

        private void bootstrapAdminRealm(KeycloakSessionFactory factory, String contextPath) {
            KeycloakSession keycloakSession = factory.create();
            keycloakSession.getTransaction().begin();

            try {
                String adminRealmName = Config.getAdminRealm();
                if (keycloakSession.realms().getRealm(adminRealmName) == null) {

                    RealmManager manager = new RealmManager(keycloakSession);
                    manager.setContextPath(contextPath);
                    RealmModel realm = manager.createRealm(adminRealmName, adminRealmName);
                    realm.setName(adminRealmName);
                    realm.setEnabled(true);
                }

                keycloakSession.getTransaction().commit();
            } finally {
                keycloakSession.close();
            }
        }

    });
    private static AtomicInteger counter = new AtomicInteger();

    private KeycloakSessionFactory factory;
    // private KeycloakSession session;
    private Worker worker;
    private boolean setupSuccess = false;


    // Executed once per JMeter thread
    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);

        worker = getWorker();

        factory = getFactory();
        getLogger().info("Retrieved factory: " + factory);
        KeycloakSession session = factory.create();
        KeycloakTransaction transaction = session.getTransaction();
        transaction.begin();

        int workerId = counter.getAndIncrement();
        try {
            worker.setup(workerId, session);
            setupSuccess = true;
        } finally {
            if (setupSuccess) {
                transaction.commit();
            } else {
                transaction.rollback();
            }
            session.close();
        }
    }

    private static KeycloakSessionFactory getFactory() {
        factoryProvider.run();
        try {
            return factoryProvider.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private Worker getWorker() {
        String workerClass = System.getProperty("keycloak.perf.workerClass");
        if (workerClass == null) {
            throw new IllegalArgumentException("System property keycloak.perf.workerClass needs to be provided");
        }

        try {
            Class workerClazz = Class.forName(workerClass);
            return (Worker)workerClazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();
        result.sampleStart();

        if (!setupSuccess) {
            getLogger().error("setupTest didn't executed successfully. Skipping");
            result.setResponseCode("500");
            result.sampleEnd();
            result.setSuccessful(true);
            return result;
        }

        KeycloakSession session = factory.create();
        KeycloakTransaction transaction = session.getTransaction();
        try {
            transaction.begin();

            worker.run(result, session);

            result.setResponseCodeOK();
            transaction.commit();
        } catch (Exception e) {
            getLogger().error("Error during worker processing", e);
            result.setResponseCode("500");
            transaction.rollback();
        } finally {
            result.sampleEnd();
            result.setSuccessful(true);
            session.close();
        }

        return result;
    }


    // Executed once per JMeter thread
    @Override
    public void teardownTest(JavaSamplerContext context) {
        super.teardownTest(context);

        if (worker != null) {
            worker.tearDown();
        }

        // TODO: Assumption is that tearDownTest is executed for each setupTest. Verify if it's always true...
        if (counter.decrementAndGet() == 0) {
            if (factory != null) {
                factory.close();
            }
        }
    }
}
