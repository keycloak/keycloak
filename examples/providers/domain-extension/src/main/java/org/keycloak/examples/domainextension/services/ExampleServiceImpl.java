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

package org.keycloak.examples.domainextension.services;

import java.util.List;

import javax.persistence.EntityManager;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.examples.domainextension.entities.Company;
import org.keycloak.examples.domainextension.services.repository.CompanyRepository;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public class ExampleServiceImpl implements ExampleService {

    private final KeycloakSession session;
    private CompanyRepository companyRepository;

    public ExampleServiceImpl(KeycloakSession session) {
        this.session = session;
        if (getRealm() == null) {
            throw new IllegalStateException("The service cannot accept a session without a realm in it's context.");
        }

        companyRepository = new CompanyRepository(getEntityManager());
    }

    private EntityManager getEntityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    protected RealmModel getRealm() {
        return session.getContext().getRealm();
    }
    
    @Override
    public List<Company> listCompanies() {
    	return companyRepository.getAll();
    }
    
    @Override
    public Company findCompany(String id) {
    	return companyRepository.findById(id);
    }
    
    @Override
    public void addCompany(Company company) {
    	companyRepository.persist(company);
    }

    public void close() {
        // Nothing to do.
    }

}
