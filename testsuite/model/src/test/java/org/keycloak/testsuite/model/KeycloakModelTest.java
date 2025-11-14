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

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.keycloak.Config.Scope;
import org.keycloak.authorization.AuthorizationSpi;
import org.keycloak.authorization.DefaultAuthorizationProviderFactory;
import org.keycloak.authorization.policy.provider.PolicyProviderFactory;
import org.keycloak.authorization.policy.provider.PolicySpi;
import org.keycloak.authorization.store.StoreFactorySpi;
import org.keycloak.cluster.ClusterSpi;
import org.keycloak.common.Profile;
import org.keycloak.common.profile.PropertiesProfileConfigResolver;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentFactoryProviderFactory;
import org.keycloak.component.ComponentFactorySpi;
import org.keycloak.events.EventStoreSpi;
import org.keycloak.executors.DefaultExecutorsProviderFactory;
import org.keycloak.executors.ExecutorsSpi;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.ClientScopeSpi;
import org.keycloak.models.ClientSpi;
import org.keycloak.models.DeploymentStateProviderFactory;
import org.keycloak.models.DeploymentStateSpi;
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
import org.keycloak.services.DefaultComponentFactoryProviderFactory;
import org.keycloak.services.DefaultKeycloakSessionFactory;
import org.keycloak.services.resteasy.ResteasyKeycloakSessionFactory;
import org.keycloak.spi.infinispan.CacheRemoteConfigProviderFactory;
import org.keycloak.spi.infinispan.CacheRemoteConfigProviderSpi;
import org.keycloak.storage.DatastoreProviderFactory;
import org.keycloak.storage.DatastoreSpi;
import org.keycloak.timer.TimerSpi;
import org.keycloak.tracing.TracingProviderFactory;
import org.keycloak.tracing.TracingSpi;

