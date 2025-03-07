/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.connections.jpa;

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

import org.hibernate.exception.ConstraintViolationException;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelIllegalStateException;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Query;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PersistenceExceptionConverter implements InvocationHandler {

    private static final Pattern WRITE_METHOD_NAMES = Pattern.compile("persist|merge");

    private final EntityManager em;
    private final boolean batchEnabled;
    private final int batchSize;
    private int changeCount = 0;

    private static ThreadLocal<Set<EntityManager>> batchMode = new ThreadLocal<Set<EntityManager>>();

    public static EntityManager create(KeycloakSession session, EntityManager em) {
        return (EntityManager) Proxy.newProxyInstance(EntityManager.class.getClassLoader(), new Class[]{EntityManager.class}, new PersistenceExceptionConverter(session, em));
    }

    private PersistenceExceptionConverter(KeycloakSession session, EntityManager em) {
        batchEnabled = session.getAttributeOrDefault(Constants.STORAGE_BATCH_ENABLED, false);
        batchSize = session.getAttributeOrDefault(Constants.STORAGE_BATCH_SIZE, 100);
        this.em = em;
    }

    static void runInBatch(Runnable runnable) {
        var batchedManagers = batchMode.get();
        if (batchedManagers == null) {
            batchedManagers = new HashSet<EntityManager>();
            batchMode.set(batchedManagers);
        } else {
            // recursive call, could have several handlings
            throw new IllegalStateException("Already Batching");
        }
        try {
            runnable.run();
        } finally {
            batchMode.remove();
            batchedManagers.stream().filter(EntityManager::isOpen).forEach(em -> {
                try {
                    em.flush();
                } catch (Exception e) {
                    throw convert(e);
                }
                em.clear();
            });
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        var batchedManagers = batchMode.get();
        boolean batched = batchedManagers != null;
        if (batched) {
            batchedManagers.add(em);
            // detect operations that should be optimized
            // TODO: could log something
            //switch (method.getName()) {
            //case "detach", "flush" -> throw new AssertionError();
            //}
            switch (method.getName()) {
            case "clear" -> throw new IllegalStateException("Cannot clear in batched mode");
            case "close", "detach" -> em.flush();
            }
        }
        try {
            flushInBatchIfEnabled(method);
            Object result = method.invoke(em, args);
            if (batched && result instanceof Query query) {
                // TODO: this should probably be done in the logic creating the queries
                // userRoleMappingIds at least is running for user import
                // and do not need to detect anything that isn't flushed
                query.setFlushMode(FlushModeType.COMMIT);
            }
            return result;
        } catch (InvocationTargetException e) {
            throw convert(e.getCause());
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
        final Predicate<Throwable> checkDuplicationMessage = throwable -> {
            final String message = throwable.getCause() != null ? throwable.getCause().getMessage() : throwable.getMessage();
            return message == null ? false : message.toLowerCase().contains("duplicate");
        };

        Predicate<Throwable> throwModelDuplicateEx = throwable ->
                throwable instanceof EntityExistsException
                || throwable instanceof ConstraintViolationException
                // SQL state class 23 captures errors like 23505 = UNIQUE VIOLATION et al.
                // This captures, for example, a BatchUpdateException which is not mapped to the other exception types
                // https://en.wikipedia.org/wiki/SQLSTATE
                || (throwable instanceof SQLException bue && bue.getSQLState().startsWith("23"))
                || throwable instanceof SQLIntegrityConstraintViolationException;

        throwModelDuplicateEx = throwModelDuplicateEx.or(checkDuplicationMessage);

        if (t.getCause() != null && throwModelDuplicateEx.test(t.getCause())) {
            throw new ModelDuplicateException("Duplicate resource error", t.getCause());
        } else if (throwModelDuplicateEx.test(t)) {
            throw new ModelDuplicateException("Duplicate resource error", t);
        } else if (t instanceof OptimisticLockException) {
            throw new ModelIllegalStateException("Database operation failed", t);
        } else {
            throw new ModelException("Database operation failed", t);
        }
    }

    public static boolean isBatchMode() {
        return batchMode.get() != null;
    }

}
