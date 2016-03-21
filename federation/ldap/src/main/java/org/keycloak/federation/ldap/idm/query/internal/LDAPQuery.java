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

package org.keycloak.federation.ldap.idm.query.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.naming.directory.SearchControls;

import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.Sort;
import org.keycloak.federation.ldap.mappers.LDAPFederationMapper;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.UserFederationMapperModel;

import static java.util.Collections.unmodifiableSet;

/**
 * Default IdentityQuery implementation.
 *
 *
 * @author Shane Bryzak
 */
public class LDAPQuery {

    private final LDAPFederationProvider ldapFedProvider;

    private int offset;
    private int limit;
    private byte[] paginationContext;
    private String searchDn;
    private final Set<Condition> conditions = new LinkedHashSet<Condition>();
    private final Set<Sort> ordering = new LinkedHashSet<Sort>();

    private final Set<String> returningLdapAttributes = new LinkedHashSet<String>();

    // Contains just those returningLdapAttributes, which are read-only. They will be marked as read-only in returned LDAPObject instances as well
    // NOTE: names of attributes are lower-cased to avoid case sensitivity issues (LDAP searching is usually case-insensitive, so we want to be as well)
    private final Set<String> returningReadOnlyLdapAttributes = new LinkedHashSet<String>();
    private final Set<String> objectClasses = new LinkedHashSet<String>();

    private final List<UserFederationMapperModel> mappers = new ArrayList<UserFederationMapperModel>();

    private int searchScope = SearchControls.SUBTREE_SCOPE;

    public LDAPQuery(LDAPFederationProvider ldapProvider) {
        this.ldapFedProvider = ldapProvider;
    }

    public LDAPQuery addWhereCondition(Condition... condition) {
        this.conditions.addAll(Arrays.asList(condition));
        return this;
    }

    public LDAPQuery sortBy(Sort... sorts) {
        this.ordering.addAll(Arrays.asList(sorts));
        return this;
    }

    public LDAPQuery setSearchDn(String searchDn) {
        this.searchDn = searchDn;
        return this;
    }

    public LDAPQuery addObjectClasses(Collection<String> objectClasses) {
        this.objectClasses.addAll(objectClasses);
        return this;
    }

    public LDAPQuery addReturningLdapAttribute(String ldapAttributeName) {
        this.returningLdapAttributes.add(ldapAttributeName);
        return this;
    }

    public LDAPQuery addReturningReadOnlyLdapAttribute(String ldapAttributeName) {
        this.returningReadOnlyLdapAttributes.add(ldapAttributeName.toLowerCase());
        return this;
    }

    public LDAPQuery addMappers(Collection<UserFederationMapperModel> mappers) {
        this.mappers.addAll(mappers);
        return this;
    }

    public LDAPQuery setSearchScope(int searchScope) {
        this.searchScope = searchScope;
        return this;
    }

    public Set<Sort> getSorting() {
        return unmodifiableSet(this.ordering);
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

    public List<UserFederationMapperModel> getMappers() {
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

    public byte[] getPaginationContext() {
        return paginationContext;
    }


    public List<LDAPObject> getResultList() {

        // Apply mappers now
        List<UserFederationMapperModel> sortedMappers = ldapFedProvider.sortMappersAsc(mappers);
        for (UserFederationMapperModel mapperModel : sortedMappers) {
            LDAPFederationMapper fedMapper = ldapFedProvider.getMapper(mapperModel);
            fedMapper.beforeLDAPQuery(mapperModel, this);
        }

        List<LDAPObject> result = new ArrayList<LDAPObject>();

        try {
            for (LDAPObject ldapObject : ldapFedProvider.getLdapIdentityStore().fetchQueryResults(this)) {
                result.add(ldapObject);
            }
        } catch (Exception e) {
            throw new ModelException("LDAP Query failed", e);
        }

        return result;
    }

    public LDAPObject getFirstResult() {
        List<LDAPObject> results = getResultList();

        if (results.isEmpty()) {
            return null;
        } else if (results.size() == 1) {
            return results.get(0);
        } else {
            throw new ModelDuplicateException("Error - multiple LDAP objects found but expected just one");
        }
    }

    public int getResultCount() {
        return ldapFedProvider.getLdapIdentityStore().countQueryResults(this);
    }

    public LDAPQuery setOffset(int offset) {
        this.offset = offset;
        return this;
    }

    public LDAPQuery setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public LDAPQuery setPaginationContext(byte[] paginationContext) {
        this.paginationContext = paginationContext;
        return this;
    }

    public Set<Condition> getConditions() {
        return this.conditions;
    }

    public LDAPFederationProvider getLdapProvider() {
        return ldapFedProvider;
    }

}