import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Base of testcases that operate on session level. The tests derived from this class
 * will have access to a shared {@link KeycloakSessionFactory} in the {@link #LOCAL_FACTORY}
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
    private static final AtomicInteger FACTORY_COUNT = new AtomicInteger();
    protected final Logger log = Logger.getLogger(getClass());
    private static final List<String> MAIN_THREAD_NAMES = Arrays.asList("main", "Time-limited test");

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
              .filter(KeycloakModelTest::checkProviderAvailability)
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

    // Returns true if annotation requirement is not met
    private static boolean checkProviderAvailability(RequireProvider annotation) {
        Set<String> allFactories = getFactory().getProviderFactoriesStream(annotation.value()).map(ProviderFactory::getId).collect(Collectors.toSet());
        List<String> only = Arrays.asList(annotation.only());
        List<String> exclude = Arrays.asList(annotation.exclude());

        // There is no factory for required provider
        if (allFactories.isEmpty()) return true;

        // Remove excluded ids
        allFactories.removeIf(exclude::contains);

        // Remove not matching only
        allFactories.removeIf(id -> !only.isEmpty() && !only.contains(id));

        // If there is no factory return true
        return allFactories.isEmpty();
    }

    @Rule
    public final TestRule guaranteeRequiredFactoryOnMethod = new TestRule() {
        @Override
        public Statement apply(Statement base, Description description) {
            Stream<RequireProvider> st = Optional.ofNullable(description.getAnnotation(RequireProviders.class))
                    .map(RequireProviders::value)
                    .stream()
                    .flatMap(Stream::of);

            RequireProvider rp = description.getAnnotation(RequireProvider.class);
            if (rp != null) {
                st = Stream.concat(st, Stream.of(rp));
            }

            for (Iterator<RequireProvider> iterator = st.iterator(); iterator.hasNext();) {
                RequireProvider rpInner = iterator.next();
                Class<? extends Provider> providerClass = rpInner.value();
                String[] only = rpInner.only();

                if (only.length == 0) {
                    if (getFactory().getProviderFactory(providerClass) == null) {
                        return new Statement() {
                            @Override
                            public void evaluate() {
                                throw new AssumptionViolatedException("Provider must exist: " + providerClass);
                            }
                        };
                    }
                } else {
                    boolean notFoundAny = Stream.of(only).allMatch(provider -> getFactory().getProviderFactory(providerClass, provider) == null);
                    if (notFoundAny) {
                        return new Statement() {
                            @Override
                            public void evaluate() {
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

    @Rule
    public final TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            log.infof("%s STARTED", description.getMethodName());
        }

        @Override
        protected void finished(Description description) {
            log.infof("%s FINISHED\n\n", description.getMethodName());
        }
    };

    private static final Set<Class<? extends Spi>> ALLOWED_SPIS = Set.of(
            AuthorizationSpi.class,
            PolicySpi.class,
            ClientScopeSpi.class,
            ClientSpi.class,
            ComponentFactorySpi.class,
            ClusterSpi.class,
            EventStoreSpi.class,
            ExecutorsSpi.class,
            GroupSpi.class,
            RealmSpi.class,
            RoleSpi.class,
            DeploymentStateSpi.class,
            StoreFactorySpi.class,
            TimerSpi.class,
            TracingSpi.class,
            UserLoginFailureSpi.class,
            UserSessionSpi.class,
            UserSpi.class,
            DatastoreSpi.class,
            CacheRemoteConfigProviderSpi.class);

    private static final Set<Class<? extends ProviderFactory>> ALLOWED_FACTORIES = Set.of(
            ComponentFactoryProviderFactory.class,
            DefaultAuthorizationProviderFactory.class,
            PolicyProviderFactory.class,
            DefaultExecutorsProviderFactory.class,
            DeploymentStateProviderFactory.class,
            DatastoreProviderFactory.class,
            TracingProviderFactory.class,
            CacheRemoteConfigProviderFactory.class);

    protected static final List<KeycloakModelParameters> MODEL_PARAMETERS;
    protected static final Config CONFIG = new Config(KeycloakModelTest::useDefaultFactory);
    private static volatile KeycloakSessionFactory DEFAULT_FACTORY;
    private static final ThreadLocal<KeycloakSessionFactory> LOCAL_FACTORY = new ThreadLocal<>();
    protected static boolean USE_DEFAULT_FACTORY = false;

    static {
        org.keycloak.Config.init(CONFIG);

        KeycloakModelParameters basicParameters = new KeycloakModelParameters(ALLOWED_SPIS, ALLOWED_FACTORIES);
        MODEL_PARAMETERS = Stream.concat(
          Stream.of(basicParameters),
          Stream.of(System.getProperty("keycloak.model.parameters", "").split("\\s*,\\s*"))
            .filter(s -> s != null && ! s.trim().isEmpty())
            .map(cn -> { try { return Class.forName(cn.indexOf('.') >= 0 ? cn : ("org.keycloak.testsuite.model.parameters." + cn)); } catch (Exception e) { throw new RuntimeException("Cannot find class " + cn, e); }})
            .filter(Objects::nonNull)
            .map(c -> { try { return c.getDeclaredConstructor().newInstance(); } catch (Exception e) { throw new RuntimeException("Cannot instantiate class " + c, e); }} )
            .filter(KeycloakModelParameters.class::isInstance)
            .map(KeycloakModelParameters.class::cast)
          )
          .collect(Collectors.toList());


        for (KeycloakModelParameters kmp : KeycloakModelTest.MODEL_PARAMETERS) {
            kmp.beforeSuite(CONFIG);
        }

        // TODO move to a class rule
        reinitializeKeycloakSessionFactory();
        DEFAULT_FACTORY = getFactory();
    }

    /**
     * Creates a fresh initialized {@link KeycloakSessionFactory}. The returned factory uses configuration
     * local to the thread that calls this method, allowing for per-thread customization. This in turn allows
     * testing of several parallel session factories which can be used to simulate several servers
     * running in parallel.
     */
    public static KeycloakSessionFactory createKeycloakSessionFactory() {
        int factoryIndex = FACTORY_COUNT.incrementAndGet();
        String threadName = Thread.currentThread().getName();
        CONFIG.reset();
        CONFIG.spi(ComponentFactorySpi.NAME)
          .provider(DefaultComponentFactoryProviderFactory.PROVIDER_ID)
            .config("cachingForced", "true");
        MODEL_PARAMETERS.forEach(m -> m.updateConfig(CONFIG));

        LOG.debugf("Creating factory %d in %s using the following configuration:\n    %s", factoryIndex, threadName, CONFIG);

        DefaultKeycloakSessionFactory res = new ResteasyKeycloakSessionFactory() {

            @Override
            public void init() {
                Profile.configure(new PropertiesProfileConfigResolver(System.getProperties()));
                super.init();
            }

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

            @Override
            public String toString() {
                return "KeycloakSessionFactory " + factoryIndex + " (from " + threadName + " thread)";
            }
        };
        try {
            res.init();
            res.publish(new PostMigrationEvent(res));
            return res;
        } catch (RuntimeException ex) {
            res.close();
            throw ex;
        }
    }

    /**
     * Closes and initializes new {@link #LOCAL_FACTORY}. This has the same effect as server restart in full-blown server scenario.
     */
    public static synchronized void reinitializeKeycloakSessionFactory() {
        closeKeycloakSessionFactory();
        setFactory(createKeycloakSessionFactory());
    }

    public static synchronized void closeKeycloakSessionFactory() {
        KeycloakSessionFactory f = getFactory();
        setFactory(null);
        if (f != null) {
            LOG.debugf("Closing %s", f);
            f.close();
        }
    }

    /**
     * Runs the given {@code task} in {@code numThreads} parallel threads, each thread operating
     * in the context of a fresh {@link KeycloakSessionFactory} independent of each other thread.
     * <p>
     * Will throw an exception when the thread throws an exception or if the thread doesn't complete in time.
     *
     * @see #inIndependentFactory
     *
     */
    public static void inIndependentFactories(int numThreads, int timeoutSeconds, Runnable task) throws InterruptedException {
        enabledContentionMonitoring();
        // memorize threads created to be able to retrieve their stacktrace later if they don't terminate
        LinkedList<Thread> threads = new LinkedList<>();
        ExecutorService es = Executors.newFixedThreadPool(numThreads, new ThreadFactory() {
            final ThreadFactory tf = Executors.defaultThreadFactory();
            @Override
            public Thread newThread(Runnable r) {
                {
                    Thread thread = tf.newThread(r);
                    threads.add(thread);
                    return thread;
                }
            }
        });
        try {
            CountDownLatch start = new CountDownLatch(numThreads);
            CountDownLatch stop = new CountDownLatch(numThreads);
            Callable<?> independentTask = () -> inIndependentFactory(() -> {
                LOG.infof("Started Keycloak server in thread: %s", Thread.currentThread().getName());
                // use the latch to ensure that all caches are online while the transaction below runs to avoid a RemoteException
                start.countDown();
                start.await();

                try {
                    task.run();

                    // use the latch to ensure that all caches are online while the transaction above runs to avoid a RemoteException
                    // otherwise might fail with "Cannot wire or start components while the registry is not running" during shutdown
                    // https://issues.redhat.com/browse/ISPN-9761
                } finally {
                    stop.countDown();
                }
                stop.await();

                return null;
            });

            // submit tasks, and wait for the results without cancelling execution so that we'll be able to analyze the thread dump
            List<? extends Future<?>> tasks = IntStream.range(0, numThreads)
                    .mapToObj(i -> independentTask)
                    .map(es::submit).collect(Collectors.toList());
            long limit = System.currentTimeMillis() + timeoutSeconds * 1000L;
            for (Future<?> future : tasks) {
                long limitForTask = limit - System.currentTimeMillis();
                if (limitForTask > 0) {
                    try {
                        future.get(limitForTask, TimeUnit.MILLISECONDS);
                    } catch (ExecutionException e) {
                        if (e.getCause() instanceof AssertionError) {
                            throw (AssertionError) e.getCause();
                        } else {
                            LOG.error("Execution didn't complete", e);
                            Assert.fail("Execution didn't complete: " + e.getMessage());
                        }
                    } catch (TimeoutException e) {
                        failWithThreadDump(threads, e);
                    }
                } else {
                    failWithThreadDump(threads, null);
                }
            }
        } finally {
            es.shutdownNow();
        }
        // wait for shutdown executor pool, but not if there has been an exception
        if (!es.awaitTermination(10, TimeUnit.SECONDS)) {
            failWithThreadDump(threads, null);
        }
    }

    private static void enabledContentionMonitoring() {
        if (!ManagementFactory.getThreadMXBean().isThreadContentionMonitoringEnabled()) {
            ManagementFactory.getThreadMXBean().setThreadContentionMonitoringEnabled(true);
        }
    }

    private static void failWithThreadDump(LinkedList<Thread> threads, Exception e) {
        ThreadInfo[] infos = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
        List<String> liveStacks = Arrays.stream(infos).map(thread -> {
            StringBuilder sb = new StringBuilder();
            if (threads.stream().anyMatch(t -> t.getId() == thread.getThreadId())) {
                sb.append("[OurThreadPool] ");
            }
            sb.append(thread.getThreadName()).append(" (").append(thread.getThreadState()).append("):");
            LockInfo lockInfo = thread.getLockInfo();
            if (lockInfo != null) {
                sb.append(" locked on ").append(lockInfo);
                if (thread.getWaitedTime() != -1) {
                  sb.append(" waiting for ").append(thread.getWaitedTime()).append(" ms");
                }
                if (thread.getBlockedTime() != -1) {
                    sb.append(" blocked for ").append(thread.getBlockedTime()).append(" ms");
                }
            }
            sb.append("\n");
            for (StackTraceElement traceElement : thread.getStackTrace()) {
                sb.append("\tat ").append(traceElement).append("\n");
            }
            return sb.toString();
        }).collect(Collectors.toList());
        throw new AssertionError("threads didn't terminate in time: " + liveStacks, e);
    }

    /**
     * Runs the given {@code task} in a context of a fresh {@link KeycloakSessionFactory} which is created before
     * running the task and destroyed afterwards.
     */
    public static <T> T inIndependentFactory(Callable<T> task) {
        if (USE_DEFAULT_FACTORY) {
            throw new IllegalStateException("USE_DEFAULT_FACTORY must be false to use an independent factory");
        }
        KeycloakSessionFactory original = getFactory();
        try {
            setFactory(createKeycloakSessionFactory());
            return task.call();
        } catch (Exception ex) {
            LOG.errorf(ex, "Exception caught while starting Keycloak server in thread %s", Thread.currentThread().getName());
            throw new RuntimeException(ex);
        } finally {
            closeKeycloakSessionFactory();
            setFactory(original);
        }
    }

    protected static boolean useDefaultFactory() {
        return USE_DEFAULT_FACTORY || MAIN_THREAD_NAMES.contains(Thread.currentThread().getName());
    }

    protected static KeycloakSessionFactory getFactory() {
        return useDefaultFactory() ? DEFAULT_FACTORY : LOCAL_FACTORY.get();
    }

    private static void setFactory(KeycloakSessionFactory factory) {
        if (useDefaultFactory()) {
            DEFAULT_FACTORY = factory;
        } else {
            LOCAL_FACTORY.set(factory);
        }
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
    public final void createEnvironment() {
        setTimeOffset(0);
        USE_DEFAULT_FACTORY = isUseSameKeycloakSessionFactoryForAllThreads();
        KeycloakModelUtils.runJobInTransaction(getFactory(), this::createEnvironment);
    }

    @After
    public final void cleanEnvironment() {
        if (getFactory() == null) {
            reinitializeKeycloakSessionFactory();
        }
        setTimeOffset(0);
        KeycloakModelUtils.runJobInTransaction(getFactory(), this::cleanEnvironment);
    }

    protected static <T> Stream<T> getParameters(Class<T> clazz) {
        return MODEL_PARAMETERS.stream().flatMap(mp -> mp.getParameters(clazz)).filter(Objects::nonNull);
    }

    protected <T> void inRolledBackTransaction(T parameter, BiConsumer<KeycloakSession, T> what) {
        try (KeycloakSession session = getFactory().create()) {
            session.getTransactionManager().begin();

            what.accept(session, parameter);

            session.getTransactionManager().setRollbackOnly();
        }
    }

    protected <T, R> R inComittedTransaction(T parameter, BiFunction<KeycloakSession, T, R> what) {
        return inComittedTransaction(parameter, what, null, null);
    }

    protected void inComittedTransaction(Consumer<KeycloakSession> what) {
        inComittedTransaction(a -> { what.accept(a); return null; });
    }

    protected <R> R inComittedTransaction(Function<KeycloakSession, R> what) {
        return inComittedTransaction(1, (a,b) -> what.apply(a), null, null);
    }

    protected <T, R> R inComittedTransaction(T parameter, BiFunction<KeycloakSession, T, R> what, BiConsumer<KeycloakSession, T> onCommit, BiConsumer<KeycloakSession, T> onRollback) {
        return KeycloakModelUtils.runJobInTransactionWithResult(getFactory(), session -> {
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
            return what.apply(session, parameter);
        });
    }

    /**
     * Convenience method for {@link #inComittedTransaction(java.util.function.Consumer)} that
     * obtains realm model from the session and puts it into session context before
     * running the {@code what} task.
     */
    protected <R> R withRealm(String realmId, BiFunction<KeycloakSession, RealmModel, R> what) {
        return inComittedTransaction(session -> {
            final RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            return what.apply(session, realm);
        });
    }

   protected void withRealmConsumer(String realmId, BiConsumer<KeycloakSession, RealmModel> what) {
       withRealm(realmId, (session, realm) -> {
          what.accept(session, realm);
          return null;
       });
   }

    protected boolean isUseSameKeycloakSessionFactoryForAllThreads() {
        return false;
    }

    protected void sleep(long timeMs) {
        try {
            Thread.sleep(timeMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
    }

    protected static RealmModel createRealm(KeycloakSession s, String name) {
        RealmModel realm = s.realms().getRealmByName(name);
        if (realm != null) {
            RealmModel current = s.getContext().getRealm();
            s.getContext().setRealm(realm);
            // The previous test didn't clean up the realm for some reason, cleanup now
            s.realms().removeRealm(realm.getId());
            s.getContext().setRealm(current);
        }
        realm = s.realms().createRealm(name);
        return realm;
    }

    /**
     * Moves time on the Keycloak server
     * @param seconds time offset in seconds by which Keycloak server time is moved
     */
    protected void setTimeOffset(int seconds) {
        inComittedTransaction(session -> {
            Time.setOffset(seconds);
        });
    }

    public static void eventually(BooleanSupplier condition) {
        eventually(null, condition, 5000, 10, MILLISECONDS);
    }

    public static void eventually(Supplier<String> message, BooleanSupplier condition) {
        eventually(message, condition, 5000, 10, MILLISECONDS);
    }

    public static void eventually(Supplier<String> message, BooleanSupplier condition, long timeout,
                                  long pollInterval, TimeUnit unit) {
        if (pollInterval <= 0) {
            throw new IllegalArgumentException("Check interval must be positive");
        }
        if (message == null) {
            message = () -> null;
        }
        try {
            long expectedEndTime = System.nanoTime() + TimeUnit.NANOSECONDS.convert(timeout, unit);
            long sleepMillis = MILLISECONDS.convert(pollInterval, unit);
            do {
                if (condition.getAsBoolean()) return;

                Thread.sleep(sleepMillis);
            } while (expectedEndTime - System.nanoTime() > 0);

        } catch (Exception e) {
            throw new RuntimeException("Unexpected!", e);
        }
        // last check
        Assert.assertTrue(message.get(), condition.getAsBoolean());
    }
}
