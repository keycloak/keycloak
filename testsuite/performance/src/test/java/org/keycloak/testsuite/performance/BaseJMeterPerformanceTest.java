package org.keycloak.testsuite.performance;

import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.provider.ProviderSession;
import org.keycloak.provider.ProviderSessionFactory;
import org.keycloak.services.resources.KeycloakApplication;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class BaseJMeterPerformanceTest extends AbstractJavaSamplerClient {


    private static FutureTask<ProviderSessionFactory> factoryProvider = new FutureTask<ProviderSessionFactory>(new Callable() {

        @Override
        public ProviderSessionFactory call() throws Exception {
            return KeycloakApplication.createProviderSessionFactory();
        }

    });
    private static AtomicInteger counter = new AtomicInteger();

    private ProviderSessionFactory factory;
    // private KeycloakSession identitySession;
    private Worker worker;
    private boolean setupSuccess = false;


    // Executed once per JMeter thread
    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);

        worker = getWorker();

        factory = getFactory();
        ProviderSession providerSession = factory.createSession();
        KeycloakSession identitySession = providerSession.getProvider(KeycloakSession.class);
        KeycloakTransaction transaction = identitySession.getTransaction();
        transaction.begin();

        int workerId = counter.getAndIncrement();
        try {
            worker.setup(workerId, identitySession);
            setupSuccess = true;
        } finally {
            if (setupSuccess) {
                transaction.commit();
            } else {
                transaction.rollback();
            }
            providerSession.close();
        }
    }

    private static ProviderSessionFactory getFactory() {
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

        ProviderSession providerSession = factory.createSession();
        KeycloakSession identitySession = providerSession.getProvider(KeycloakSession.class);
        KeycloakTransaction transaction = identitySession.getTransaction();
        try {
            transaction.begin();

            worker.run(result, identitySession);

            result.setResponseCodeOK();
            transaction.commit();
        } catch (Exception e) {
            getLogger().error("Error during worker processing", e);
            result.setResponseCode("500");
            transaction.rollback();
        } finally {
            result.sampleEnd();
            result.setSuccessful(true);
            providerSession.close();
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
