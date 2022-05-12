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
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.map.storage.ldap.config.LdapMapConfig;
import org.keycloak.models.map.storage.ldap.model.LdapMapDn;
import org.keycloak.truststore.TruststoreProvider;

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
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.StartTlsResponse;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

/**
 * <p>This class provides a set of operations to manage LDAP trees.</p>
 *
 * @author Anil Saldhana
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class LdapMapOperationManager implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(LdapMapOperationManager.class);

    private static final Logger perfLogger = Logger.getLogger(LdapMapOperationManager.class, "perf");

    private final KeycloakSession session;
    private final LdapMapConfig config;
    private LdapMapContextManager ldapMapContextManager;

    public LdapMapOperationManager(KeycloakSession session, LdapMapConfig config) {
        this.session = session;
        this.config = config;
    }

    /**
     * <p>
     * Modifies the given {@link Attribute} instance using the given DN. This method performs a REPLACE_ATTRIBUTE
     * operation.
     * </p>
     *
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
     */
    public void modifyAttributes(String dn,  NamingEnumeration<Attribute> attributes) {
        try {
            List<ModificationItem> modItems = new ArrayList<>();
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
                public SearchResult execute(LdapContext context) {
                    if (logger.isTraceEnabled()) {
                        logger.tracef("Removing entry with DN [%s]", entryDn);
                    }
                    destroySubcontext(context, entryDn);
                    return null;
                }


                @Override
                public String toString() {
                    return "LdapOperation: remove\n" +
                            " dn: " + entryDn;
                }

            });
        } catch (NamingException e) {
            throw new ModelException("Could not remove entry from DN [" + entryDn + "]", e);
        }
    }


    /**
     * Rename LDAPObject name (DN)
     *
     * @param fallback With fallback=true, we will try to find the another DN in case of conflict. For example if there is an
     *                 attempt to rename to "CN=John Doe", but there is already existing "CN=John Doe", we will try "CN=John Doe0"
     * @return the non-conflicting DN, which was used in the end
     */
    public String renameEntry(String oldDn, String newDn, boolean fallback) {
        try {
            return execute(new LdapOperation<String>() {

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
                                dn = findNextDNForFallback(newDn, i);
                                logger.warnf("Failed to rename DN [%s] to [%s]. Will try to fallback to DN [%s]", oldDn, failedDn, dn);
                            }
                        }
                    }
                    throw new ModelException("Could not rename entry from DN [" + oldDn + "] to new DN [" + newDn + "]. All fallbacks failed");
                }


                @Override
                public String toString() {
                    return "LdapOperation: renameEntry\n" +
                            " oldDn: " + oldDn + "\n" +
                            " newDn: " + newDn;
                }

            });
        } catch (NamingException e) {
            throw new ModelException("Could not rename entry from DN [" + oldDn + "] to new DN [" + newDn + "]", e);
        }
    }

    private String findNextDNForFallback(String newDn, int counter) {
        LdapMapDn dn = LdapMapDn.fromString(newDn);
        LdapMapDn.RDN firstRdn = dn.getFirstRdn();
        String rdnAttrName = firstRdn.getAllKeys().get(0);
        String rdnAttrVal = firstRdn.getAttrValue(rdnAttrName);
        LdapMapDn parentDn = dn.getParentDn();
        parentDn.addFirst(rdnAttrName, rdnAttrVal + counter);
        return parentDn.toString();
    }


    public List<SearchResult> search(final String baseDN, final String filter, Collection<String> returningAttributes, int searchScope) throws NamingException {
        final List<SearchResult> result = new ArrayList<>();
        final SearchControls cons = getSearchControls(returningAttributes, searchScope);

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
                return "LdapOperation: search\n" +
                        " baseDn: " + baseDN + "\n" +
                        " filter: " + filter + "\n" +
                        " searchScope: " + searchScope + "\n" +
                        " returningAttrs: " + returningAttributes + "\n" +
                        " resultSize: " + result.size();
            }


        });
    }

    private SearchControls getSearchControls(Collection<String> returningAttributes, int searchScope) {
        final SearchControls cons = new SearchControls();

        cons.setSearchScope(searchScope);
        cons.setReturningObjFlag(false);

        returningAttributes = getReturningAttributes(returningAttributes);

        cons.setReturningAttributes(returningAttributes.toArray(new String[0]));
        return cons;
    }

    public String getFilterById(String id) {
        StringBuilder filter = new StringBuilder();
        filter.insert(0, "(&");

        if (this.config.isObjectGUID()) {
            byte[] objectGUID = LdapMapUtil.encodeObjectGUID(id);
            filter.append("(objectClass=*)(").append(
                    getUuidAttributeName()).append(LDAPConstants.EQUAL)
                .append(LdapMapUtil.convertObjectGUIDToByteString(
                    objectGUID)).append(")");

        } else if (this.config.isEdirectoryGUID()) {
            filter.append("(objectClass=*)(").append(getUuidAttributeName().toUpperCase())
                .append(LDAPConstants.EQUAL
                ).append(LdapMapUtil.convertGUIDToEdirectoryHexString(id)).append(")");
        } else {
            filter.append("(objectClass=*)(").append(getUuidAttributeName()).append(LDAPConstants.EQUAL)
                .append(id).append(")");
        }

        if (config.getCustomUserSearchFilter() != null) {
            filter.append(config.getCustomUserSearchFilter());
        }

        filter.append(")");
        String ldapIdFilter = filter.toString();

        logger.tracef("Using filter for lookup user by LDAP ID: %s", ldapIdFilter);

        return ldapIdFilter;
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
                    return "LdapOperation: lookupById\n" +
                            " baseDN: " + baseDN + "\n" +
                            " filter: " + filter + "\n" +
                            " searchScope: " + cons.getSearchScope() + "\n" +
                            " returningAttrs: " + returningAttributes;
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
                if (enumeration != null) {
                    try {
                        enumeration.close();
                    } catch (Exception e) {
                        logger.warn("problem during close", e);
                    }
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

            Hashtable<Object, Object> env = LdapMapContextManager.getNonAuthConnectionProperties(config);

            // Never use connection pool to prevent password caching
            env.put("com.sun.jndi.ldap.connect.pool", "false");

            if(!this.config.isStartTls()) {
                env.put(Context.SECURITY_AUTHENTICATION, "simple");
                env.put(Context.SECURITY_PRINCIPAL, dn);
                env.put(Context.SECURITY_CREDENTIALS, password);
            }

            authCtx = new InitialLdapContext(env, null);
            if (config.isStartTls()) {
                SSLSocketFactory sslSocketFactory = null;
                String useTruststoreSpi = config.getUseTruststoreSpi();
                if (useTruststoreSpi != null && useTruststoreSpi.equals(LDAPConstants.USE_TRUSTSTORE_ALWAYS)) {
                    TruststoreProvider provider = session.getProvider(TruststoreProvider.class);
                    sslSocketFactory = provider.getSSLSocketFactory();
                }

                tlsResponse = LdapMapContextManager.startTLS(authCtx, "simple", dn, password.toCharArray(), sslSocketFactory);

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
        } catch(RuntimeException re){
            if (logger.isDebugEnabled()) {
                logger.debugf(re, "LDAP Connection TimeOut for DN [%s]", dn);
            }
            
            throw re;

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

    public void modifyAttributesNaming(final String dn, final ModificationItem[] mods, LdapMapOperationDecorator decorator) throws NamingException {
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
                return "LdapOperation: modify\n" +
                        " dn: " + dn + "\n" +
                        " modificationsSize: " + mods.length;
            }

        }, decorator);
    }

    public void modifyAttributes(final String dn, final ModificationItem[] mods, LdapMapOperationDecorator decorator) {
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
                    return "LdapOperation: create\n" +
                            " dn: " + name + "\n" +
                            " attributesSize: " + attributes.size();
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
                return LdapMapUtil.decodeObjectGUID((byte[]) entryUUID);
            }
            if (this.config.isEdirectory() && this.config.isEdirectoryGUID()) {
                return LdapMapUtil.decodeGuid((byte[]) entryUUID);
            }
        }
        return entryUUID.toString();
    }

    private <R> R execute(LdapOperation<R> operation) throws NamingException {
        return execute(operation, null);
    }

    private <R> R execute(LdapOperation<R> operation, LdapMapOperationDecorator decorator) throws NamingException {
        return execute(operation, getLdapContextManager().getLdapContext(), decorator);
    }

    private LdapMapContextManager getLdapContextManager() {
        if (ldapMapContextManager == null) {
            ldapMapContextManager = LdapMapContextManager.create(session, config);
        }
        return ldapMapContextManager;
    }

    private <R> R execute(LdapOperation<R> operation, LdapContext context, LdapMapOperationDecorator decorator) throws NamingException {
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
            if (start != null) {
                long took = Time.currentTimeMillis() - start;

                if (took > 100) {
                    perfLogger.debugf("\n%s\ntook: %d ms\n", operation.toString(), took);
                } else if (perfLogger.isTraceEnabled()) {
                    perfLogger.tracef("\n%s\ntook: %d ms\n", operation.toString(), took);
                }
            }
        }
    }

    @Override
    public void close() {
        ldapMapContextManager.close();
    }

    public interface LdapOperation<R> {
        R execute(LdapContext context) throws NamingException;
    }

    private Set<String> getReturningAttributes(final Collection<String> returningAttributes) {
        Set<String> result = new HashSet<>(returningAttributes);
        result.add(getUuidAttributeName());
        result.add(LDAPConstants.OBJECT_CLASS);

        return result;
    }
}
