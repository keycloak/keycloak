package org.keycloak.federation.ldap.idm.store.ldap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
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

import org.jboss.logging.Logger;
import org.keycloak.federation.ldap.idm.model.IdentityType;
import org.keycloak.federation.ldap.idm.query.internal.IdentityQuery;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;

import static javax.naming.directory.SearchControls.SUBTREE_SCOPE;

/**
 * <p>This class provides a set of operations to manage LDAP trees.</p>
 *
 * @author Anil Saldhana
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class LDAPOperationManager {

    private static final Logger logger = Logger.getLogger(LDAPOperationManager.class);

    private final LDAPIdentityStoreConfiguration config;
    private final Map<String, Object> connectionProperties;

    public LDAPOperationManager(LDAPIdentityStoreConfiguration config) throws NamingException {
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
        modifyAttributes(dn, mods);
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

            modifyAttributes(dn, modItems.toArray(new ModificationItem[] {}));
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
        modifyAttributes(dn, mods);
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
        modifyAttributes(dn, mods);
    }

    /**
     * <p>
     * Searches the LDAP tree.
     * </p>
     *
     * @param baseDN
     * @param id
     *
     * @return
     */
    public void removeEntryById(final String baseDN, final String id, final LDAPMappingConfiguration mappingConfiguration) {
        final String filter = getFilterById(baseDN, id);

        try {
            final SearchControls cons = getSearchControls(mappingConfiguration);

            execute(new LdapOperation<SearchResult>() {
                @Override
                public SearchResult execute(LdapContext context) throws NamingException {
                    NamingEnumeration<SearchResult> result = context.search(baseDN, filter, cons);

                    if (result.hasMore()) {
                        SearchResult sr = result.next();
                        if (logger.isDebugEnabled()) {
                            logger.debugf("Removing entry [%s] with attributes: [", sr.getNameInNamespace());

                            NamingEnumeration<? extends Attribute> all = sr.getAttributes().getAll();

                            while (all.hasMore()) {
                                Attribute attribute = all.next();

                                logger.debugf("  %s = %s", attribute.getID(), attribute.get());
                            }

                            logger.debugf("]");
                        }
                        destroySubcontext(context, sr.getNameInNamespace());
                    }

                    result.close();

                    return null;
                }
            });
        } catch (NamingException e) {
            throw new ModelException("Could not remove entry from DN [" + baseDN + "] and id [" + id + "]", e);
        }
    }

    public List<SearchResult> search(final String baseDN, final String filter, LDAPMappingConfiguration mappingConfiguration) throws NamingException {
        final List<SearchResult> result = new ArrayList<SearchResult>();
        final SearchControls cons = getSearchControls(mappingConfiguration);

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

    public <V extends IdentityType> List<SearchResult> searchPaginated(final String baseDN, final String filter, LDAPMappingConfiguration mappingConfiguration, final IdentityQuery<V> identityQuery) throws NamingException {
        final List<SearchResult> result = new ArrayList<SearchResult>();
        final SearchControls cons = getSearchControls(mappingConfiguration);

        try {
            return execute(new LdapOperation<List<SearchResult>>() {
                @Override
                public List<SearchResult> execute(LdapContext context) throws NamingException {
                    try {
                        byte[] cookie = (byte[])identityQuery.getPaginationContext();
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

    private SearchControls getSearchControls(LDAPMappingConfiguration mappingConfiguration) {
        final SearchControls cons = new SearchControls();

        cons.setSearchScope(SUBTREE_SCOPE);
        cons.setReturningObjFlag(false);

        Set<String> returningAttributes = getReturningAttributes(mappingConfiguration);

        cons.setReturningAttributes(returningAttributes.toArray(new String[returningAttributes.size()]));
        return cons;
    }

    public String getFilterById(String baseDN, String id) {
        String filter = null;

        if (this.config.isActiveDirectory()) {
            final String strObjectGUID = "<GUID=" + id + ">";

            try {
                Attributes attributes = execute(new LdapOperation<Attributes>() {
                    @Override
                    public Attributes execute(LdapContext context) throws NamingException {
                        return context.getAttributes(strObjectGUID);
                    }
                });

                byte[] objectGUID = (byte[]) attributes.get(LDAPConstants.OBJECT_GUID).get();

                filter = "(&(objectClass=*)(" + getUniqueIdentifierAttributeName() + LDAPConstants.EQUAL + LDAPUtil.convertObjectGUIToByteString(objectGUID) + "))";
            } catch (NamingException ne) {
                return filter;
            }
        }

        if (filter == null) {
            filter = "(&(objectClass=*)(" + getUniqueIdentifierAttributeName() + LDAPConstants.EQUAL + id + "))";
        }

        return filter;
    }

    public SearchResult lookupById(final String baseDN, final String id, final LDAPMappingConfiguration mappingConfiguration) {
        final String filter = getFilterById(baseDN, id);

        try {
            final SearchControls cons = getSearchControls(mappingConfiguration);

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
     *
     * @return
     */
    public boolean authenticate(String dn, String password) {
        InitialContext authCtx = null;

        try {
            Hashtable<String, Object> env = new Hashtable<String, Object>(this.connectionProperties);

            env.put(Context.SECURITY_PRINCIPAL, dn);
            env.put(Context.SECURITY_CREDENTIALS, password);

            // Never use connection pool to prevent password caching
            env.put("com.sun.jndi.ldap.connect.pool", "false");

            authCtx = new InitialLdapContext(env, null);

            return true;
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debugf(e, "Authentication failed for DN [%s]", dn);
            }

            return false;
        } finally {
            if (authCtx != null) {
                try {
                    authCtx.close();
                } catch (NamingException e) {

                }
            }
        }
    }

    public void modifyAttributes(final String dn, final ModificationItem[] mods) {
        try {
            if (logger.isDebugEnabled()) {
                logger.debugf("Modifying attributes for entry [%s]: [", dn);

                for (ModificationItem item : mods) {
                    Object values;

                    if (item.getAttribute().size() > 0) {
                        values = item.getAttribute().get();
                    } else {
                        values = "No values";
                    }

                    logger.debugf("  Op [%s]: %s = %s", item.getModificationOp(), item.getAttribute().getID(), values);
                }

                logger.debugf("]");
            }

            execute(new LdapOperation<Void>() {
                @Override
                public Void execute(LdapContext context) throws NamingException {
                    context.modifyAttributes(dn, mods);
                    return null;
                }
            });
        } catch (NamingException e) {
            throw new ModelException("Could not modify attribute for DN [" + dn + "]", e);
        }
    }

    public void createSubContext(final String name, final Attributes attributes) {
        try {
            if (logger.isDebugEnabled()) {
                logger.debugf("Creating entry [%s] with attributes: [", name);

                NamingEnumeration<? extends Attribute> all = attributes.getAll();

                while (all.hasMore()) {
                    Attribute attribute = all.next();

                    logger.debugf("  %s = %s", attribute.getID(), attribute.get());
                }

                logger.debugf("]");
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

    private String getUniqueIdentifierAttributeName() {
        return this.config.getUniqueIdentifierAttributeName();
    }

    public Attributes getAttributes(final String entryUUID, final String baseDN, LDAPMappingConfiguration mappingConfiguration) {
        SearchResult search = lookupById(baseDN, entryUUID, mappingConfiguration);

        if (search == null) {
            throw new ModelException("Couldn't find item with entryUUID [" + entryUUID + "] and baseDN [" + baseDN + "]");
        }

        return search.getAttributes();
    }

    public String decodeEntryUUID(final Object entryUUID) {
        String id;

        if (this.config.isActiveDirectory()) {
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

        env.put(Context.INITIAL_CONTEXT_FACTORY, this.config.getFactoryName());
        env.put(Context.SECURITY_AUTHENTICATION, this.config.getAuthType());

        String protocol = this.config.getProtocol();

        if (protocol != null) {
            env.put(Context.SECURITY_PROTOCOL, protocol);
        }

        String bindDN = this.config.getBindDN();

        char[] bindCredential = null;

        if (this.config.getBindCredential() != null) {
            bindCredential = this.config.getBindCredential().toCharArray();
        }

        if (bindDN != null) {
            env.put(Context.SECURITY_PRINCIPAL, bindDN);
            env.put(Context.SECURITY_CREDENTIALS, bindCredential);
        }

        String url = this.config.getLdapURL();

        if (url == null) {
            throw new RuntimeException("url");
        }

        env.put(Context.PROVIDER_URL, url);

        // Just dump the additional properties
        Properties additionalProperties = this.config.getConnectionProperties();

        if (additionalProperties != null) {
            for (Object key : additionalProperties.keySet()) {
                env.put(key.toString(), additionalProperties.getProperty(key.toString()));
            }
        }

        if (config.isActiveDirectory()) {
            env.put("java.naming.ldap.attributes.binary", LDAPConstants.OBJECT_GUID);
        }

        if (logger.isDebugEnabled()) {
            logger.debugf("Creating LdapContext using properties: [%s]", env);
        }

        return env;
    }

    private <R> R execute(LdapOperation<R> operation) throws NamingException {
        LdapContext context = null;

        try {
            // TODO: Remove this
            logger.info("Executing operation: " + operation);
            context = createLdapContext();
            return operation.execute(context);
        } catch (NamingException ne) {
            logger.error("Could not create Ldap context or operation execution error.", ne);
            throw ne;
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

    private interface LdapOperation<R> {
        R execute(LdapContext context) throws NamingException;
    }

    private Set<String> getReturningAttributes(final LDAPMappingConfiguration mappingConfiguration) {
        Set<String> returningAttributes = new HashSet<String>();

        if (mappingConfiguration != null) {
            returningAttributes.addAll(mappingConfiguration.getMappedProperties().values());

            returningAttributes.add(mappingConfiguration.getParentMembershipAttributeName());

//            for (LDAPMappingConfiguration relationshipConfig : this.config.getRelationshipConfigs()) {
//                if (relationshipConfig.getRelatedAttributedType().equals(mappingConfiguration.getMappedClass())) {
//                    returningAttributes.addAll(relationshipConfig.getMappedProperties().values());
//                }
//            }
        } else {
            returningAttributes.add("*");
        }

        returningAttributes.add(getUniqueIdentifierAttributeName());
        returningAttributes.add(LDAPConstants.CREATE_TIMESTAMP);
        returningAttributes.add(LDAPConstants.OBJECT_CLASS);

        return returningAttributes;
    }
}