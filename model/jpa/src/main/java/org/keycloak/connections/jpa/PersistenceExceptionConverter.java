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

import org.hibernate.exception.ConstraintViolationException;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelIllegalStateException;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class PersistenceExceptionConverter implements InvocationHandler {

    private static final Pattern WRITE_METHOD_NAMES = Pattern.compile("persist|merge");

    private final EntityManager em;
    private final boolean batchEnabled;
    private final int batchSize;
    private int changeCount = 0;

    public static EntityManager create(KeycloakSession session, EntityManager em) {
        return (EntityManager) Proxy.newProxyInstance(EntityManager.class.getClassLoader(), new Class[]{EntityManager.class}, new PersistenceExceptionConverter(session, em));
    }

    private PersistenceExceptionConverter(KeycloakSession session, EntityManager em) {
        batchEnabled = session.getAttributeOrDefault(Constants.STORAGE_BATCH_ENABLED, false);
        batchSize = session.getAttributeOrDefault(Constants.STORAGE_BATCH_SIZE, 100);
        this.em = em;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            flushInBatchIfEnabled(method);
            return method.invoke(em, args);
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
                || isSqlStateClass23(throwable)
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
