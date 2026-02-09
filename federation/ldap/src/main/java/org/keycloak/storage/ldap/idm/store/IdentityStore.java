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

package org.keycloak.storage.ldap.idm.store;

import java.util.List;
import java.util.Set;
import javax.naming.AuthenticationException;
import javax.naming.ldap.LdapName;

import org.keycloak.models.ModelException;
import org.keycloak.representations.idm.LDAPCapabilityRepresentation;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.mappers.LDAPOperationDecorator;

/**
 * IdentityStore representation providing minimal SPI
 *
 * TODO: Rather remove this abstraction
 *
 * @author Boleslaw Dawidowicz
 * @author Shane Bryzak
 */
public interface IdentityStore {

    /**
     * Returns the configuration for this IdentityStore instance
     *
     * @return
     */
    LDAPConfig getConfig();

    // General

    /**
     * Persists the specified IdentityType
     *
     * @param ldapObject
     */
    void add(LDAPObject ldapObject);

    /**
     * Updates the specified IdentityType
     *
     * @param ldapObject
     */
    void update(LDAPObject ldapObject);

    /**
     * Removes the specified IdentityType
     *
     * @param ldapObject
     */
    void remove(LDAPObject ldapObject);

    /**
     * Adds a member to a group.
     * @param groupDn The DN of the group object
     * @param memberAttrName The member attribute name
     * @param value The value (it can be uid or dn depending the group type)
     */
    public void addMemberToGroup(LdapName groupDn, String memberAttrName, String value);

    /**
     * Removes a member from a group.
     * @param groupDn The DN of the group object
     * @param memberAttrName The member attribute name
     * @param value The value (it can be uid or dn depending the group type)
     */
    public void removeMemberFromGroup(LdapName groupDn, String memberAttrName, String value);

    // Identity query

    List<LDAPObject> fetchQueryResults(LDAPQuery LDAPQuery);

    int countQueryResults(LDAPQuery LDAPQuery);

//    // Relationship query
//
//    <V extends Relationship> List<V> fetchQueryResults(RelationshipQuery<V> query);
//
//    <V extends Relationship> int countQueryResults(RelationshipQuery<V> query);

    /**
     * Query the LDAP server <a href="https://ldapwiki.com/wiki/RootDSE">RootDSE</a> and extract the {@link LDAPCapabilityRepresentation}
     * of all supported <i>extensions</i>, <i>controls</i> and <i>features</i> the server announces. The LDAP Wiki
     * provides a <a href="https://ldapwiki.com/wiki/LDAP%20Extensions%20and%20Controls%20Listing">list of known capabilities</a>.
     *
     * Will throw a {@link ModelException} on any LDAP error, or when the searchResult is empty.
     *
     * @return a set of LDAPOid, each representing a server capability (control, extension or feature).
     */
    Set<LDAPCapabilityRepresentation> queryServerCapabilities();

    // Credentials

    /**
     * Validates the specified credentials.
     *
     * @param user Keycloak user
     * @param password Ldap password
     * @throws AuthenticationException if authentication is not successful
     */
    void validatePassword(LDAPObject user, String password) throws AuthenticationException;

    /**
     * Updates the specified credential value.
     *
     * @param user Keycloak user
     * @param password Ldap password
     * @param passwordUpdateDecorator Callback to be executed before/after password update. Can be null
     */
    void updatePassword(LDAPObject user, String password, LDAPOperationDecorator passwordUpdateDecorator);

}
