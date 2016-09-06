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
package org.keycloak.example.photoz.util;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Interceptor
@Transaction
public class TransactionInterceptor {

    @Inject
    private Instance<EntityManager> entityManager;

    @AroundInvoke
    public Object aroundInvoke(InvocationContext context) {
        EntityManager entityManager = this.entityManager.get();
        EntityTransaction transaction = entityManager.getTransaction();

        try {
            transaction.begin();
            Object proceed = context.proceed();
            transaction.commit();
            return proceed;
        } catch (Exception cause) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException(cause);
        } finally {
            entityManager.close();
        }
    }
}
