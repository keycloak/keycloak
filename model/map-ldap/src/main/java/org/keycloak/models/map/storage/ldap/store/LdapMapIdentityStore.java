/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.ldap.store;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Base64;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.map.storage.ldap.config.LdapMapConfig;
import org.keycloak.models.map.storage.ldap.model.LdapMapDn;
import org.keycloak.models.map.storage.ldap.model.LdapMapObject;
import org.keycloak.models.map.storage.ldap.model.LdapMapQuery;
import org.keycloak.representations.idm.LDAPCapabilityRepresentation;
import org.keycloak.representations.idm.LDAPCapabilityRepresentation.CapabilityType;

import javax.naming.AuthenticationException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.AttributeInUseException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.NoSuchAttributeException;
import javax.naming.directory.SchemaViolationException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * An IdentityStore implementation backed by an LDAP directory
 *
 * @author Shane Bryzak
 * @author Anil Saldhana
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class LdapMapIdentityStore implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(LdapMapIdentityStore.class);
    private static final Pattern rangePattern = Pattern.compile("([^;]+);range=([0-9]+)-([0-9]+|\\*)");

    private final LdapMapConfig config;
    private final LdapMapOperationManager operationManager;

    public LdapMapIdentityStore(KeycloakSession session, LdapMapConfig config) {
        this.config = config;
        this.operationManager = new LdapMapOperationManager(session, config);
    }

    public LdapMapConfig getConfig() {
        return this.config;
    }

    public void add(LdapMapObject ldapObject) {
        // id will be assigned by the ldap server
        if (ldapObject.getId() != null) {
            throw new ModelException("Can't add object with already assigned uuid");
        }

        String entryDN = ldapObject.getDn().toString();
        BasicAttributes ldapAttributes = extractAttributesForSaving(ldapObject, true);
        this.operationManager.createSubContext(entryDN, ldapAttributes);
        ldapObject.setId(getEntryIdentifier(ldapObject));

        if (logger.isDebugEnabled()) {
            logger.debugf("Type with identifier [%s] and dn [%s] successfully added to LDAP store.", ldapObject.getId(), entryDN);
        }
    }

    public void addMemberToGroup(String groupDn, String memberAttrName, String value) {
        // do not check EMPTY_MEMBER_ATTRIBUTE_VALUE, we save one useless query
        // the value will be there forever for objectclasses that enforces the attribute as MUST
        BasicAttribute attr = new BasicAttribute(memberAttrName, value);
        ModificationItem item = new ModificationItem(DirContext.ADD_ATTRIBUTE, attr);
        try {
            this.operationManager.modifyAttributesNaming(groupDn, new ModificationItem[]{item}, null);
        } catch (AttributeInUseException e) {
            logger.debugf("Group %s already contains the member %s", groupDn, value);
        } catch (NamingException e) {
            throw new ModelException("Could not modify attribute for DN [" + groupDn + "]", e);
        }
    }

    public void removeMemberFromGroup(String groupDn, String memberAttrName, String value) {
        BasicAttribute attr = new BasicAttribute(memberAttrName, value);
        ModificationItem item = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, attr);
        try {
            this.operationManager.modifyAttributesNaming(groupDn, new ModificationItem[]{item}, null);
        } catch (NoSuchAttributeException e) {
            logger.debugf("Group %s does not contain the member %s", groupDn, value);
        } catch (SchemaViolationException e) {
            // schema violation removing one member => add the empty attribute, it cannot be other thing
            logger.infof("Schema violation in group %s removing member %s. Trying adding empty member attribute.", groupDn, value);
            try {
                this.operationManager.modifyAttributesNaming(groupDn,
                        new ModificationItem[]{item, new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute(memberAttrName, LDAPConstants.EMPTY_MEMBER_ATTRIBUTE_VALUE))},
                        null);
            } catch (NamingException ex) {
                throw new ModelException("Could not modify attribute for DN [" + groupDn + "]", ex);
            }
        } catch (NamingException e) {
            throw new ModelException("Could not modify attribute for DN [" + groupDn + "]", e);
        }
    }

    public void update(LdapMapObject ldapObject) {
        checkRename(ldapObject);

        BasicAttributes updatedAttributes = extractAttributesForSaving(ldapObject, false);
        NamingEnumeration<Attribute> attributes = updatedAttributes.getAll();

        String entryDn = ldapObject.getDn().toString();
        this.operationManager.modifyAttributes(entryDn, attributes);

        if (logger.isDebugEnabled()) {
            logger.debugf("Type with identifier [%s] and DN [%s] successfully updated to LDAP store.", ldapObject.getId(), entryDn);
        }
    }

    protected void checkRename(LdapMapObject ldapObject) {
        LdapMapDn.RDN firstRdn = ldapObject.getDn().getFirstRdn();
        String oldDn = ldapObject.getDn().toString();

        // Detect which keys will need to be updated in RDN, which are new keys to be added, and which are to be removed
        List<String> toUpdateKeys = firstRdn.getAllKeys();
        toUpdateKeys.retainAll(ldapObject.getRdnAttributeNames());

        List<String> toRemoveKeys = firstRdn.getAllKeys();
        toRemoveKeys.removeAll(ldapObject.getRdnAttributeNames());

        List<String> toAddKeys = new ArrayList<>(ldapObject.getRdnAttributeNames());
        toAddKeys.removeAll(firstRdn.getAllKeys());

        // Go through all the keys in the oldRDN and doublecheck if they are changed or not
        boolean changed = false;
        for (String attrKey : toUpdateKeys) {
            if (ldapObject.getReadOnlyAttributeNames().contains(attrKey.toLowerCase())) {
                continue;
            }

            String rdnAttrVal = ldapObject.getAttributeAsString(attrKey);

            // Could be the case when RDN attribute of the target object is not included in Keycloak mappers
            if (rdnAttrVal == null) {
                continue;
            }

            String oldRdnAttrVal = firstRdn.getAttrValue(attrKey);

            if (!oldRdnAttrVal.equalsIgnoreCase(rdnAttrVal)) {
                changed = true;
                firstRdn.setAttrValue(attrKey, rdnAttrVal);
            }
        }

        // Add new keys
        for (String attrKey : toAddKeys) {
            String rdnAttrVal = ldapObject.getAttributeAsString(attrKey);

            // Could be the case when RDN attribute of the target object is not included in Keycloak mappers
            if (rdnAttrVal == null) {
                continue;
            }

            changed = true;
            firstRdn.setAttrValue(attrKey, rdnAttrVal);
        }

        // Remove old keys
        for (String attrKey : toRemoveKeys) {
            changed |= firstRdn.removeAttrValue(attrKey);
        }

        if (changed) {
            LdapMapDn newLdapMapDn = ldapObject.getDn().getParentDn();
            newLdapMapDn.addFirst(firstRdn);

            String newDn = newLdapMapDn.toString();

            logger.debugf("Renaming LDAP Object. Old DN: [%s], New DN: [%s]", oldDn, newDn);

            // In case, that there is conflict (For example already existing "CN=John Anthony"), the different DN is returned
            newDn = this.operationManager.renameEntry(oldDn, newDn, true);

            ldapObject.setDn(LdapMapDn.fromString(newDn));
        }
    }

    public void remove(LdapMapObject ldapObject) {
        this.operationManager.removeEntry(ldapObject.getDn().toString());

        if (logger.isDebugEnabled()) {
            logger.debugf("Type with identifier [%s] and DN [%s] successfully removed from LDAP store.", ldapObject.getId(), ldapObject.getDn().toString());
        }
    }

    public LdapMapObject fetchById(String id, LdapMapQuery identityQuery) {
        SearchResult search = this.operationManager
                .lookupById(identityQuery.getSearchDn(), id, identityQuery.getReturningLdapAttributes());

        if (search != null) {
            return populateAttributedType(search, identityQuery);
        } else {
            return null;
        }
    }

    public List<LdapMapObject> fetchQueryResults(LdapMapQuery identityQuery) {
        List<LdapMapObject> results = new ArrayList<>();

        StringBuilder filter = null;

        try {
            String baseDN = identityQuery.getSearchDn();

            filter = createIdentityTypeSearchFilter(identityQuery);

            List<SearchResult> search;
            search = this.operationManager.search(baseDN, filter.toString(), identityQuery.getReturningLdapAttributes(), identityQuery.getSearchScope());

            for (SearchResult result : search) {
                // don't add the branch in subtree search
                if (identityQuery.getSearchScope() != SearchControls.SUBTREE_SCOPE || !result.getNameInNamespace().equalsIgnoreCase(baseDN)) {
                    results.add(populateAttributedType(result, identityQuery));
                }
            }
        } catch (Exception e) {
            throw new ModelException("Querying of LDAP failed " + identityQuery + ", filter: " + filter, e);
        }

        return results;
    }

    public Set<LDAPCapabilityRepresentation> queryServerCapabilities() {
        Set<LDAPCapabilityRepresentation> result = new LinkedHashSet<>();
        try {
            List<String> attrs = new ArrayList<>();
            attrs.add("supportedControl");
            attrs.add("supportedExtension");
            attrs.add("supportedFeatures");
            List<SearchResult> searchResults = operationManager
                .search("", "(objectClass=*)", Collections.unmodifiableCollection(attrs), SearchControls.OBJECT_SCOPE);
            if (searchResults.size() != 1) {
                throw new ModelException("Could not query root DSE: unexpected result size");
            }
            SearchResult rootDse = searchResults.get(0);
            Attributes attributes = rootDse.getAttributes();
            for (String attr: attrs) {
                Attribute attribute = attributes.get(attr);
                if (null != attribute) {
                    CapabilityType capabilityType = CapabilityType.fromRootDseAttributeName(attr);
                    NamingEnumeration<?> values = attribute.getAll();
                    while (values.hasMoreElements()) {
                        Object o = values.nextElement();
                        LDAPCapabilityRepresentation capability = new LDAPCapabilityRepresentation(o, capabilityType);
                        logger.info("rootDSE query: " + capability);
                        result.add(capability);
                    }
                }
            }
            return result;
        } catch (NamingException e) {
            throw new ModelException("Failed to query root DSE: " + e.getMessage(), e);
        }
    }

    // *************** CREDENTIALS AND USER SPECIFIC STUFF

    public void validatePassword(LdapMapObject user, String password) throws AuthenticationException {
        String userDN = user.getDn().toString();

        if (logger.isTraceEnabled()) {
            logger.tracef("Using DN [%s] for authentication of user", userDN);
        }

        operationManager.authenticate(userDN, password);
    }

    // ************ END CREDENTIALS AND USER SPECIFIC STUFF

    protected StringBuilder createIdentityTypeSearchFilter(final LdapMapQuery identityQuery) {
        StringBuilder filter = identityQuery.getModelCriteriaBuilder().getPredicateFunc().get();

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


    private LdapMapObject populateAttributedType(SearchResult searchResult, LdapMapQuery ldapQuery) {
        Set<String> readOnlyAttrNames = ldapQuery.getReturningReadOnlyLdapAttributes();
        Set<String> lowerCasedAttrNames = new TreeSet<>();
        for (String attrName : ldapQuery.getReturningLdapAttributes()) {
            lowerCasedAttrNames.add(attrName.toLowerCase());
        }

        try {
            String entryDN = searchResult.getNameInNamespace();
            Attributes attributes = searchResult.getAttributes();

            LdapMapObject ldapObject = new LdapMapObject();
            LdapMapDn dn = LdapMapDn.fromString(entryDN);
            ldapObject.setDn(dn);
            ldapObject.setRdnAttributeNames(dn.getFirstRdn().getAllKeys());

            NamingEnumeration<? extends Attribute> ldapAttributes = attributes.getAll();

            while (ldapAttributes.hasMore()) {
                Attribute ldapAttribute = ldapAttributes.next();

                try {
                    ldapAttribute.get();
                } catch (NoSuchElementException nsee) {
                    continue;
                }

                String ldapAttributeName = ldapAttribute.getID();

                // check for ranged attribute
                Matcher m = rangePattern.matcher(ldapAttributeName);
                if (m.matches()) {
                    ldapAttributeName = m.group(1);
                    // range=X-* means all the attributes returned
                    if (!m.group(3).equals("*")) {
                        try {
                            int max = Integer.parseInt(m.group(3));
                            ldapObject.addRangedAttribute(ldapAttributeName, max);
                        } catch (NumberFormatException e) {
                            logger.warnf("Invalid ranged expresion for attribute: %s", m.group(0));
                        }
                    }
                }

                if (ldapAttributeName.equalsIgnoreCase(getConfig().getUuidLDAPAttributeName())) {
                    Object uuidValue = ldapAttribute.get();
                    ldapObject.setId(this.operationManager.decodeEntryUUID(uuidValue));
                }

                // Note: UUID is normally not populated here. It's populated just in case that it's used for name of other attribute as well
                if (!ldapAttributeName.equalsIgnoreCase(getConfig().getUuidLDAPAttributeName()) || (lowerCasedAttrNames.contains(ldapAttributeName.toLowerCase()))) {
                    Set<String> attrValues = new LinkedHashSet<>();
                    NamingEnumeration<?> enumm = ldapAttribute.getAll();
                    while (enumm.hasMoreElements()) {
                        Object val = enumm.next();

                        if (val instanceof byte[]) { // byte[]
                            String attrVal = Base64.encodeBytes((byte[]) val);
                            attrValues.add(attrVal);
                        } else { // String
                            String attrVal = val.toString().trim();
                            attrValues.add(attrVal);
                        }
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


    protected BasicAttributes extractAttributesForSaving(LdapMapObject ldapObject, boolean isCreate) {
        BasicAttributes entryAttributes = new BasicAttributes();

        Set<String> rdnAttrNamesLowerCased = ldapObject.getRdnAttributeNames().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        for (Map.Entry<String, Set<String>> attrEntry : ldapObject.getAttributes().entrySet()) {
            String attrName = attrEntry.getKey();
            Set<String> attrValue = attrEntry.getValue();

            if (attrValue == null) {
                // Shouldn't happen
                logger.warnf("Attribute '%s' is null on LDAP object '%s' . Using empty value to be saved to LDAP", attrName, ldapObject.getDn().toString());
                attrValue = Collections.emptySet();
            }

            String attrNameLowercased = attrName.toLowerCase();
            if (
                // Ignore empty attributes on create (changetype: add)
                !(isCreate && attrValue.isEmpty()) &&

                // Since we're extracting for saving, skip read-only attributes. ldapObject.getReadOnlyAttributeNames() are lower-cased
                !ldapObject.getReadOnlyAttributeNames().contains(attrNameLowercased) &&

                // Only extract RDN for create since it can't be changed on update
                (isCreate || !rdnAttrNamesLowerCased.contains(attrNameLowercased))
            ) {
                if (getConfig().getBinaryAttributeNames().contains(attrName)) {
                    // Binary attribute
                    entryAttributes.put(createBinaryBasicAttribute(attrName, attrValue));
                } else {
                    // Text attribute
                    entryAttributes.put(createBasicAttribute(attrName, attrValue));
                }
            }
        }

        // Don't extract object classes for update
        if (isCreate) {
            BasicAttribute objectClassAttribute = new BasicAttribute(LDAPConstants.OBJECT_CLASS);

            for (String objectClassValue : ldapObject.getObjectClasses()) {
                objectClassAttribute.add(objectClassValue);
            }

            entryAttributes.put(objectClassAttribute);
        }

        return entryAttributes;
    }

    private BasicAttribute createBasicAttribute(String attrName, Set<String> attrValue) {
        BasicAttribute attr = new BasicAttribute(attrName);

        for (String value : attrValue) {
            if (value == null || value.trim().length() == 0) {
                value = LDAPConstants.EMPTY_ATTRIBUTE_VALUE;
            }

            attr.add(value);
        }

        return attr;
    }

    private BasicAttribute createBinaryBasicAttribute(String attrName, Set<String> attrValue) {
        BasicAttribute attr = new BasicAttribute(attrName);

        for (String value : attrValue) {
            if (value == null || value.trim().length() == 0) {
                value = LDAPConstants.EMPTY_ATTRIBUTE_VALUE;
            }

            try {
                byte[] bytes = Base64.decode(value);
                attr.add(bytes);
            } catch (IOException ioe) {
                logger.warnf("Wasn't able to Base64 decode the attribute value. Ignoring attribute update. Attribute: %s, Attribute value: %s", attrName, attrValue);
            }
        }

        return attr;
    }

    protected String getEntryIdentifier(final LdapMapObject ldapObject) {
        try {
            // we need this to retrieve the entry's identifier from the ldap server
            String uuidAttrName = getConfig().getUuidLDAPAttributeName();

            String rdn = ldapObject.getDn().getFirstRdn().toString(false);
            String filter = "(" + LdapMapEscapeStrategy.DEFAULT.escape(rdn) + ")";
            List<SearchResult> search = this.operationManager.search(ldapObject.getDn().toString(), filter, Collections.singletonList(uuidAttrName), SearchControls.OBJECT_SCOPE);
            Attribute id = search.get(0).getAttributes().get(getConfig().getUuidLDAPAttributeName());

            if (id == null) {
                throw new ModelException("Could not retrieve identifier for entry [" + ldapObject.getDn().toString() + "].");
            }

            return this.operationManager.decodeEntryUUID(id.get());
        } catch (NamingException ne) {
            throw new ModelException("Could not retrieve identifier for entry [" + ldapObject.getDn().toString() + "].");
        }
    }

    @Override
    public void close() {
        operationManager.close();
    }
}
