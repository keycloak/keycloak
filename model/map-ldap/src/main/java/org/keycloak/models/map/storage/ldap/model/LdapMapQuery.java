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

package org.keycloak.models.map.storage.ldap.model;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.map.storage.ldap.LdapModelCriteriaBuilder;

import javax.naming.directory.SearchControls;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;

/**
 * Default IdentityQuery implementation.
 *
 * LDAPQuery should be closed after use in case that pagination was used (initPagination was called)
 * Closing LDAPQuery is very important in case ldapContextManager contains VaultSecret
 *
 * @author Shane Bryzak
 */
public class LdapMapQuery implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(LdapMapQuery.class);

    private int offset;
    private int limit;
    private String searchDn;
    private LdapModelCriteriaBuilder<?, ?, ?> modelCriteriaBuilder;

    private final Set<String> returningLdapAttributes = new LinkedHashSet<>();

    // Contains just those returningLdapAttributes, which are read-only. They will be marked as read-only in returned LDAPObject instances as well
    // NOTE: names of attributes are lower-cased to avoid case sensitivity issues (LDAP searching is usually case-insensitive, so we want to be as well)
    private final Set<String> returningReadOnlyLdapAttributes = new LinkedHashSet<>();
    private final Set<String> objectClasses = new LinkedHashSet<>();

    private final List<ComponentModel> mappers = new ArrayList<>();

    private int searchScope = SearchControls.SUBTREE_SCOPE;

    public void setSearchDn(String searchDn) {
        this.searchDn = searchDn;
    }

    public void addObjectClasses(Collection<String> objectClasses) {
        this.objectClasses.addAll(objectClasses);
    }

    public void addReturningLdapAttribute(String ldapAttributeName) {
        this.returningLdapAttributes.add(ldapAttributeName);
    }

    public void addReturningReadOnlyLdapAttribute(String ldapAttributeName) {
        this.returningReadOnlyLdapAttributes.add(ldapAttributeName.toLowerCase());
    }

    public LdapMapQuery addMappers(Collection<ComponentModel> mappers) {
        this.mappers.addAll(mappers);
        return this;
    }

    public void setSearchScope(int searchScope) {
        this.searchScope = searchScope;
    }

    public String getSearchDn() {
        return this.searchDn;
    }

    public Set<String> getObjectClasses() {
        return unmodifiableSet(this.objectClasses);
    }

    public Set<String> getReturningLdapAttributes() {
        return unmodifiableSet(this.returningLdapAttributes);
    }

    public Set<String> getReturningReadOnlyLdapAttributes() {
        return unmodifiableSet(this.returningReadOnlyLdapAttributes);
    }

    public List<ComponentModel> getMappers() {
        return mappers;
    }

    public int getSearchScope() {
        return searchScope;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public LdapMapQuery setOffset(int offset) {
        this.offset = offset;
        return this;
    }

    public LdapMapQuery setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public void close() {
    }

    public void setModelCriteriaBuilder(LdapModelCriteriaBuilder<?, ?, ?> ldapModelCriteriaBuilder) {
        this.modelCriteriaBuilder = ldapModelCriteriaBuilder;
    }

    public LdapModelCriteriaBuilder<?, ?, ?> getModelCriteriaBuilder() {
        return modelCriteriaBuilder;
    }
}
