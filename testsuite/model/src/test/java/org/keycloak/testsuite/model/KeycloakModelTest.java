/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.model;

import org.keycloak.Config.Scope;
import org.keycloak.authorization.AuthorizationSpi;
import org.keycloak.authorization.DefaultAuthorizationProviderFactory;
import org.keycloak.authorization.store.StoreFactorySpi;
import org.keycloak.cluster.ClusterSpi;
import org.keycloak.events.EventStoreSpi;
import org.keycloak.executors.DefaultExecutorsProviderFactory;
import org.keycloak.executors.ExecutorsSpi;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.ClientSpi;
import org.keycloak.models.GroupSpi;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmSpi;
import org.keycloak.models.RoleSpi;
import org.keycloak.models.UserSpi;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.ProviderManager;
import org.keycloak.provider.Spi;
import org.keycloak.services.DefaultKeycloakSession;
import org.keycloak.services.DefaultKeycloakSessionFactory;
import com.google.common.collect.ImmutableSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Assume;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Base of testcases that operate on session level. The tests derived from this class
 * will have access to a shared {@link KeycloakSessionFactory} in the {@link #FACTORY}
 * field that can be used to obtain a session and e.g. start / stop transaction.
 * <p>
 * This class expects {@code keycloak.model.parameters} system property to contain
 * comma-separated class names that implement {@link KeycloakModelParameters} interface
 * to provide list of factories and SPIs that are visible to the {@link KeycloakSessionFactory}
 * that is offered to the tests.
 * <p>
 * If no parameters are set via this property, the tests derived from this class are skipped.
 * @author hmlnarik
 */
public abstract class KeycloakModelTest {

    private static final Logger LOG = Logger.getLogger(KeycloakModelParameters.class);
    protected final Logger log = Logger.getLogger(getClass());

    @ClassRule
    public static final TestRule GUARANTEE_REQUIRED_FACTORY = new TestRule() {
        @Override
        public Statement apply(Statement base, Description description) {
            Class<?> testClass = description.getTestClass();
            Stream<RequireProvider> st = Stream.empty();
            while (testClass != Object.class) {
                st = Stream.concat(Stream.of(testClass.getAnnotationsByType(RequireProvider.class)), st);
                testClass = testClass.getSuperclass();
            }
            List<Class<? extends Provider>> notFound = st.map(RequireProvider::value)
              .filter(pClass -> FACTORY.getProviderFactory(pClass) == null)
              .collect(Collectors.toList());
            Assume.assumeThat("Some required providers not found", notFound, Matchers.empty());

            Statement res = base;
            for (KeycloakModelParameters kmp : KeycloakModelTest.MODEL_PARAMETERS) {
                res = kmp.classRule(res, description);
            }
            return res;
        }
    };

    @Rule
    public final TestRule guaranteeRequiredFactoryOnMethod = new TestRule() {
        @Override
        public Statement apply(Statement base, Description description) {
            Stream<RequireProvider> st = Optional.ofNullable(description.getAnnotation(RequireProviders.class))
              .map(RequireProviders::value)
              .map(Stream::of)
              .orElseGet(Stream::empty);

            RequireProvider rp = description.getAnnotation(RequireProvider.class);
            if (rp != null) {
                st = Stream.concat(st, Stream.of(rp));
            }

            for (Iterator<Class<? extends Provider>> iterator = st.map(RequireProvider::value).iterator(); iterator.hasNext();) {
                Class<? extends Provider> providerClass = iterator.next();

                if (FACTORY.getProviderFactory(providerClass) == null) {
                    return new Statement() {
                        @Override
                        public void evaluate() throws Throwable {
                            throw new AssumptionViolatedException("Provider must exist: " + providerClass);
                        }
                    };
                }
            }

            Statement res = base;
            for (KeycloakModelParameters kmp : KeycloakModelTest.MODEL_PARAMETERS) {
                res = kmp.instanceRule(res, description);
            }
            return res;
        }
    };

    private static final Set<Class<? extends Spi>> ALLOWED_SPIS = ImmutableSet.<Class<? extends Spi>>builder()
      .add(AuthorizationSpi.class)
      .add(ClientSpi.class)
      .add(ClusterSpi.class)
      .add(EventStoreSpi.class)
      .add(ExecutorsSpi.class)
      .add(GroupSpi.class)
      .add(RealmSpi.class)
      .add(RoleSpi.class)
      .add(StoreFactorySpi.class)
      .add(UserSpi.class)
      .build();

    private static final Set<Class<? extends ProviderFactory>> ALLOWED_FACTORIES = ImmutableSet.<Class<? extends ProviderFactory>>builder()
      .add(DefaultAuthorizationProviderFactory.class)
      .add(DefaultExecutorsProviderFactory.class)
      .build();

    protected static final List<KeycloakModelParameters> MODEL_PARAMETERS;
    protected static final DefaultKeycloakSessionFactory FACTORY;

    static {
        KeycloakModelParameters basicParameters = new KeycloakModelParameters(ALLOWED_SPIS, ALLOWED_FACTORIES);
        MODEL_PARAMETERS = Stream.concat(
          Stream.of(basicParameters),
          Stream.of(System.getProperty("keycloak.model.parameters", "").split("\\s*,\\s*"))
            .filter(s -> s != null && ! s.trim().isEmpty())
            .map(cn -> { try { return Class.forName(cn.indexOf('.') >= 0 ? cn : ("org.keycloak.testsuite.model.parameters." + cn)); } catch (Exception e) { LOG.error("Cannot find " + cn); return null; }})
            .filter(Objects::nonNull)
            .map(c -> { try { return c.newInstance(); } catch (Exception e) { LOG.error("Cannot instantiate " + c); return null; }} )
            .filter(KeycloakModelParameters.class::isInstance)
            .map(KeycloakModelParameters.class::cast)
          )
          .collect(Collectors.toList());

        FACTORY = new DefaultKeycloakSessionFactory() {
            @Override
            protected boolean isEnabled(ProviderFactory factory, Scope scope) {
                return super.isEnabled(factory, scope) && isFactoryAllowed(factory);
            }

            @Override
            protected Map<Class<? extends Provider>, Map<String, ProviderFactory>> loadFactories(ProviderManager pm) {
                spis.removeIf(s -> ! isSpiAllowed(s));
                return super.loadFactories(pm);
            }

            private boolean isSpiAllowed(Spi s) {
                return MODEL_PARAMETERS.stream().anyMatch(p -> p.isSpiAllowed(s));
            }

            private boolean isFactoryAllowed(ProviderFactory factory) {
                return MODEL_PARAMETERS.stream().anyMatch(p -> p.isFactoryAllowed(factory));
            }
        };
        FACTORY.init();
    }

    @BeforeClass
    public static void checkValidParameters() {
        Assume.assumeTrue("keycloak.model.parameters property must be set", MODEL_PARAMETERS.size() > 1);   // Additional parameters have to be set
    }

    protected void createEnvironment(KeycloakSession s) {
    }

    protected void cleanEnvironment(KeycloakSession s) {
    }

    @Before
    public void createEnvironment() {
        KeycloakModelUtils.runJobInTransaction(FACTORY, this::createEnvironment);
    }

    @After
    public void cleanEnvironment() {
        KeycloakModelUtils.runJobInTransaction(FACTORY, this::cleanEnvironment);
    }

    protected <T> Stream<T> getParameters(Class<T> clazz) {
        return MODEL_PARAMETERS.stream().flatMap(mp -> mp.getParameters(clazz)).filter(Objects::nonNull);
    }

    protected <T> void withEach(Class<T> parameterClazz, Consumer<T> what) {
        getParameters(parameterClazz).forEach(what);
    }

    protected <T> void inRolledBackTransaction(T parameter, BiConsumer<KeycloakSession, T> what) {
        KeycloakSession session = new DefaultKeycloakSession(FACTORY);
        session.getTransactionManager().begin();

        what.accept(session, parameter);

        session.getTransactionManager().rollback();
    }

    protected <T> void inComittedTransaction(T parameter, BiConsumer<KeycloakSession, T> what) {
        inComittedTransaction(parameter, what, (a,b) -> {}, (a,b) -> {});
    }

    protected <T> void inComittedTransaction(T parameter, BiConsumer<KeycloakSession, T> what, BiConsumer<KeycloakSession, T> onCommit, BiConsumer<KeycloakSession, T> onRollback) {
        KeycloakModelUtils.runJobInTransaction(FACTORY, session -> {
            session.getTransactionManager().enlistAfterCompletion(new AbstractKeycloakTransaction() {
                @Override
                protected void commitImpl() {
                    if (onCommit != null) { onCommit.accept(session, parameter); }
                }

                @Override
                protected void rollbackImpl() {
                    if (onRollback != null) { onRollback.accept(session, parameter); }
                }
            });
            what.accept(session, parameter);
        });
    }

}
