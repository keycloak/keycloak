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

package org.keycloak.examples.domainextension.entities;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.keycloak.models.jpa.entities.UserEntity;

/**
 * The Class UserAccount.
 */
@Entity
@Table(name = "EXAMPLE_USER_ACCOUNT")
public class UserAccount {

    @Id
    @Column(name = "ID")
    private String id;

    @ManyToOne
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "COMPANY_ID", nullable = true)
    private Company company;

    @SuppressWarnings("unused")
    private UserAccount() {

    }

    public UserAccount(String id, UserEntity userEntity, Company company) {
        this.id = UUID.randomUUID().toString();
        user = userEntity;
        this.company = company;
    }

    public String getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public Company getCompany() {
		return company;
	}

}
