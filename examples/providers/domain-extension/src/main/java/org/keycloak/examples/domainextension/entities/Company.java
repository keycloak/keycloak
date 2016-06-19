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

import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "EXAMPLE_COMPANY")
@NamedQueries({ @NamedQuery(name = "findAllCompanies", query = "from Company"),
    @NamedQuery(name = "findByRealm", query = "from Company where realmId = :realmId") })
public class Company {

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "REALM_ID", nullable = false)
    private String realmId;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "company", orphanRemoval = true)
    private final Set<Region> regions = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "company", orphanRemoval = true)
    private final Set<UserAccount> userAccounts = new HashSet<>();

    @SuppressWarnings("unused")
    private Company() {

    }

    public Company(String realmId, String name) {
    	this.id = UUID.randomUUID().toString();
        this.realmId = realmId;
        this.name = name;
    }

    public String getId() {
		return id;
	}
    
    public String getRealmId() {
        return realmId;
    }
    
    public String getName() {
		return name;
	}

    public Set<UserAccount> getUserAccounts() {
        return Collections.unmodifiableSet(userAccounts);
    }

    public void addUserAccount(UserAccount userAccount) {
        userAccounts.add(userAccount);
    }

    public boolean removeUserAccount(UserAccount userAccount) {
        return userAccounts.remove(userAccount);
    }

    public UserAccount getUserAccountByUsername(String username) {
        for (UserAccount userAccount : userAccounts) {
            if (userAccount.getUser().getUsername().equals(username)) {
                return userAccount;
            }
        }

        throw new NoSuchElementException("No user found with name '" + username + "'");
    }

    public void addRegion(Region region) {
    	regions.add(region);
    }
    
    public boolean removeRegion(Region region) {
    	return regions.remove(region);
    }
    
}
