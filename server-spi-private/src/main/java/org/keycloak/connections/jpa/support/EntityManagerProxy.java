/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.connections.jpa.support;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Query;

import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelIllegalStateException;

import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EntityManagerProxy {

    public static final String SYNC_COMMIT_REQUIRED = "kc.sync_commit_required";
    static final String ASYNC_COMMIT_ALLOWED = "kc.async_commit_allowed";
    private static final String ASYNC_COMMIT_ENABLED = "kc.async_commit_enabled";

    /**
     * Marks an entity manager factory as async-commit-capable. Called once at startup
     * by {@code AsyncCommitIntegrator} after successfully registering listeners.
     */
    public static void enableAsyncCommit(EntityManagerFactory emf) {
        emf.getProperties().put(ASYNC_COMMIT_ENABLED, Boolean.TRUE);
    }

    /**
     * Returns whether async commit is enabled for the given entity manager's factory.
     */
    public static boolean isAsyncCommitEnabled(EntityManager em) {
        return Boolean.TRUE.equals(em.getEntityManagerFactory().getProperties().get(ASYNC_COMMIT_ENABLED));
    }

    /**
     * Marks a query as safe for asynchronous commit. Call this on queries that only modify
     * entities implementing {@code AsynchronousCommitAllowed}. When async commit is not enabled
     * on the entity manager's factory, this is a no-op.
     */
    public static Query allowAsyncCommit(EntityManager em, Query query) {
        if (isAsyncCommitEnabled(em)) {
            query.setHint(ASYNC_COMMIT_ALLOWED, true);
        }
        return query;
    }

    private static final Pattern WRITE_METHOD_NAMES = Pattern.compile("persist|merge");

    private Set<EntityManagerProxy> entityManagerProxies;
    private EntityManager em;
    private final KeycloakSession session;
    private final boolean batchEnabled;
    private final boolean asyncCommitEnabled;
    private final int batchSize;
    private int changeCount = 0;

    public static EntityManager create(KeycloakSession session, EntityManager em, boolean sessionManaged) {
        Set<EntityManagerProxy> entityManagerProxies = null;
        if (sessionManaged) {
            // the alternative to this tracking is to have a method on the session for
            // getting the in use providers - not something that will create all providers
            entityManagerProxies = session.getAttribute(EntityManagers.ENTITY_MANAGER_PROXIES, Set.class);
            if (entityManagerProxies == null) {
                entityManagerProxies = new HashSet<>();
                session.setAttribute(EntityManagers.ENTITY_MANAGER_PROXIES, entityManagerProxies);
            }
        }
        boolean batchEnabled = session.getAttributeOrDefault(Constants.STORAGE_BATCH_ENABLED, false);
        int batchSize = session.getAttributeOrDefault(Constants.STORAGE_BATCH_SIZE, 100);
        return create(session, em, entityManagerProxies, batchEnabled, batchSize);
    }

    static EntityManager create(KeycloakSession session, EntityManager em, Set<EntityManagerProxy> entityManagerProxies,
            boolean batchEnabled, int batchSize) {
        EntityManagerProxy converter = new EntityManagerProxy(session, em, entityManagerProxies, batchEnabled, batchSize);
        if (entityManagerProxies != null) {
            entityManagerProxies.add(converter);
        }
        return (EntityManager) Proxy.newProxyInstance(EntityManager.class.getClassLoader(), new Class[]{EntityManager.class}, converter::invoke);
    }

    private EntityManagerProxy(KeycloakSession session, EntityManager em, Set<EntityManagerProxy> entityManagerProxies, boolean batchEnabled, int batchSize) {
        this.session = session;
        this.batchEnabled = batchEnabled;
        this.asyncCommitEnabled = Boolean.TRUE.equals(em.getEntityManagerFactory().getProperties().get(ASYNC_COMMIT_ENABLED));
        this.batchSize = batchSize;
        this.em = em;
        this.entityManagerProxies = entityManagerProxies;
    }

    void setEntityManager(EntityManager manager) {
        this.em = manager;
    }

    EntityManager getEntityManager() {
        return this.em;
    }

    private Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        boolean batched = EntityManagers.isBatchMode();
        boolean readOnly = session != null && session.isReadOnly();
        try {
            flushInBatchIfEnabled(method);
            Object result = method.invoke(em, args);
            if (result instanceof Query query) {
                if (batched || readOnly) {
                    // TODO: it would be safer if there were a way to validate
                    // if this or disabling persist/detach where correct for a given batch
                    // and types were correct
                    query.setFlushMode(FlushModeType.COMMIT);
                }
                if (asyncCommitEnabled) {
                    result = wrapQuery(query);
                }
            }
            if (entityManagerProxies != null && args == null && method.getName().equals("close")) {
                entityManagerProxies.remove(this);
            }
            return result;
        } catch (InvocationTargetException e) {
            throw convert(e);
        }
    }

    /**
     * Wraps a JPA {@link Query} to intercept {@code executeUpdate()} calls. When the update modifies
     * rows (return value &gt; 0), marks the Hibernate session for synchronous commit so that
     * {@code AsyncCommitIntegrator} does not relax durability for that transaction.
     * <p>
     * Queries that only modify entities implementing {@code AsynchronousCommitAllowed} can opt out
     * by setting the {@link #ASYNC_COMMIT_ALLOWED} hint:
     * {@code query.setHint(EntityManagerProxy.ASYNC_COMMIT_ALLOWED, true)}.
     * <p>
     * The proxy is necessary to distinguish read queries ({@code getResultList}, {@code getSingleResult})
     * from write queries ({@code executeUpdate}). Without it, the only alternative would be to force
     * synchronous commit on every {@code createQuery} call, which would disable async commit for most
     * transactions since nearly all of them use HQL SELECT queries.
     * <p>
     * Performance: the JDK caches proxy classes per interface set, so each call only allocates one
     * small object — negligible compared to the SQL round-trip the query will perform.
     */
    private Object wrapQuery(Query delegate) {
        Class<?>[] ifaces = delegate.getClass().getInterfaces();
        if (ifaces.length == 0) {
            ifaces = new Class<?>[]{ Query.class };
        }
        return Proxy.newProxyInstance(delegate.getClass().getClassLoader(), ifaces,
                new QueryHandler(delegate, this.em));
    }

    private static class QueryHandler implements InvocationHandler {
        private final Query delegate;
        private final EntityManager em;
        private boolean asyncAllowed;

        QueryHandler(Query delegate, EntityManager em) {
            this.delegate = delegate;
            this.em = em;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Intercept our custom hint before it reaches Hibernate, which would
            // log "ignoring unrecognized query hint" and discard it.
            if (method.getName().equals("setHint")
                    && args != null && args.length == 2
                    && ASYNC_COMMIT_ALLOWED.equals(args[0])) {
                asyncAllowed = Boolean.TRUE.equals(args[1]);
                return proxy;
            }
            try {
                Object result = method.invoke(delegate, args);
                if (method.getName().equals("executeUpdate")
                        && result instanceof Integer rowCount && rowCount > 0
                        && !asyncAllowed) {
                    em.unwrap(Session.class).setProperty(SYNC_COMMIT_REQUIRED, Boolean.TRUE);
                }
                // Preserve proxy for fluent methods (setParameter, setHint, …) that return
                // the delegate.  Skip for unwrap(), which may return a concrete Hibernate
                // class the JDK proxy cannot implement — substituting would cause ClassCastException.
                if (result == delegate && !method.getName().equals("unwrap")) {
                    return proxy;
                }
                return result;
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }

    private void flushInBatchIfEnabled(Method method) {
        if (batchEnabled) {
            if (WRITE_METHOD_NAMES.matcher(method.getName()).matches()) {
                if (changeCount++ > batchSize) {
                    em.flush();
                    em.clear();
                    changeCount = 0;
                }
            }
        }
    }

    // For JTA, the database operations are executed during the commit phase of a transaction, and DB exceptions can be propagated differently
    public static ModelException convert(Throwable t) {
        Predicate<Throwable> throwModelDuplicateEx = throwable ->
                throwable instanceof EntityExistsException
                        || throwable instanceof ConstraintViolationException
                        || isSqlStateClass23(throwable)
                        || throwable instanceof SQLIntegrityConstraintViolationException;
        while (true) {
            if (t instanceof ModelException me) {
                throw me;
            } else if (throwModelDuplicateEx.test(t)) {
                return new ModelDuplicateException("Duplicate resource error", t);
            } else if (t instanceof OptimisticLockException) {
                return new ModelIllegalStateException("Database operation failed", t);
            } else if (t.getCause() == null) {
                return new ModelException("Database operation failed", t);
            } else {
                t = t.getCause();
            }
        }
    }

    /**
     * SQL state class 23 captures errors like 23505 = UNIQUE VIOLATION et al.
     * This captures, for example, a BatchUpdateException which is not mapped to the other exception types
     * https://en.wikipedia.org/wiki/SQLSTATE
     */
    private static boolean isSqlStateClass23(Throwable t) {
        return t instanceof SQLException bue
            && bue.getSQLState() != null
            && bue.getSQLState().startsWith("23");
    }

}
