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

package org.keycloak.testsuite.domainextension.spi.impl;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.testsuite.domainextension.CompanyRepresentation;
import org.keycloak.testsuite.domainextension.jpa.Company;
import org.keycloak.testsuite.domainextension.spi.ExampleService;

import javax.persistence.EntityManager;
import java.util.LinkedList;
import java.util.List;

public class ExampleServiceImpl implements ExampleService {

    private final KeycloakSession session;

    public ExampleServiceImpl(KeycloakSession session) {
        this.session = session;
        if (getRealm() == null) {
            throw new IllegalStateException("The service cannot accept a session without a realm in it's context.");
        }
    }

    private EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    protected RealmModel getRealm() {
        return session.getContext().getRealm();
    }
    
    @Override
    public List<CompanyRepresentation> listCompanies() {
    	List<Company> companyEntities = getEntityManager().createNamedQuery("findByRealm", Company.class)
                .setParameter("realmId", getRealm().getId())
                .getResultList();

        List<CompanyRepresentation> result = new LinkedList<>();
        for (Company entity : companyEntities) {
            result.add(new CompanyRepresentation(entity));
        }
        return result;
    }
    
    @Override
    public CompanyRepresentation findCompany(String id) {
    	Company entity = getEntityManager().find(Company.class, id);
        return entity==null ? null : new CompanyRepresentation(entity);
    }
    
    @Override
    public CompanyRepresentation addCompany(CompanyRepresentation company) {
        Company entity = new Company();
        String id = company.getId()==null ?  KeycloakModelUtils.generateId() : company.getId();
        entity.setId(id);
        entity.setName(company.getName());
        entity.setRealmId(getRealm().getId());
        getEntityManager().persist(entity);

        company.setId(id);
        return company;
    }

    @Override
    public void deleteAllCompanies() {
        EntityManager em = getEntityManager();
        List<Company> companyEntities = em.createNamedQuery("findByRealm", Company.class)
                .setParameter("realmId", getRealm().getId())
                .getResultList();

        for (Company entity : companyEntities) {
            em.remove(entity);
        }
    }

    public void close() {
        // Nothing to do.
    }

}
