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

package org.keycloak.storage.ldap.idm.store.ldap;

import org.jboss.logging.Logger;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.idm.model.LDAPDn;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.mappers.LDAPOperationDecorator;

import javax.naming.AuthenticationException;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * <p>This class provides a set of operations to manage LDAP trees.</p>
 *
 * @author Anil Saldhana
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class LDAPOperationManager {

    private static final Logger logger = Logger.getLogger(LDAPOperationManager.class);

    private final LDAPConfig config;
    private final Map<String, Object> connectionProperties;

    public LDAPOperationManager(LDAPConfig config) throws NamingException {
        this.config = config;
        this.connectionProperties = Collections.unmodifiableMap(createConnectionProperties());
    }

    /**
     * <p>
     * Modifies the given {@link javax.naming.directory.Attribute} instance using the given DN. This method performs a REPLACE_ATTRIBUTE
     * operation.
     * </p>
     *
     * @param dn
     * @param attribute
     */
    public void modifyAttribute(String dn, Attribute attribute) {
        ModificationItem[] mods = new ModificationItem[]{new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attribute)};
        modifyAttributes(dn, mods, null);
    }

    /**
     * <p>
     * Modifies the given {@link Attribute} instances using the given DN. This method performs a REPLACE_ATTRIBUTE
     * operation.
     * </p>
     *
     * @param dn
     * @param attributes
     */
    public void modifyAttributes(String dn,  NamingEnumeration<Attribute> attributes) {
        try {
            List<ModificationItem> modItems = new ArrayList<ModificationItem>();
            while (attributes.hasMore()) {
                ModificationItem modItem = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attributes.next());
                modItems.add(modItem);
            }

            modifyAttributes(dn, modItems.toArray(new ModificationItem[] {}), null);
        } catch (NamingException ne) {
            throw new ModelException("Could not modify attributes on entry from DN [" + dn + "]", ne);
        }

    }

    /**
     * <p>
     * Removes the given {@link Attribute} instance using the given DN. This method performs a REMOVE_ATTRIBUTE
     * operation.
     * </p>
     *
     * @param dn
     * @param attribute
     */
    public void removeAttribute(String dn, Attribute attribute) {
        ModificationItem[] mods = new ModificationItem[]{new ModificationItem(DirContext.REMOVE_ATTRIBUTE, attribute)};
        modifyAttributes(dn, mods, null);
    }

    /**
     * <p>
     * Adds the given {@link Attribute} instance using the given DN. This method performs a ADD_ATTRIBUTE operation.
     * </p>
     *
     * @param dn
     * @param attribute
     */
    public void addAttribute(String dn, Attribute attribute) {
        ModificationItem[] mods = new ModificationItem[]{new ModificationItem(DirContext.ADD_ATTRIBUTE, attribute)};
        modifyAttributes(dn, mods, null);
    }

    /**
     * <p>
     * Removes the object from the LDAP tree
     * </p>
     */
    public void removeEntry(final String entryDn) {
        try {
            execute(new LdapOperation<SearchResult>() {
                @Override
                public SearchResult execute(LdapContext context) throws NamingException {
                    if (logger.isTraceEnabled()) {
                        logger.tracef("Removing entry with DN [%s]", entryDn);
                    }
                    destroySubcontext(context, entryDn);
                    return null;
                }
            });
        } catch (NamingException e) {
            throw new ModelException("Could not remove entry from DN [" + entryDn + "]", e);
        }
    }


    /**
     * Rename LDAPObject name (DN)
     *
     * @param oldDn
     * @param newDn
     * @param fallback With fallback=true, we will try to find the another DN in case of conflict. For example if there is an
     *                 attempt to rename to "CN=John Doe", but there is already existing "CN=John Doe", we will try "CN=John Doe0"
     * @return the non-conflicting DN, which was used in the end
     */
    public String renameEntry(String oldDn, String newDn, boolean fallback) {
        try {
            String newNonConflictingDn = execute(new LdapOperation<String>() {
                @Override
                public String execute(LdapContext context) throws NamingException {
                    String dn = newDn;

                    // Max 5 attempts for now
                    int max = 5;
                    for (int i=0 ; i<max ; i++) {
                        try {
                            context.rename(oldDn, dn);
                            return dn;
                        } catch (NameAlreadyBoundException ex) {
                            if (!fallback) {
                                throw ex;
                            } else {
                                String failedDn = dn;
                                if (i<max) {
                                    dn = findNextDNForFallback(newDn, i);
                                    logger.warnf("Failed to rename DN [%s] to [%s]. Will try to fallback to DN [%s]", oldDn, failedDn, dn);
                                } else {
                                    logger.warnf("Failed all fallbacks for renaming [%s]", oldDn);
                                    throw ex;
                                }
                            }
                        }
                    }

                    throw new ModelException("Could not rename entry from DN [" + oldDn + "] to new DN [" + newDn + "]. All fallbacks failed");
                }
            });
            return newNonConflictingDn;
        } catch (NamingException e) {
            throw new ModelException("Could not rename entry from DN [" + oldDn + "] to new DN [" + newDn + "]", e);
        }
    }

    private String findNextDNForFallback(String newDn, int counter) {
        LDAPDn dn = LDAPDn.fromString(newDn);
        String rdnAttrName = dn.getFirstRdnAttrName();
        String rdnAttrVal = dn.getFirstRdnAttrValue();
        LDAPDn parentDn = dn.getParentDn();
        parentDn.addFirst(rdnAttrName, rdnAttrVal + counter);
        return parentDn.toString();
    }


    public List<SearchResult> search(final String baseDN, final String filter, Collection<String> returningAttributes, int searchScope) throws NamingException {
        final List<SearchResult> result = new ArrayList<SearchResult>();
        final SearchControls cons = getSearchControls(returningAttributes, searchScope);

        try {
            return execute(new LdapOperation<List<SearchResult>>() {
                @Override
                public List<SearchResult> execute(LdapContext context) throws NamingException {
                    NamingEnumeration<SearchResult> search = context.search(baseDN, filter, cons);

                    while (search.hasMoreElements()) {
                        result.add(search.nextElement());
                    }

                    search.close();

                    return result;
                }
            });
        } catch (NamingException e) {
            logger.errorf(e, "Could not query server using DN [%s] and filter [%s]", baseDN, filter);
            throw e;
        }
    }

    public List<SearchResult> searchPaginated(final String baseDN, final String filter, final LDAPQuery identityQuery) throws NamingException {
        final List<SearchResult> result = new ArrayList<SearchResult>();
        final SearchControls cons = getSearchControls(identityQuery.getReturningLdapAttributes(), identityQuery.getSearchScope());

        try {
            return execute(new LdapOperation<List<SearchResult>>() {
                @Override
                public List<SearchResult> execute(LdapContext context) throws NamingException {
                    try {
                        byte[] cookie = identityQuery.getPaginationContext();
                        PagedResultsControl pagedControls = new PagedResultsControl(identityQuery.getLimit(), cookie, Control.CRITICAL);
                        context.setRequestControls(new Control[] { pagedControls });

                        NamingEnumeration<SearchResult> search = context.search(baseDN, filter, cons);

                        while (search.hasMoreElements()) {
                            result.add(search.nextElement());
                        }

                        search.close();

                        Control[] responseControls = context.getResponseControls();
                        if (responseControls != null) {
                            for (Control respControl : responseControls) {
                                if (respControl instanceof PagedResultsResponseControl) {
                                    PagedResultsResponseControl prrc = (PagedResultsResponseControl)respControl;
                                    cookie = prrc.getCookie();
                                    identityQuery.setPaginationContext(cookie);
                                }
                            }
                        }

                        return result;
                    } catch (IOException ioe) {
                        logger.errorf(ioe, "Could not query server with paginated query using DN [%s], filter [%s]", baseDN, filter);
                        throw new NamingException(ioe.getMessage());
                    }
                }
            });
        } catch (NamingException e) {
            logger.errorf(e, "Could not query server using DN [%s] and filter [%s]", baseDN, filter);
            throw e;
        }
    }

    private SearchControls getSearchControls(Collection<String> returningAttributes, int searchScope) {
        final SearchControls cons = new SearchControls();

        cons.setSearchScope(searchScope);
        cons.setReturningObjFlag(false);

        returningAttributes = getReturningAttributes(returningAttributes);

        cons.setReturningAttributes(returningAttributes.toArray(new String[returningAttributes.size()]));
        return cons;
    }

    public String getFilterById(String id) {
        String filter = null;

        if (this.config.isObjectGUID()) {
            final String strObjectGUID = "<GUID=" + id + ">";

            try {
                Attributes attributes = execute(new LdapOperation<Attributes>() {
                    @Override
                    public Attributes execute(LdapContext context) throws NamingException {
                        return context.getAttributes(strObjectGUID);
                    }
                });

                byte[] objectGUID = (byte[]) attributes.get(LDAPConstants.OBJECT_GUID).get();

                filter = "(&(objectClass=*)(" + getUuidAttributeName() + LDAPConstants.EQUAL + LDAPUtil.convertObjectGUIToByteString(objectGUID) + "))";
            } catch (NamingException ne) {
                filter = null;
            }
        }

        if (filter == null) {
            filter = "(&(objectClass=*)(" + getUuidAttributeName() + LDAPConstants.EQUAL + id + "))";
        }

        return filter;
    }

    public SearchResult lookupById(final String baseDN, final String id, final Collection<String> returningAttributes) {
        final String filter = getFilterById(id);

        try {
            final SearchControls cons = getSearchControls(returningAttributes, this.config.getSearchScope());

            return execute(new LdapOperation<SearchResult>() {
                @Override
                public SearchResult execute(LdapContext context) throws NamingException {
                    NamingEnumeration<SearchResult> search = context.search(baseDN, filter, cons);

                    try {
                        if (search.hasMoreElements()) {
                            return search.next();
                        }
                    } finally {
                        if (search != null) {
                            search.close();
                        }
                    }

                    return null;
                }
            });
        } catch (NamingException e) {
            throw new ModelException("Could not query server using DN [" + baseDN + "] and filter [" + filter + "]", e);
        }
    }

    /**
     * <p>
     * Destroys a subcontext with the given DN from the LDAP tree.
     * </p>
     *
     * @param dn
     */
    private void destroySubcontext(LdapContext context, final String dn) {
        try {
            NamingEnumeration<Binding> enumeration = null;

            try {
                enumeration = context.listBindings(dn);

                while (enumeration.hasMore()) {
                    Binding binding = enumeration.next();
                    String name = binding.getNameInNamespace();

                    destroySubcontext(context, name);
                }

                context.unbind(dn);
            } finally {
                try {
                    enumeration.close();
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            throw new ModelException("Could not unbind DN [" + dn + "]", e);
        }
    }

    /**
     * <p>
     * Performs a simple authentication using the given DN and password to bind to the authentication context.
     * </p>
     *
     * @param dn
     * @param password
     * @throws AuthenticationException if authentication is not successful
     *
     */
    public void authenticate(String dn, String password) throws AuthenticationException {
        InitialContext authCtx = null;

        try {
            if (password == null || password.isEmpty()) {
                throw new AuthenticationException("Empty password used");
            }

            Hashtable<String, Object> env = new Hashtable<String, Object>(this.connectionProperties);

            env.put(Context.SECURITY_AUTHENTICATION, LDAPConstants.AUTH_TYPE_SIMPLE);
            env.put(Context.SECURITY_PRINCIPAL, dn);
            env.put(Context.SECURITY_CREDENTIALS, password);

            // Never use connection pool to prevent password caching
            env.put("com.sun.jndi.ldap.connect.pool", "false");

            authCtx = new InitialLdapContext(env, null);

        } catch (AuthenticationException ae) {
            if (logger.isDebugEnabled()) {
                logger.debugf(ae, "Authentication failed for DN [%s]", dn);
            }

            throw ae;
        } catch (Exception e) {
            logger.errorf(e, "Unexpected exception when validating password of DN [%s]", dn);
            throw new AuthenticationException("Unexpected exception when validating password of user");
        } finally {
            if (authCtx != null) {
                try {
                    authCtx.close();
                } catch (NamingException e) {

                }
            }
        }
    }

    public void modifyAttributes(final String dn, final ModificationItem[] mods, LDAPOperationDecorator decorator) {
        try {
            if (logger.isTraceEnabled()) {
                logger.tracef("Modifying attributes for entry [%s]: [", dn);

                for (ModificationItem item : mods) {
                    Object values;

                    if (item.getAttribute().size() > 0) {
                        values = item.getAttribute().get();
                    } else {
                        values = "No values";
                    }

                    String attrName = item.getAttribute().getID().toUpperCase();
                    if (attrName.contains("PASSWORD") || attrName.contains("UNICODEPWD")) {
                        values = "********************";
                    }

                    logger.tracef("  Op [%s]: %s = %s", item.getModificationOp(), item.getAttribute().getID(), values);
                }

                logger.tracef("]");
            }

            execute(new LdapOperation<Void>() {
                @Override
                public Void execute(LdapContext context) throws NamingException {
                    context.modifyAttributes(dn, mods);
                    return null;
                }
            }, decorator);
        } catch (NamingException e) {
            throw new ModelException("Could not modify attribute for DN [" + dn + "]", e);
        }
    }

    public void createSubContext(final String name, final Attributes attributes) {
        try {
            if (logger.isTraceEnabled()) {
                logger.tracef("Creating entry [%s] with attributes: [", name);

                NamingEnumeration<? extends Attribute> all = attributes.getAll();

                while (all.hasMore()) {
                    Attribute attribute = all.next();

                    String attrName = attribute.getID().toUpperCase();
                    Object attrVal = attribute.get();
                    if (attrName.contains("PASSWORD") || attrName.contains("UNICODEPWD")) {
                        attrVal = "********************";
                    }

                    logger.tracef("  %s = %s", attribute.getID(), attrVal);
                }

                logger.tracef("]");
            }

            execute(new LdapOperation<Void>() {
                @Override
                public Void execute(LdapContext context) throws NamingException {
                    DirContext subcontext = context.createSubcontext(name, attributes);

                    subcontext.close();

                    return null;
                }
            });
        } catch (NamingException e) {
            throw new ModelException("Error creating subcontext [" + name + "]", e);
        }
    }

    private String getUuidAttributeName() {
        return this.config.getUuidLDAPAttributeName();
    }

    public Attributes getAttributes(final String entryUUID, final String baseDN, Set<String> returningAttributes) {
        SearchResult search = lookupById(baseDN, entryUUID, returningAttributes);

        if (search == null) {
            throw new ModelException("Couldn't find item with ID [" + entryUUID + " under base DN [" + baseDN + "]");
        }

        return search.getAttributes();
    }

    public String decodeEntryUUID(final Object entryUUID) {
        String id;
        if (this.config.isObjectGUID() && entryUUID instanceof byte[]) {
            id = LDAPUtil.decodeObjectGUID((byte[]) entryUUID);
        } else {
            id = entryUUID.toString();
        }

        return id;
    }

    private LdapContext createLdapContext() throws NamingException {
        return new InitialLdapContext(new Hashtable<Object, Object>(this.connectionProperties), null);
    }

    private Map<String, Object> createConnectionProperties() {
        HashMap<String, Object> env = new HashMap<String, Object>();

        String authType = this.config.getAuthType();
        env.put(Context.INITIAL_CONTEXT_FACTORY, this.config.getFactoryName());
        env.put(Context.SECURITY_AUTHENTICATION, authType);

        String bindDN = this.config.getBindDN();

        char[] bindCredential = null;

        if (this.config.getBindCredential() != null) {
            bindCredential = this.config.getBindCredential().toCharArray();
        }

        if (!LDAPConstants.AUTH_TYPE_NONE.equals(authType)) {
            env.put(Context.SECURITY_PRINCIPAL, bindDN);
            env.put(Context.SECURITY_CREDENTIALS, bindCredential);
        }

        String url = this.config.getConnectionUrl();

        if (url != null) {
            env.put(Context.PROVIDER_URL, url);
        } else {
            logger.warn("LDAP URL is null. LDAPOperationManager won't work correctly");
        }

        String useTruststoreSpi = this.config.getUseTruststoreSpi();
        LDAPConstants.setTruststoreSpiIfNeeded(useTruststoreSpi, url, env);

        String connectionPooling = this.config.getConnectionPooling();
        if (connectionPooling != null) {
            env.put("com.sun.jndi.ldap.connect.pool", connectionPooling);
        }

        String connectionTimeout = config.getConnectionTimeout();
        if (connectionTimeout != null && !connectionTimeout.isEmpty()) {
            env.put("com.sun.jndi.ldap.connect.timeout", connectionTimeout);
        }

        String readTimeout = config.getReadTimeout();
        if (readTimeout != null && !readTimeout.isEmpty()) {
            env.put("com.sun.jndi.ldap.read.timeout", readTimeout);
        }

        // Just dump the additional properties
        Properties additionalProperties = this.config.getAdditionalConnectionProperties();
        if (additionalProperties != null) {
            for (Object key : additionalProperties.keySet()) {
                env.put(key.toString(), additionalProperties.getProperty(key.toString()));
            }
        }

        StringBuilder binaryAttrsBuilder = new StringBuilder();
        if (this.config.isObjectGUID()) {
            binaryAttrsBuilder.append(LDAPConstants.OBJECT_GUID).append(" ");
        }
        for (String attrName : config.getBinaryAttributeNames()) {
            binaryAttrsBuilder.append(attrName).append(" ");
        }

        String binaryAttrs = binaryAttrsBuilder.toString().trim();
        if (!binaryAttrs.isEmpty()) {
            env.put("java.naming.ldap.attributes.binary", binaryAttrs);
        }

        if (logger.isDebugEnabled()) {
            Map<String, Object> copyEnv = new HashMap<>(env);
            if (copyEnv.containsKey(Context.SECURITY_CREDENTIALS)) {
                copyEnv.put(Context.SECURITY_CREDENTIALS, "**************************************");
            }
            logger.debugf("Creating LdapContext using properties: [%s]", copyEnv);
        }

        return env;
    }

    private <R> R execute(LdapOperation<R> operation) throws NamingException {
        return execute(operation, null);
    }

    private <R> R execute(LdapOperation<R> operation, LDAPOperationDecorator decorator) throws NamingException {
        LdapContext context = null;

        try {
            context = createLdapContext();
            if (decorator != null) {
                decorator.beforeLDAPOperation(context, operation);
            }

            return operation.execute(context);
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException ne) {
                    logger.error("Could not close Ldap context.", ne);
                }
            }
        }
    }

    public interface LdapOperation<R> {
        R execute(LdapContext context) throws NamingException;
    }

    private Set<String> getReturningAttributes(final Collection<String> returningAttributes) {
        Set<String> result = new HashSet<String>();

        result.addAll(returningAttributes);
        result.add(getUuidAttributeName());
        result.add(LDAPConstants.OBJECT_CLASS);

        return result;
    }
}