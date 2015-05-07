package org.keycloak.federation.ldap.idm.store.ldap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;

import org.jboss.logging.Logger;
import org.keycloak.federation.ldap.idm.model.AttributedType;
import org.keycloak.federation.ldap.idm.model.IdentityType;
import org.keycloak.federation.ldap.idm.model.LDAPUser;
import org.keycloak.federation.ldap.idm.query.AttributeParameter;
import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.QueryParameter;
import org.keycloak.federation.ldap.idm.query.internal.BetweenCondition;
import org.keycloak.federation.ldap.idm.query.internal.IdentityQuery;
import org.keycloak.federation.ldap.idm.query.internal.IdentityQueryBuilder;
import org.keycloak.federation.ldap.idm.query.internal.EqualCondition;
import org.keycloak.federation.ldap.idm.query.internal.GreaterThanCondition;
import org.keycloak.federation.ldap.idm.query.internal.InCondition;
import org.keycloak.federation.ldap.idm.query.internal.LessThanCondition;
import org.keycloak.federation.ldap.idm.query.internal.LikeCondition;
import org.keycloak.federation.ldap.idm.query.internal.OrCondition;
import org.keycloak.federation.ldap.idm.store.IdentityStore;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.utils.reflection.NamedPropertyCriteria;
import org.keycloak.models.utils.reflection.Property;
import org.keycloak.models.utils.reflection.PropertyQueries;
import org.keycloak.models.utils.reflection.TypedPropertyCriteria;
import org.keycloak.util.reflections.Reflections;

