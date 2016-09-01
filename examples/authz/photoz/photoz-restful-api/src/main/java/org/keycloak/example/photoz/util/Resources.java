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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@ApplicationScoped
public class Resources {

    private EntityManagerFactory entityManagerFactory;

    @PostConstruct
    public void init() {
        entityManagerFactory = Persistence.createEntityManagerFactory("primary");
    }

    @PreDestroy
    public void dispose() {
        entityManagerFactory.close();
    }

    @RequestScoped
    @Produces
    public EntityManager createEntityManager() {
        return entityManagerFactory.createEntityManager();
    }
}
