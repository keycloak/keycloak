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
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.idm.model.LDAPDn;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.mappers.LDAPOperationDecorator;

import javax.naming.AuthenticationException;
import javax.naming.Binding;
import javax.naming.Context;
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
import javax.naming.ldap.LdapName;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
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

    private static final Logger perfLogger = Logger.getLogger(LDAPOperationManager.class, "perf");

    private final KeycloakSession session;
    private final LDAPConfig config;

    public LDAPOperationManager(KeycloakSession session, LDAPConfig config) {
        this.session = session;
        this.config = config;
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


                @Override
                public String toString() {
                    return new StringBuilder("LdapOperation: remove\n")
                            .append(" dn: ").append(entryDn)
                            .toString();
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
                            context.rename(new LdapName(oldDn), new LdapName(dn));
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


                @Override
                public String toString() {
                    return new StringBuilder("LdapOperation: renameEntry\n")
                            .append(" oldDn: ").append(oldDn).append("\n")
                            .append(" newDn: ").append(newDn)
                            .toString();
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
                    NamingEnumeration<SearchResult> search = context.search(new LdapName(baseDN), filter, cons);

                    while (search.hasMoreElements()) {
                        result.add(search.nextElement());
                    }

                    search.close();

                    return result;
                }


                @Override
                public String toString() {
                    return new StringBuilder("LdapOperation: search\n")
                            .append(" baseDn: ").append(baseDN).append("\n")
                            .append(" filter: ").append(filter).append("\n")
                            .append(" searchScope: ").append(searchScope).append("\n")
                            .append(" returningAttrs: ").append(returningAttributes).append("\n")
                            .append(" resultSize: ").append(result.size())
                            .toString();
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

        // Very 1st page. Pagination context is not yet present
        if (identityQuery.getPaginationContext() == null) {
            identityQuery.initPagination();
        }

        try {
            return execute(new LdapOperation<List<SearchResult>>() {

                @Override
                public List<SearchResult> execute(LdapContext context) throws NamingException {
                    try {
                        byte[] cookie = identityQuery.getPaginationContext().getCookie();
                        PagedResultsControl pagedControls = new PagedResultsControl(identityQuery.getLimit(), cookie, Control.CRITICAL);
                        context.setRequestControls(new Control[] { pagedControls });

                        NamingEnumeration<SearchResult> search = context.search(new LdapName(baseDN), filter, cons);

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
                                    identityQuery.getPaginationContext().setCookie(cookie);
                                }
                            }
                        }

                        return result;
                    } catch (IOException ioe) {
                        logger.errorf(ioe, "Could not query server with paginated query using DN [%s], filter [%s]", baseDN, filter);
                        throw new NamingException(ioe.getMessage());
                    }
                }


                @Override
                public String toString() {
                    return new StringBuilder("LdapOperation: searchPaginated\n")
                            .append(" baseDn: ").append(baseDN).append("\n")
                            .append(" filter: ").append(filter).append("\n")
                            .append(" searchScope: ").append(identityQuery.getSearchScope()).append("\n")
                            .append(" returningAttrs: ").append(identityQuery.getReturningLdapAttributes()).append("\n")
                            .append(" limit: ").append(identityQuery.getLimit()).append("\n")
                            .append(" resultSize: ").append(result.size())
                            .toString();
                }

            }, identityQuery.getPaginationContext().getLdapContext(), null);
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


                    @Override
                    public String toString() {
                        return new StringBuilder("LdapOperation: GUIDResolve\n")
                                .append(" strObjectGUID: ").append(strObjectGUID)
                                .toString();
                    }


                });

                byte[] objectGUID = (byte[]) attributes.get(LDAPConstants.OBJECT_GUID).get();

                filter = "(&(objectClass=*)(" + getUuidAttributeName() + LDAPConstants.EQUAL + LDAPUtil.convertObjectGUIDToByteString(objectGUID) + "))";
            } catch (NamingException ne) {
                filter = null;
            }
        }

        if (filter == null) {
            filter = "(&(objectClass=*)(" + getUuidAttributeName() + LDAPConstants.EQUAL + id + "))";
        }

        if (logger.isTraceEnabled()) {
            logger.tracef("Using filter for lookup user by LDAP ID: %s", filter);
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
                    NamingEnumeration<SearchResult> search = context.search(new LdapName(baseDN), filter, cons);

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


                @Override
                public String toString() {
                    return new StringBuilder("LdapOperation: lookupById\n")
                            .append(" baseDN: ").append(baseDN).append("\n")
                            .append(" filter: ").append(filter).append("\n")
                            .append(" searchScope: ").append(cons.getSearchScope()).append("\n")
                            .append(" returningAttrs: ").append(returningAttributes)
                            .toString();
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
                enumeration = context.listBindings(new LdapName(dn));

                while (enumeration.hasMore()) {
                    Binding binding = enumeration.next();
                    String name = binding.getNameInNamespace();

                    destroySubcontext(context, name);
                }

                context.unbind(new LdapName(dn));
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

        if (password == null || password.isEmpty()) {
            throw new AuthenticationException("Empty password used");
        }

        LdapContext authCtx = null;
        StartTlsResponse tlsResponse = null;

        try {

            Hashtable<Object, Object> env = LDAPContextManager.getNonAuthConnectionProperties(config);

            // Never use connection pool to prevent password caching
            env.put("com.sun.jndi.ldap.connect.pool", "false");

            if(!this.config.isStartTls()) {
                env.put(Context.SECURITY_AUTHENTICATION, "simple");
                env.put(Context.SECURITY_PRINCIPAL, dn);
                env.put(Context.SECURITY_CREDENTIALS, password);
            }

            authCtx = new InitialLdapContext(env, null);
            if (config.isStartTls()) {
                tlsResponse = LDAPContextManager.startTLS(authCtx, "simple", dn, password.toCharArray());

                // Exception should be already thrown by LDAPContextManager.startTLS if "startTLS" could not be established, but rather do some additional check
                if (tlsResponse == null) {
                    throw new AuthenticationException("Null TLS Response returned from the authentication");
                }
            }
        } catch (AuthenticationException ae) {
            if (logger.isDebugEnabled()) {
                logger.debugf(ae, "Authentication failed for DN [%s]", dn);
            }

            throw ae;
        } catch (Exception e) {
            logger.errorf(e, "Unexpected exception when validating password of DN [%s]", dn);
            throw new AuthenticationException("Unexpected exception when validating password of user");
        } finally {
            if (tlsResponse != null) {
                try {
                    tlsResponse.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (authCtx != null) {
                try {
                    authCtx.close();
                } catch (NamingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void modifyAttributesNaming(final String dn, final ModificationItem[] mods, LDAPOperationDecorator decorator) throws NamingException {
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
                context.modifyAttributes(new LdapName(dn), mods);
                return null;
            }

            @Override
            public String toString() {
                return new StringBuilder("LdapOperation: modify\n")
                        .append(" dn: ").append(dn).append("\n")
                        .append(" modificationsSize: ").append(mods.length)
                        .toString();
            }

        }, decorator);
    }

    public void modifyAttributes(final String dn, final ModificationItem[] mods, LDAPOperationDecorator decorator) {
        try {
            modifyAttributesNaming(dn, mods, decorator);
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
                    DirContext subcontext = context.createSubcontext(new LdapName(name), attributes);

                    subcontext.close();

                    return null;
                }


                @Override
                public String toString() {
                    return new StringBuilder("LdapOperation: create\n")
                            .append(" dn: ").append(name).append("\n")
                            .append(" attributesSize: ").append(attributes.size())
                            .toString();
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
        if (entryUUID instanceof byte[]) {
            if (this.config.isObjectGUID()) {
                return LDAPUtil.decodeObjectGUID((byte[]) entryUUID);
            }
            if (this.config.isEdirectory() && this.config.isEdirectoryGUID()) {
                return LDAPUtil.decodeGuid((byte[]) entryUUID);
            }
        }
        return entryUUID.toString();
    }

    private <R> R execute(LdapOperation<R> operation) throws NamingException {
        return execute(operation, null);
    }

    private <R> R execute(LdapOperation<R> operation, LDAPOperationDecorator decorator) throws NamingException {
        try (LDAPContextManager ldapContextManager = LDAPContextManager.create(session, config)) {
            return execute(operation, ldapContextManager.getLdapContext(), decorator);
        }
    }

    private <R> R execute(LdapOperation<R> operation, LdapContext context, LDAPOperationDecorator decorator) throws NamingException {
        if (context == null) {
            throw new IllegalArgumentException("Ldap context cannot be null");
        }

        Long start = null;

        if (perfLogger.isDebugEnabled()) {
            start = Time.currentTimeMillis();
        }

        try {
            if (decorator != null) {
                decorator.beforeLDAPOperation(context, operation);
            }

            return operation.execute(context);
        } finally {
            if (perfLogger.isDebugEnabled()) {
                long took = Time.currentTimeMillis() - start;

                if (took > 100) {
                    perfLogger.debugf("\n%s\ntook: %d ms\n", operation.toString(), took);
                } else if (perfLogger.isTraceEnabled()) {
                    perfLogger.tracef("\n%s\ntook: %d ms\n", operation.toString(), took);
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