/**
 * An IdentityStore implementation backed by an LDAP directory
 *
 * @author Shane Bryzak
 * @author Anil Saldhana
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class LDAPIdentityStore implements IdentityStore {

    private static final Logger logger = Logger.getLogger(LDAPIdentityStore.class);

    public static final String EMPTY_ATTRIBUTE_VALUE = " ";

    private final LDAPIdentityStoreConfiguration config;
    private final LDAPOperationManager operationManager;

    public LDAPIdentityStore(LDAPIdentityStoreConfiguration config) {
        this.config = config;

        try {
            this.operationManager = new LDAPOperationManager(getConfig());
        } catch (NamingException e) {
            throw new ModelException("Couldn't init operation manager", e);
        }
    }

    @Override
    public LDAPIdentityStoreConfiguration getConfig() {
        return this.config;
    }

    @Override
    public void add(AttributedType attributedType) {
        // id will be assigned by the ldap server
        attributedType.setId(null);

        String entryDN = getBindingDN(attributedType, true);
        this.operationManager.createSubContext(entryDN, extractAttributes(attributedType, true));
        addToParentAsMember(attributedType);
        attributedType.setId(getEntryIdentifier(attributedType));

        attributedType.setEntryDN(entryDN);

        if (logger.isTraceEnabled()) {
            logger.tracef("Type with identifier [%s] successfully added to identity store [%s].", attributedType.getId(), this);
        }
    }

    @Override
    public void update(AttributedType attributedType) {
        BasicAttributes updatedAttributes = extractAttributes(attributedType, false);
        NamingEnumeration<Attribute> attributes = updatedAttributes.getAll();

        this.operationManager.modifyAttributes(getBindingDN(attributedType, true), attributes);

        if (logger.isTraceEnabled()) {
            logger.tracef("Type with identifier [%s] successfully updated to identity store [%s].", attributedType.getId(), this);
        }
    }

    @Override
    public void remove(AttributedType attributedType) {
        LDAPMappingConfiguration mappingConfig = getMappingConfig(attributedType.getClass());

        this.operationManager.removeEntryById(getBaseDN(attributedType), attributedType.getId(), mappingConfig);

        if (logger.isTraceEnabled()) {
            logger.tracef("Type with identifier [%s] successfully removed from identity store [%s].", attributedType.getId(), this);
        }
    }

    @Override
    public <V extends IdentityType> List<V> fetchQueryResults(IdentityQuery<V> identityQuery) {
        List<V> results = new ArrayList<V>();

        try {
            if (identityQuery.getSorting() != null && !identityQuery.getSorting().isEmpty()) {
                throw new ModelException("LDAP Identity Store does not support sorted queries.");
            }

            for (Condition condition : identityQuery.getConditions()) {

                if (IdentityType.ID.equals(condition.getParameter())) {
                    if (EqualCondition.class.isInstance(condition)) {
                        EqualCondition equalCondition = (EqualCondition) condition;
                        SearchResult search = this.operationManager
                                .lookupById(getConfig().getBaseDN(), equalCondition.getValue().toString(), null);

                        if (search != null) {
                            results.add((V) populateAttributedType(search, null));
                        }
                    }

                    return results;
                }
            }

            if (!IdentityType.class.equals(identityQuery.getIdentityType())) {
                // the ldap store does not support queries based on root types. Except if based on the identifier.
                LDAPMappingConfiguration ldapEntryConfig = getMappingConfig(identityQuery.getIdentityType());
                StringBuilder filter = createIdentityTypeSearchFilter(identityQuery, ldapEntryConfig);
                String baseDN = getBaseDN(ldapEntryConfig);
                List<SearchResult> search;

                if (getConfig().isPagination() && identityQuery.getLimit() > 0) {
                    search = this.operationManager.searchPaginated(baseDN, filter.toString(), ldapEntryConfig, identityQuery);
                } else {
                    search = this.operationManager.search(baseDN, filter.toString(), ldapEntryConfig);
                }

                for (SearchResult result : search) {
                    if (!result.getNameInNamespace().equals(baseDN)) {
                        results.add((V) populateAttributedType(result, null));
                    }
                }
            }
        } catch (Exception e) {
            throw new ModelException("Querying of identity type failed " + identityQuery, e);
        }

        return results;
    }

    @Override
    public <V extends IdentityType> int countQueryResults(IdentityQuery<V> identityQuery) {
        int limit = identityQuery.getLimit();
        int offset = identityQuery.getOffset();

        identityQuery.setLimit(0);
        identityQuery.setOffset(0);

        int resultCount = identityQuery.getResultList().size();

        identityQuery.setLimit(limit);
        identityQuery.setOffset(offset);

        return resultCount;
    }

    public IdentityQueryBuilder createQueryBuilder() {
        return new IdentityQueryBuilder(this);
    }

    // *************** CREDENTIALS AND USER SPECIFIC STUFF

    @Override
    public boolean validatePassword(LDAPUser user, String password) {
        String userDN = getEntryDNOfUser(user);

        if (logger.isDebugEnabled()) {
            logger.debugf("Using DN [%s] for authentication of user [%s]", userDN, user.getLoginName());
        }

        if (operationManager.authenticate(userDN, password)) {
            return true;
        }

        return false;
    }

    @Override
    public void updatePassword(LDAPUser user, String password) {
        String userDN = getEntryDNOfUser(user);

        if (logger.isDebugEnabled()) {
            logger.debugf("Using DN [%s] for updating LDAP password of user [%s]", userDN, user.getLoginName());
        }

        if (getConfig().isActiveDirectory()) {
            updateADPassword(userDN, password);
        } else {
            ModificationItem[] mods = new ModificationItem[1];

            try {
                BasicAttribute mod0 = new BasicAttribute(LDAPConstants.USER_PASSWORD_ATTRIBUTE, password);

                mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, mod0);

                operationManager.modifyAttribute(userDN, mod0);
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

            // Used in ActiveDirectory to put account into "enabled" state (aka userAccountControl=512, see http://support.microsoft.com/kb/305144/en ) after password update. If value is -1, it's ignored
            if (getConfig().isUserAccountControlsAfterPasswordUpdate()) {
                BasicAttribute userAccountControl = new BasicAttribute("userAccountControl", "512");
                modItems.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, userAccountControl));

                logger.debugf("Attribute userAccountControls will be switched to 512 after password update of user [%s]", userDN);
            }

            operationManager.modifyAttributes(userDN, modItems.toArray(new ModificationItem[] {}));
        } catch (Exception e) {
            throw new ModelException(e);
        }
    }


    private String getEntryDNOfUser(LDAPUser user) {
        // First try if user already has entryDN on him
        String entryDN = user.getEntryDN();
        if (entryDN != null) {
            return entryDN;
        }

        // Need to find user in LDAP
        String username = user.getLoginName();
        user = getUser(username);
        if (user == null) {
            throw new ModelException("No LDAP user found with username " + username);
        }

        return user.getEntryDN();
    }


    public LDAPUser getUser(String username) {

        if (isNullOrEmpty(username)) {
            return null;
        }

        IdentityQueryBuilder queryBuilder = createQueryBuilder();
        List<LDAPUser> agents = queryBuilder.createIdentityQuery(LDAPUser.class)
                .where(queryBuilder.equal(LDAPUser.LOGIN_NAME, username)).getResultList();

        if (agents.isEmpty()) {
            return null;
        } else if (agents.size() == 1) {
            return agents.get(0);
        } else {
            throw new ModelDuplicateException("Error - multiple Agent objects found with same login name");
        }
    }

    // ************ END CREDENTIALS AND USER SPECIFIC STUFF


    private String getBaseDN(final LDAPMappingConfiguration ldapEntryConfig) {
        String baseDN = getConfig().getBaseDN();

        if (ldapEntryConfig.getBaseDN() != null) {
            baseDN = ldapEntryConfig.getBaseDN();
        }

        return baseDN;
    }

    protected <V extends IdentityType> StringBuilder createIdentityTypeSearchFilter(final IdentityQuery<V> identityQuery, final LDAPMappingConfiguration ldapEntryConfig) {
        StringBuilder filter = new StringBuilder();

        for (Condition condition : identityQuery.getConditions()) {
            applyCondition(filter, condition, ldapEntryConfig);
        }


        filter.insert(0, "(&");
        filter.append(getObjectClassesFilter(ldapEntryConfig));
        filter.append(")");

        logger.infof("Using filter for LDAP search: %s", filter);
        return filter;
    }

    protected void applyCondition(StringBuilder filter, Condition condition, LDAPMappingConfiguration ldapEntryConfig) {
        if (OrCondition.class.isInstance(condition)) {
            OrCondition orCondition = (OrCondition) condition;
            filter.append("(|");

            for (Condition innerCondition : orCondition.getInnerConditions()) {
                applyCondition(filter, innerCondition, ldapEntryConfig);
            }

            filter.append(")");
            return;
        }

        QueryParameter queryParameter = condition.getParameter();

        if (!IdentityType.ID.equals(queryParameter)) {
            if (AttributeParameter.class.isInstance(queryParameter)) {
                AttributeParameter attributeParameter = (AttributeParameter) queryParameter;
                String attributeName = ldapEntryConfig.getMappedProperties().get(attributeParameter.getName());

                if (attributeName != null) {
                    if (EqualCondition.class.isInstance(condition)) {
                        EqualCondition equalCondition = (EqualCondition) condition;
                        Object parameterValue = equalCondition.getValue();

                        if (Date.class.isInstance(parameterValue)) {
                            parameterValue = LDAPUtil.formatDate((Date) parameterValue);
                        }

                        filter.append("(").append(attributeName).append(LDAPConstants.EQUAL).append(parameterValue).append(")");
                    } else if (LikeCondition.class.isInstance(condition)) {
                        LikeCondition likeCondition = (LikeCondition) condition;
                        String parameterValue = (String) likeCondition.getValue();

                    } else if (GreaterThanCondition.class.isInstance(condition)) {
                        GreaterThanCondition greaterThanCondition = (GreaterThanCondition) condition;
                        Comparable parameterValue = (Comparable) greaterThanCondition.getValue();

                        if (Date.class.isInstance(parameterValue)) {
                            parameterValue = LDAPUtil.formatDate((Date) parameterValue);
                        }

                        if (greaterThanCondition.isOrEqual()) {
                            filter.append("(").append(attributeName).append(">=").append(parameterValue).append(")");
                        } else {
                            filter.append("(").append(attributeName).append(">").append(parameterValue).append(")");
                        }
                    } else if (LessThanCondition.class.isInstance(condition)) {
                        LessThanCondition lessThanCondition = (LessThanCondition) condition;
                        Comparable parameterValue = (Comparable) lessThanCondition.getValue();

                        if (Date.class.isInstance(parameterValue)) {
                            parameterValue = LDAPUtil.formatDate((Date) parameterValue);
                        }

                        if (lessThanCondition.isOrEqual()) {
                            filter.append("(").append(attributeName).append("<=").append(parameterValue).append(")");
                        } else {
                            filter.append("(").append(attributeName).append("<").append(parameterValue).append(")");
                        }
                    } else if (BetweenCondition.class.isInstance(condition)) {
                        BetweenCondition betweenCondition = (BetweenCondition) condition;
                        Comparable x = betweenCondition.getX();
                        Comparable y = betweenCondition.getY();

                        if (Date.class.isInstance(x)) {
                            x = LDAPUtil.formatDate((Date) x);
                        }

                        if (Date.class.isInstance(y)) {
                            y = LDAPUtil.formatDate((Date) y);
                        }

                        filter.append("(").append(x).append("<=").append(attributeName).append("<=").append(y).append(")");
                    } else if (InCondition.class.isInstance(condition)) {
                        InCondition inCondition = (InCondition) condition;
                        Object[] valuesToCompare = inCondition.getValue();

                        filter.append("(&(");

                        for (int i = 0; i< valuesToCompare.length; i++) {
                            Object value = valuesToCompare[i];

                            filter.append("(").append(attributeName).append(LDAPConstants.EQUAL).append(value).append(")");
                        }

                        filter.append("))");
                    } else {
                        throw new ModelException("Unsupported query condition [" + condition + "].");
                    }
                }
            }
        }
    }

    private StringBuilder getObjectClassesFilter(final LDAPMappingConfiguration ldapEntryConfig) {
        StringBuilder builder = new StringBuilder();

        if (ldapEntryConfig != null && !ldapEntryConfig.getObjectClasses().isEmpty()) {
            for (String objectClass : ldapEntryConfig.getObjectClasses()) {
                builder.append("(").append(LDAPConstants.OBJECT_CLASS).append(LDAPConstants.EQUAL).append(objectClass).append(")");
            }
        } else {
            builder.append("(").append(LDAPConstants.OBJECT_CLASS).append(LDAPConstants.EQUAL).append("*").append(")");
        }

        return builder;
    }

    private AttributedType populateAttributedType(SearchResult searchResult, AttributedType attributedType) {
        return populateAttributedType(searchResult, attributedType, 0);
    }

    private AttributedType populateAttributedType(SearchResult searchResult, AttributedType attributedType, int hierarchyDepthCount) {
        try {
            String entryDN = searchResult.getNameInNamespace();
            Attributes attributes = searchResult.getAttributes();

            if (attributedType == null) {
                attributedType = Reflections.newInstance(getConfig().getSupportedTypeByBaseDN(entryDN, getEntryObjectClasses(attributes)));
            }

            attributedType.setEntryDN(entryDN);

            LDAPMappingConfiguration mappingConfig = getMappingConfig(attributedType.getClass());

            if (hierarchyDepthCount > mappingConfig.getHierarchySearchDepth()) {
                return null;
            }

            if (logger.isTraceEnabled()) {
                logger.tracef("Populating attributed type [%s] from DN [%s]", attributedType, entryDN);
            }

            NamingEnumeration<? extends Attribute> ldapAttributes = attributes.getAll();

            while (ldapAttributes.hasMore()) {
                Attribute ldapAttribute = ldapAttributes.next();
                Object attributeValue;

                try {
                    attributeValue = ldapAttribute.get();
                } catch (NoSuchElementException nsee) {
                    continue;
                }

                String ldapAttributeName = ldapAttribute.getID();

                if (ldapAttributeName.toLowerCase().equals(getConfig().getUniqueIdentifierAttributeName().toLowerCase())) {
                    attributedType.setId(this.operationManager.decodeEntryUUID(attributeValue));
                } else {
                    String attributeName = findAttributeName(mappingConfig.getMappedProperties(), ldapAttributeName);

                    if (attributeName != null) {
                        // Find if it's java property or attribute
                        Property<Object> property = PropertyQueries
                                .createQuery(attributedType.getClass())
                                .addCriteria(new NamedPropertyCriteria(attributeName)).getFirstResult();

                        if (property != null) {
                            if (logger.isTraceEnabled()) {
                                logger.tracef("Populating property [%s] from ldap attribute [%s] with value [%s] from DN [%s].", property.getName(), ldapAttributeName, attributeValue, entryDN);
                            }

                            if (property.getJavaClass().equals(Date.class)) {
                                property.setValue(attributedType, LDAPUtil.parseDate(attributeValue.toString()));
                            } else {
                                property.setValue(attributedType, attributeValue);
                            }
                        } else {
                            if (logger.isTraceEnabled()) {
                                logger.tracef("Populating attribute [%s] from ldap attribute [%s] with value [%s] from DN [%s].", attributeName, ldapAttributeName, attributeValue, entryDN);
                            }

                            attributedType.setAttribute(new org.keycloak.federation.ldap.idm.model.Attribute(attributeName, (Serializable) attributeValue));
                        }
                    }
                }
            }

            if (IdentityType.class.isInstance(attributedType)) {
                IdentityType identityType = (IdentityType) attributedType;

                String createdTimestamp = attributes.get(LDAPConstants.CREATE_TIMESTAMP).get().toString();

                identityType.setCreatedDate(LDAPUtil.parseDate(createdTimestamp));
            }

            LDAPMappingConfiguration entryConfig = getMappingConfig(attributedType.getClass());

            if (mappingConfig.getParentMembershipAttributeName() != null) {
                StringBuilder filter = new StringBuilder("(&");
                String entryBaseDN = entryDN.substring(entryDN.indexOf(LDAPConstants.COMMA) + 1);

                filter
                        .append("(")
                        .append(getObjectClassesFilter(entryConfig))
                        .append(")")
                        .append("(")
                        .append(mappingConfig.getParentMembershipAttributeName())
                        .append(LDAPConstants.EQUAL).append("")
                        .append(getBindingDN(attributedType, false))
                        .append(LDAPConstants.COMMA)
                        .append(entryBaseDN)
                        .append(")");

                filter.append(")");

                if (logger.isTraceEnabled()) {
                    logger.tracef("Searching parent entry for DN [%s] using filter [%s].", entryBaseDN, filter.toString());
                }

                List<SearchResult> search = this.operationManager.search(getConfig().getBaseDN(), filter.toString(), entryConfig);

                if (!search.isEmpty()) {
                    SearchResult next = search.get(0);

                    Property<AttributedType> parentProperty = PropertyQueries
                            .<AttributedType>createQuery(attributedType.getClass())
                            .addCriteria(new TypedPropertyCriteria(attributedType.getClass())).getFirstResult();

                    if (parentProperty != null) {
                        String parentDN = next.getNameInNamespace();
                        String parentBaseDN = parentDN.substring(parentDN.indexOf(",") + 1);
                        Class<? extends AttributedType> baseDNType = getConfig().getSupportedTypeByBaseDN(parentBaseDN, getEntryObjectClasses(attributes));

                        if (parentProperty.getJavaClass().isAssignableFrom(baseDNType)) {
                            if (logger.isTraceEnabled()) {
                                logger.tracef("Found parent [%s] for entry for DN [%s].", parentDN, entryDN);
                            }

                            int hierarchyDepthCount1 = ++hierarchyDepthCount;

                            parentProperty.setValue(attributedType, populateAttributedType(next, null, hierarchyDepthCount1));
                        }
                    }
                } else {
                    if (logger.isTraceEnabled()) {
                        logger.tracef("No parent entry found for DN [%s] using filter [%s].", entryDN, filter.toString());
                    }
                }
            }
        } catch (Exception e) {
            throw new ModelException("Could not populate attribute type " + attributedType + ".", e);
        }

        return attributedType;
    }

    private String findAttributeName(Map<String, String> attrMapping, String ldapAttributeName) {
        for (Map.Entry<String,String> currentAttr : attrMapping.entrySet()) {
            if (currentAttr.getValue().equalsIgnoreCase(ldapAttributeName)) {
                return currentAttr.getKey();
            }
        }

        return null;
    }

    private List<String> getEntryObjectClasses(final Attributes attributes) throws NamingException {
        Attribute objectClassesAttribute = attributes.get(LDAPConstants.OBJECT_CLASS);
        List<String> objectClasses = new ArrayList<String>();

        if (objectClassesAttribute == null) {
            return objectClasses;
        }

        NamingEnumeration<?> all = objectClassesAttribute.getAll();

        while (all.hasMore()) {
            objectClasses.add(all.next().toString());
        }

        return objectClasses;
    }

    protected BasicAttributes extractAttributes(AttributedType attributedType, boolean isCreate) {
        BasicAttributes entryAttributes = new BasicAttributes();
        LDAPMappingConfiguration mappingConfig = getMappingConfig(attributedType.getClass());
        Map<String, String> mappedProperties = mappingConfig.getMappedProperties();

        for (String propertyName : mappedProperties.keySet()) {
            if (!mappingConfig.getReadOnlyAttributes().contains(propertyName) && (isCreate || !mappingConfig.getBindingProperty().getName().equals(propertyName))) {
                Property<Object> property = PropertyQueries
                        .<Object>createQuery(attributedType.getClass())
                        .addCriteria(new NamedPropertyCriteria(propertyName)).getFirstResult();

                Object propertyValue = null;
                if (property != null) {
                    // Mapped Java property on the object
                    propertyValue = property.getValue(attributedType);
                } else {
                    // Not mapped property. So fallback to attribute
                    org.keycloak.federation.ldap.idm.model.Attribute<?> attribute = attributedType.getAttribute(propertyName);
                    if (attribute != null) {
                        propertyValue = attribute.getValue();
                    }
                }

                if (AttributedType.class.isInstance(propertyValue)) {
                    AttributedType referencedType = (AttributedType) propertyValue;
                    propertyValue = getBindingDN(referencedType, true);
                } else {
                    if (propertyValue == null || isNullOrEmpty(propertyValue.toString())) {
                        propertyValue = EMPTY_ATTRIBUTE_VALUE;
                    }
                }

                entryAttributes.put(mappedProperties.get(propertyName), propertyValue);
            }
        }

        // Don't extract object classes for update
        if (isCreate) {
            LDAPMappingConfiguration ldapEntryConfig = getMappingConfig(attributedType.getClass());

            BasicAttribute objectClassAttribute = new BasicAttribute(LDAPConstants.OBJECT_CLASS);

            for (String objectClassValue : ldapEntryConfig.getObjectClasses()) {
                objectClassAttribute.add(objectClassValue);

                if (objectClassValue.equals(LDAPConstants.GROUP_OF_NAMES)
                        || objectClassValue.equals(LDAPConstants.GROUP_OF_ENTRIES)
                        || objectClassValue.equals(LDAPConstants.GROUP_OF_UNIQUE_NAMES)) {
                    entryAttributes.put(LDAPConstants.MEMBER, EMPTY_ATTRIBUTE_VALUE);
                }
            }

            entryAttributes.put(objectClassAttribute);
        }

        return entryAttributes;
    }

    // TODO: Move class StringUtil from SAML module
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    private LDAPMappingConfiguration getMappingConfig(Class<? extends AttributedType> attributedType) {
        LDAPMappingConfiguration mappingConfig = getConfig().getMappingConfig(attributedType);

        if (mappingConfig == null) {
            throw new ModelException("Not mapped type [" + attributedType + "].");
        }

        return mappingConfig;
    }

    public String getBindingDN(AttributedType attributedType, boolean appendBaseDN) {
        LDAPMappingConfiguration mappingConfig = getMappingConfig(attributedType.getClass());
        Property<String> idProperty = mappingConfig.getIdProperty();

        String baseDN;

        if (mappingConfig.getBaseDN() == null || !appendBaseDN) {
            baseDN = "";
        } else {
            baseDN = LDAPConstants.COMMA + getBaseDN(attributedType);
        }

        Property<String> bindingProperty = mappingConfig.getBindingProperty();
        String bindingAttribute;
        String dn;

        if (bindingProperty == null) {
            bindingAttribute = mappingConfig.getMappedProperties().get(idProperty.getName());
            dn = idProperty.getValue(attributedType);
        } else {
            bindingAttribute = mappingConfig.getMappedProperties().get(bindingProperty.getName());
            dn = mappingConfig.getBindingProperty().getValue(attributedType);
        }

        return bindingAttribute + LDAPConstants.EQUAL + dn + baseDN;
    }

    private String getBaseDN(AttributedType attributedType) {
        LDAPMappingConfiguration mappingConfig = getMappingConfig(attributedType.getClass());
        String baseDN = mappingConfig.getBaseDN();
        String parentDN = mappingConfig.getParentMapping().get(mappingConfig.getIdProperty().getValue(attributedType));

        if (parentDN != null) {
            baseDN = parentDN;
        } else {
            Property<AttributedType> parentProperty = PropertyQueries
                    .<AttributedType>createQuery(attributedType.getClass())
                    .addCriteria(new TypedPropertyCriteria(attributedType.getClass())).getFirstResult();

            if (parentProperty != null) {
                AttributedType parentType = parentProperty.getValue(attributedType);

                if (parentType != null) {
                    Property<String> parentIdProperty = getMappingConfig(parentType.getClass()).getIdProperty();

                    String parentId = parentIdProperty.getValue(parentType);

                    String parentBaseDN = mappingConfig.getParentMapping().get(parentId);

                    if (parentBaseDN != null) {
                        baseDN = parentBaseDN;
                    } else {
                        baseDN = getBaseDN(parentType);
                    }
                }
            }
        }

        if (baseDN == null) {
            baseDN = getConfig().getBaseDN();
        }

        return baseDN;
    }

    protected void addToParentAsMember(final AttributedType attributedType) {
        LDAPMappingConfiguration entryConfig = getMappingConfig(attributedType.getClass());

        if (entryConfig.getParentMembershipAttributeName() != null) {
            Property<AttributedType> parentProperty = PropertyQueries
                    .<AttributedType>createQuery(attributedType.getClass())
                    .addCriteria(new TypedPropertyCriteria(attributedType.getClass()))
                    .getFirstResult();

            if (parentProperty != null) {
                AttributedType parentType = parentProperty.getValue(attributedType);

                if (parentType != null) {
                    Attributes attributes = this.operationManager.getAttributes(parentType.getId(), getBaseDN(parentType), entryConfig);
                    Attribute attribute = attributes.get(entryConfig.getParentMembershipAttributeName());

                    attribute.add(getBindingDN(attributedType, true));

                    this.operationManager.modifyAttribute(getBindingDN(parentType, true), attribute);
                }
            }
        }
    }

    protected String getEntryIdentifier(final AttributedType attributedType) {
        try {
            // we need this to retrieve the entry's identifier from the ldap server
            List<SearchResult> search = this.operationManager.search(getBaseDN(attributedType), "(" + getBindingDN(attributedType, false) + ")", getMappingConfig(attributedType.getClass()));
            Attribute id = search.get(0).getAttributes().get(getConfig().getUniqueIdentifierAttributeName());

            if (id == null) {
                throw new ModelException("Could not retrieve identifier for entry [" + getBindingDN(attributedType, true) + "].");
            }

            return this.operationManager.decodeEntryUUID(id.get());
        } catch (NamingException ne) {
            throw new ModelException("Could not add type [" + attributedType + "].", ne);
        }
    }
}
