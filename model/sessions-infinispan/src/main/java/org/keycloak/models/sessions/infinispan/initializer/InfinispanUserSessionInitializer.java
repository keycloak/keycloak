package org.keycloak.models.sessions.infinispan.initializer;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.distexec.DefaultExecutorService;
import org.infinispan.distexec.DistributedExecutorService;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Transport;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * Startup initialization for reading persistent userSessions/clientSessions to be filled into infinispan/memory . In cluster,
 * the initialization is distributed among all cluster nodes, so the startup time is even faster
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanUserSessionInitializer {

    private static final Logger log = Logger.getLogger(InfinispanUserSessionInitializer.class);

    private static final String STATE_KEY_PREFIX = "initializerState";

    private final KeycloakSessionFactory sessionFactory;
    private final Cache<String, SessionEntity> cache;
    private final SessionLoader sessionLoader;
    private final int maxErrors;
    private final int sessionsPerSegment;
    private final String stateKey;

    private volatile CountDownLatch latch = new CountDownLatch(1);


    public InfinispanUserSessionInitializer(KeycloakSessionFactory sessionFactory, Cache<String, SessionEntity> cache, SessionLoader sessionLoader, int maxErrors, int sessionsPerSegment, String stateKeySuffix) {
        this.sessionFactory = sessionFactory;
        this.cache = cache;
        this.sessionLoader = sessionLoader;
        this.maxErrors = maxErrors;
        this.sessionsPerSegment = sessionsPerSegment;
        this.stateKey = STATE_KEY_PREFIX + "::" + stateKeySuffix;
    }

    public void initCache() {
        this.cache.getAdvancedCache().getComponentRegistry().registerComponent(sessionFactory, KeycloakSessionFactory.class);
        cache.getCacheManager().addListener(new ViewChangeListener());
    }


    public void loadPersistentSessions() {
        if (isFinished()) {
            return;
        }

        while (!isFinished()) {
            if (!isCoordinator()) {
                try {
                    latch.await(500, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ie) {
                    log.error("Interrupted", ie);
                }
            } else {
                startLoading();
            }
        }
    }


    private boolean isFinished() {
        InitializerState state = (InitializerState) cache.get(stateKey);
        return state != null && state.isFinished();
    }


    private InitializerState getOrCreateInitializerState() {
        InitializerState state = (InitializerState) cache.get(stateKey);
        if (state == null) {
            final int[] count = new int[1];

            // Rather use separate transactions for update and counting

            KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {
                @Override
                public void run(KeycloakSession session) {
                    sessionLoader.init(session);
                }

            });

            KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {
                @Override
                public void run(KeycloakSession session) {
                    count[0] = sessionLoader.getSessionsCount(session);
                }

            });

            state = new InitializerState();
            state.init(count[0], sessionsPerSegment);
            saveStateToCache(state);
        }
        return state;

    }


    private void saveStateToCache(final InitializerState state) {

        // 3 attempts to send the message (it may fail if some node fails in the meantime)
        retry(3, new Runnable() {

            @Override
            public void run() {

                // Save this synchronously to ensure all nodes read correct state
                InfinispanUserSessionInitializer.this.cache.getAdvancedCache().
                        withFlags(Flag.IGNORE_RETURN_VALUES, Flag.FORCE_SYNCHRONOUS)
                        .put(stateKey, state);
            }

        });
    }


    private boolean isCoordinator() {
        Transport transport = cache.getCacheManager().getTransport();
        return transport == null || transport.isCoordinator();
    }


    // Just coordinator will run this
    private void startLoading() {
        InitializerState state = getOrCreateInitializerState();

        // Assume each worker has same processor's count
        int processors = Runtime.getRuntime().availableProcessors();

        ExecutorService localExecutor = Executors.newCachedThreadPool();
        DistributedExecutorService distributedExecutorService = new DefaultExecutorService(cache, localExecutor);

        int errors = 0;

        try {
            while (!state.isFinished()) {
                Transport transport = cache.getCacheManager().getTransport();
                int nodesCount = transport==null ? 1 : transport.getMembers().size();
                int distributedWorkersCount = processors * nodesCount;

                log.debugf("Starting next iteration with %d workers", distributedWorkersCount);

                List<Integer> segments = state.getUnfinishedSegments(distributedWorkersCount);

                if (log.isTraceEnabled()) {
                    log.trace("unfinished segments for this iteration: " + segments);
                }

                List<Future<WorkerResult>> futures = new LinkedList<>();
                for (Integer segment : segments) {
                    SessionInitializerWorker worker = new SessionInitializerWorker();
                    worker.setWorkerEnvironment(segment, sessionsPerSegment, sessionLoader);

                    Future<WorkerResult> future = distributedExecutorService.submit(worker);
                    futures.add(future);
                }

                for (Future<WorkerResult> future : futures) {
                    try {
                        WorkerResult result = future.get();

                        if (result.getSuccess()) {
                            int computedSegment = result.getSegment();
                            state.markSegmentFinished(computedSegment);
                        } else {
                            if (log.isTraceEnabled()) {
                                log.tracef("Segment %d failed to compute", result.getSegment());
                            }
                        }
                    } catch (InterruptedException ie) {
                        errors++;
                        log.error("Interruped exception when computed future. Errors: " + errors, ie);
                    } catch (ExecutionException ee) {
                        errors++;
                        log.error("ExecutionException when computed future. Errors: " + errors, ee);
                    }
                }

                if (errors >= maxErrors) {
                    throw new RuntimeException("Maximum count of worker errors occured. Limit was " + maxErrors + ". See server.log for details");
                }

                saveStateToCache(state);

                if (log.isDebugEnabled()) {
                    log.debug("New initializer state pushed. The state is: " + state.printState());
                }
            }
        } finally {
            distributedExecutorService.shutdown();
            localExecutor.shutdown();
        }
    }

    private void retry(int retry, Runnable runnable) {
        while (true) {
            try {
                runnable.run();
                return;
            } catch (RuntimeException e) {
                retry--;
                if (retry == 0) {
                    throw e;
                }
            }
        }
    }


    @Listener
    public class ViewChangeListener {

        @ViewChanged
        public void viewChanged(ViewChangedEvent event) {
            boolean isCoordinator = isCoordinator();
            log.debug("View Changed: is coordinator: " + isCoordinator);

            if (isCoordinator) {
                latch.countDown();
                latch = new CountDownLatch(1);
            }
        }

    }


    public static class WorkerResult implements Serializable {

        private Integer segment;
        private Boolean success;

        public static WorkerResult create (Integer segment, boolean success) {
            WorkerResult res = new WorkerResult();
            res.setSegment(segment);
            res.setSuccess(success);
            return res;
        }

        public Integer getSegment() {
            return segment;
        }

        public void setSegment(Integer segment) {
            this.segment = segment;
        }

        public Boolean getSuccess() {
            return success;
        }

        public void setSuccess(Boolean success) {
            this.success = success;
        }
    }
}
