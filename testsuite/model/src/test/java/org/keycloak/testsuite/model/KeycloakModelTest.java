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
import org.keycloak.models.ClientScopeSpi;
import org.keycloak.models.ClientSpi;
import org.keycloak.models.GroupSpi;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmSpi;
import org.keycloak.models.RoleSpi;
import org.keycloak.models.UserLoginFailureSpi;
import org.keycloak.models.UserSessionSpi;
import org.keycloak.models.UserSpi;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
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
import org.keycloak.timer.TimerSpi;

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
            List<Class<? extends Provider>> notFound = st
              .filter(rp -> rp.only().length == 0 
                ? FACTORY.getProviderFactory(rp.value()) == null
                : Stream.of(rp.only()).allMatch(provider -> FACTORY.getProviderFactory(rp.value(), provider) == null))
              .map(RequireProvider::value)
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

            for (Iterator<RequireProvider> iterator = st.iterator(); iterator.hasNext();) {
                RequireProvider rpInner = iterator.next();
                Class<? extends Provider> providerClass = rpInner.value();
                String[] only = rpInner.only();

                if (only.length == 0) {
                    if (FACTORY.getProviderFactory(providerClass) == null) {
                        return new Statement() {
                            @Override
                            public void evaluate() throws Throwable {
                                throw new AssumptionViolatedException("Provider must exist: " + providerClass);
                            }
                        };
                    }
                } else {
                    boolean notFoundAny = Stream.of(only).allMatch(provider -> FACTORY.getProviderFactory(providerClass, provider) == null);
                    if (notFoundAny) {
                        return new Statement() {
                            @Override
                            public void evaluate() throws Throwable {
                                throw new AssumptionViolatedException("Provider must exist: " + providerClass + " one of [" + String.join(",", only) + "]");
                            }
                        };
                    }
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
      .add(ClientScopeSpi.class)
      .add(ClientSpi.class)
      .add(ClusterSpi.class)
      .add(EventStoreSpi.class)
      .add(ExecutorsSpi.class)
      .add(GroupSpi.class)
      .add(RealmSpi.class)
      .add(RoleSpi.class)
      .add(StoreFactorySpi.class)
      .add(TimerSpi.class)
      .add(UserLoginFailureSpi.class)
      .add(UserSessionSpi.class)
      .add(UserSpi.class)
      .build();

    private static final Set<Class<? extends ProviderFactory>> ALLOWED_FACTORIES = ImmutableSet.<Class<? extends ProviderFactory>>builder()
      .add(DefaultAuthorizationProviderFactory.class)
      .add(DefaultExecutorsProviderFactory.class)
      .build();

    protected static final List<KeycloakModelParameters> MODEL_PARAMETERS;
    protected static Config CONFIG;
    protected static DefaultKeycloakSessionFactory FACTORY;

    static {
        KeycloakModelParameters basicParameters = new KeycloakModelParameters(ALLOWED_SPIS, ALLOWED_FACTORIES);
        MODEL_PARAMETERS = Stream.concat(
          Stream.of(basicParameters),
          Stream.of(System.getProperty("keycloak.model.parameters", "").split("\\s*,\\s*"))
            .filter(s -> s != null && ! s.trim().isEmpty())
            .map(cn -> { try { return Class.forName(cn.indexOf('.') >= 0 ? cn : ("org.keycloak.testsuite.model.parameters." + cn)); } catch (Exception e) { LOG.error("Cannot find " + cn); return null; }})
            .filter(Objects::nonNull)
            .map(c -> { try { return c.getDeclaredConstructor().newInstance(); } catch (Exception e) { LOG.error("Cannot instantiate " + c); return null; }} )
            .filter(KeycloakModelParameters.class::isInstance)
            .map(KeycloakModelParameters.class::cast)
          )
          .collect(Collectors.toList());

        reinitializeKeycloakSessionFactory();
    }

    public static DefaultKeycloakSessionFactory createKeycloakSessionFactory() {
        CONFIG = new Config();
        MODEL_PARAMETERS.forEach(m -> m.updateConfig(CONFIG));
        LOG.debug("Using the following configuration:\n    " + CONFIG);
        org.keycloak.Config.init(CONFIG);

        DefaultKeycloakSessionFactory res = new DefaultKeycloakSessionFactory() {
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
        res.init();
        res.publish(new PostMigrationEvent());
        return res;
    }

    public static void reinitializeKeycloakSessionFactory() {
        if (FACTORY != null) {
            FACTORY.close();
        }
        FACTORY = createKeycloakSessionFactory();
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

    protected <T, R> R inComittedTransaction(T parameter, BiFunction<KeycloakSession, T, R> what) {
        return inComittedTransaction(parameter, what, null, null);
    }

    protected void inComittedTransaction(Consumer<KeycloakSession> what) {
        inComittedTransaction(a -> { what.accept(a); return null; });
    }

    protected <R> R inComittedTransaction(Function<KeycloakSession, R> what) {
        return inComittedTransaction(1, (a,b) -> what.apply(a));
    }

    protected <T, R> R inComittedTransaction(T parameter, BiFunction<KeycloakSession, T, R> what, BiConsumer<KeycloakSession, T> onCommit, BiConsumer<KeycloakSession, T> onRollback) {
        AtomicReference<R> res = new AtomicReference<>();
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
            res.set(what.apply(session, parameter));
        });
        return res.get();
    }

    protected <R> R withRealm(String realmId, BiFunction<KeycloakSession, RealmModel, R> what) {
        return inComittedTransaction(session -> {
            final RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            return what.apply(session, realm);
        });
    }

}
