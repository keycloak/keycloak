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

package org.keycloak.federation.ldap.idm.store.ldap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

import javax.naming.AuthenticationException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.jboss.logging.Logger;
import org.keycloak.federation.ldap.LDAPConfig;
import org.keycloak.federation.ldap.idm.model.LDAPDn;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.federation.ldap.idm.query.internal.EqualCondition;
import org.keycloak.federation.ldap.idm.store.IdentityStore;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;

/**
 * An IdentityStore implementation backed by an LDAP directory
 *
 * @author Shane Bryzak
 * @author Anil Saldhana
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class LDAPIdentityStore implements IdentityStore {

    private static final Logger logger = Logger.getLogger(LDAPIdentityStore.class);

    private final LDAPConfig config;
    private final LDAPOperationManager operationManager;

    public LDAPIdentityStore(LDAPConfig config) {
        this.config = config;

        try {
            this.operationManager = new LDAPOperationManager(config);
        } catch (NamingException e) {
            throw new ModelException("Couldn't init operation manager", e);
        }
    }

    @Override
    public LDAPConfig getConfig() {
        return this.config;
    }

    @Override
    public void add(LDAPObject ldapObject) {
        // id will be assigned by the ldap server
        if (ldapObject.getUuid() != null) {
            throw new ModelException("Can't add object with already assigned uuid");
        }

        String entryDN = ldapObject.getDn().toString();
        BasicAttributes ldapAttributes = extractAttributes(ldapObject, true);
        this.operationManager.createSubContext(entryDN, ldapAttributes);
        ldapObject.setUuid(getEntryIdentifier(ldapObject));

        if (logger.isDebugEnabled()) {
            logger.debugf("Type with identifier [%s] and dn [%s] successfully added to LDAP store.", ldapObject.getUuid(), entryDN);
        }
    }

    @Override
    public void update(LDAPObject ldapObject) {
        BasicAttributes updatedAttributes = extractAttributes(ldapObject, false);
        NamingEnumeration<Attribute> attributes = updatedAttributes.getAll();

        String entryDn = ldapObject.getDn().toString();
        this.operationManager.modifyAttributes(entryDn, attributes);

        if (logger.isDebugEnabled()) {
            logger.debugf("Type with identifier [%s] and DN [%s] successfully updated to LDAP store.", ldapObject.getUuid(), entryDn);
        }
    }

    @Override
    public void remove(LDAPObject ldapObject) {
        this.operationManager.removeEntry(ldapObject.getDn().toString());

        if (logger.isDebugEnabled()) {
            logger.debugf("Type with identifier [%s] and DN [%s] successfully removed from LDAP store.", ldapObject.getUuid(), ldapObject.getDn().toString());
        }
    }


    @Override
    public List<LDAPObject> fetchQueryResults(LDAPQuery identityQuery) {
        if (identityQuery.getSorting() != null && !identityQuery.getSorting().isEmpty()) {
            throw new ModelException("LDAP Identity Store does not yet support sorted queries.");
        }

        List<LDAPObject> results = new ArrayList<>();

        try {
            String baseDN = identityQuery.getSearchDn();

            for (Condition condition : identityQuery.getConditions()) {

                // Check if we are searching by ID
                String uuidAttrName = getConfig().getUuidLDAPAttributeName();
                if (condition instanceof EqualCondition) {
                    EqualCondition equalCondition = (EqualCondition) condition;
                    if (equalCondition.getParameterName().equalsIgnoreCase(uuidAttrName)) {
                        SearchResult search = this.operationManager
                                .lookupById(baseDN, equalCondition.getValue().toString(), identityQuery.getReturningLdapAttributes());

                        if (search != null) {
                            results.add(populateAttributedType(search, identityQuery));
                        }

                        return results;
                    }
                }
            }


            StringBuilder filter = createIdentityTypeSearchFilter(identityQuery);

            List<SearchResult> search;
            if (getConfig().isPagination() && identityQuery.getLimit() > 0) {
                search = this.operationManager.searchPaginated(baseDN, filter.toString(), identityQuery);
            } else {
                search = this.operationManager.search(baseDN, filter.toString(), identityQuery.getReturningLdapAttributes(), identityQuery.getSearchScope());
            }

            for (SearchResult result : search) {
                if (!result.getNameInNamespace().equalsIgnoreCase(baseDN)) {
                    results.add(populateAttributedType(result, identityQuery));
                }
            }
        } catch (Exception e) {
            throw new ModelException("Querying of LDAP failed " + identityQuery, e);
        }

        return results;
    }

    @Override
    public int countQueryResults(LDAPQuery identityQuery) {
        int limit = identityQuery.getLimit();
        int offset = identityQuery.getOffset();

        identityQuery.setLimit(0);
        identityQuery.setOffset(0);

        int resultCount = identityQuery.getResultList().size();

        identityQuery.setLimit(limit);
        identityQuery.setOffset(offset);

        return resultCount;
    }

    // *************** CREDENTIALS AND USER SPECIFIC STUFF

    @Override
    public void validatePassword(LDAPObject user, String password) throws AuthenticationException {
        String userDN = user.getDn().toString();

        if (logger.isTraceEnabled()) {
            logger.tracef("Using DN [%s] for authentication of user", userDN);
        }

        operationManager.authenticate(userDN, password);
    }

    @Override
    public void updatePassword(LDAPObject user, String password) {
        String userDN = user.getDn().toString();

        if (logger.isDebugEnabled()) {
            logger.debugf("Using DN [%s] for updating LDAP password of user", userDN);
        }

        if (getConfig().isActiveDirectory()) {
            updateADPassword(userDN, password);
        } else {
            ModificationItem[] mods = new ModificationItem[1];

            try {
                BasicAttribute mod0 = new BasicAttribute(LDAPConstants.USER_PASSWORD_ATTRIBUTE, password);

                mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, mod0);

                operationManager.modifyAttribute(userDN, mod0);
            } catch (ModelException me) {
                throw me;
            } catch (Exception e) {
                throw new ModelException("Error updating password.", e);
            }
        }
    }


    private void updateADPassword(String userDN, String password) {
        try {
            // Replace the "unicdodePwd" attribute with a new value
            // Password must be both Unicode and a quoted string
            String newQuotedPassword = "\"" + password + "\"";
            byte[] newUnicodePassword = newQuotedPassword.getBytes("UTF-16LE");

            BasicAttribute unicodePwd = new BasicAttribute("unicodePwd", newUnicodePassword);

            List<ModificationItem> modItems = new ArrayList<ModificationItem>();
            modItems.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, unicodePwd));

            operationManager.modifyAttributes(userDN, modItems.toArray(new ModificationItem[] {}));
        } catch (ModelException me) {
            throw me;
        } catch (Exception e) {
            throw new ModelException(e);
        }
    }

    // ************ END CREDENTIALS AND USER SPECIFIC STUFF

    protected StringBuilder createIdentityTypeSearchFilter(final LDAPQuery identityQuery) {
        StringBuilder filter = new StringBuilder();

        for (Condition condition : identityQuery.getConditions()) {
            condition.applyCondition(filter);
        }

        filter.insert(0, "(&");
        filter.append(getObjectClassesFilter(identityQuery.getObjectClasses()));
        filter.append(")");

        if (logger.isTraceEnabled()) {
            logger.tracef("Using filter for LDAP search: %s . Searching in DN: %s", filter, identityQuery.getSearchDn());
        }
        return filter;
    }


    private StringBuilder getObjectClassesFilter(Collection<String> objectClasses) {
        StringBuilder builder = new StringBuilder();

        if (!objectClasses.isEmpty()) {
            for (String objectClass : objectClasses) {
                builder.append("(").append(LDAPConstants.OBJECT_CLASS).append(LDAPConstants.EQUAL).append(objectClass).append(")");
            }
        } else {
            builder.append("(").append(LDAPConstants.OBJECT_CLASS).append(LDAPConstants.EQUAL).append("*").append(")");
        }

        return builder;
    }


    private LDAPObject populateAttributedType(SearchResult searchResult, LDAPQuery ldapQuery) {
        Set<String> readOnlyAttrNames = ldapQuery.getReturningReadOnlyLdapAttributes();
        Set<String> lowerCasedAttrNames = new TreeSet<>();
        for (String attrName : ldapQuery.getReturningLdapAttributes()) {
            lowerCasedAttrNames.add(attrName.toLowerCase());
        }

        try {
            String entryDN = searchResult.getNameInNamespace();
            Attributes attributes = searchResult.getAttributes();

            LDAPObject ldapObject = new LDAPObject();
            LDAPDn dn = LDAPDn.fromString(entryDN);
            ldapObject.setDn(dn);
            ldapObject.setRdnAttributeName(dn.getFirstRdnAttrName());

            NamingEnumeration<? extends Attribute> ldapAttributes = attributes.getAll();

            while (ldapAttributes.hasMore()) {
                Attribute ldapAttribute = ldapAttributes.next();

                try {
                    ldapAttribute.get();
                } catch (NoSuchElementException nsee) {
                    continue;
                }

                String ldapAttributeName = ldapAttribute.getID();

                if (ldapAttributeName.equalsIgnoreCase(getConfig().getUuidLDAPAttributeName())) {
                    Object uuidValue = ldapAttribute.get();
                    ldapObject.setUuid(this.operationManager.decodeEntryUUID(uuidValue));
                }

                // Note: UUID is normally not populated here. It's populated just in case that it's used for name of other attribute as well
                if (!ldapAttributeName.equalsIgnoreCase(getConfig().getUuidLDAPAttributeName()) || (lowerCasedAttrNames.contains(ldapAttributeName.toLowerCase()))) {
                    Set<String> attrValues = new LinkedHashSet<>();
                    NamingEnumeration<?> enumm = ldapAttribute.getAll();
                    while (enumm.hasMoreElements()) {
                        String attrVal = enumm.next().toString().trim();
                        attrValues.add(attrVal);
                    }

                    if (ldapAttributeName.equalsIgnoreCase(LDAPConstants.OBJECT_CLASS)) {
                        ldapObject.setObjectClasses(attrValues);
                    } else {
                        ldapObject.setAttribute(ldapAttributeName, attrValues);

                        // readOnlyAttrNames are lower-cased
                        if (readOnlyAttrNames.contains(ldapAttributeName.toLowerCase())) {
                            ldapObject.addReadOnlyAttributeName(ldapAttributeName);
                        }
                    }
                }
            }

            if (logger.isTraceEnabled()) {
                logger.tracef("Found ldap object and populated with the attributes. LDAP Object: %s", ldapObject.toString());
            }
            return ldapObject;

        } catch (Exception e) {
            throw new ModelException("Could not populate attribute type " + searchResult.getNameInNamespace() + ".", e);
        }
    }


    protected BasicAttributes extractAttributes(LDAPObject ldapObject, boolean isCreate) {
        BasicAttributes entryAttributes = new BasicAttributes();

        for (Map.Entry<String, Set<String>> attrEntry : ldapObject.getAttributes().entrySet()) {
            String attrName = attrEntry.getKey();
            Set<String> attrValue = attrEntry.getValue();

            // ldapObject.getReadOnlyAttributeNames() are lower-cased
            if (!ldapObject.getReadOnlyAttributeNames().contains(attrName.toLowerCase()) && (isCreate || !ldapObject.getRdnAttributeName().equalsIgnoreCase(attrName))) {

                if (attrValue == null) {
                    // Shouldn't happen
                    logger.warnf("Attribute '%s' is null on LDAP object '%s' . Using empty value to be saved to LDAP", attrName, ldapObject.getDn().toString());
                    attrValue = Collections.emptySet();
                }

                // Ignore empty attributes during create
                if (isCreate && attrValue.isEmpty()) {
                    continue;
                }

                BasicAttribute attr = new BasicAttribute(attrName);
                for (String val : attrValue) {
                    if (val == null || val.toString().trim().length() == 0) {
                        val = LDAPConstants.EMPTY_ATTRIBUTE_VALUE;
                    }
                    attr.add(val);
                }

                entryAttributes.put(attr);
            }
        }

        // Don't extract object classes for update
        if (isCreate) {
            BasicAttribute objectClassAttribute = new BasicAttribute(LDAPConstants.OBJECT_CLASS);

            for (String objectClassValue : ldapObject.getObjectClasses()) {
                objectClassAttribute.add(objectClassValue);

                if (objectClassValue.equalsIgnoreCase(LDAPConstants.GROUP_OF_NAMES)
                        || objectClassValue.equalsIgnoreCase(LDAPConstants.GROUP_OF_ENTRIES)
                        || objectClassValue.equalsIgnoreCase(LDAPConstants.GROUP_OF_UNIQUE_NAMES)) {
                    entryAttributes.put(LDAPConstants.MEMBER, LDAPConstants.EMPTY_MEMBER_ATTRIBUTE_VALUE);
                }
            }

            entryAttributes.put(objectClassAttribute);
        }

        return entryAttributes;
    }


    protected String getEntryIdentifier(final LDAPObject ldapObject) {
        try {
            // we need this to retrieve the entry's identifier from the ldap server
            String uuidAttrName = getConfig().getUuidLDAPAttributeName();
            List<SearchResult> search = this.operationManager.search(ldapObject.getDn().toString(), "(" + ldapObject.getDn().getFirstRdn() + ")", Arrays.asList(uuidAttrName), SearchControls.OBJECT_SCOPE);
            Attribute id = search.get(0).getAttributes().get(getConfig().getUuidLDAPAttributeName());

            if (id == null) {
                throw new ModelException("Could not retrieve identifier for entry [" + ldapObject.getDn().toString() + "].");
            }

            return this.operationManager.decodeEntryUUID(id.get());
        } catch (NamingException ne) {
            throw new ModelException("Could not retrieve identifier for entry [" + ldapObject.getDn().toString() + "].");
        }
    }
}
