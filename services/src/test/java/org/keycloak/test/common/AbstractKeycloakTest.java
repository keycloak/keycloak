package org.keycloak.test.common;

import org.jboss.resteasy.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelProvider;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.utils.ModelProviderUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@RunWith(Parameterized.class)
public abstract class AbstractKeycloakTest {

    private static final Logger log = Logger.getLogger(AbstractKeycloakTest.class);

    protected KeycloakSessionFactory factory;
    protected KeycloakSession identitySession;
    protected RealmManager realmManager;

    @Parameterized.Parameters
    public static Iterable<Object[]> parameters() {
        Iterable<ModelProvider> modelProviders;

        // We will run tests with all registered models if -Dkeycloak.model=all . Otherwise just with configured provider
        String configuredProvider = System.getProperty(ModelProviderUtils.MODEL_PROVIDER);
        if ("all".equalsIgnoreCase(configuredProvider)) {
            modelProviders = ModelProviderUtils.getRegisteredProviders();
        } else {
            ModelProvider provider = ModelProviderUtils.getConfiguredModelProvider();
            modelProviders = Arrays.asList(provider);
        }

        log.debug("Will use model providers: " + modelProviders);

        List<Object[]> params = new ArrayList<Object[]>();

        for (ModelProvider provider : modelProviders) {
            params.add(new Object[] { provider.getId() });
        }
        return params;
    }


    public AbstractKeycloakTest(String providerId) {
        System.setProperty(ModelProviderUtils.MODEL_PROVIDER, providerId);
    }

    @Before
    public void before() throws Exception {
        factory = KeycloakApplication.createSessionFactory();
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

}
