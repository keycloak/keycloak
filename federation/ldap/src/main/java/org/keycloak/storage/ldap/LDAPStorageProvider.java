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

package org.keycloak.storage.ldap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;

import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialAuthentication;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.UserCredentialManager;
import org.keycloak.federation.kerberos.KerberosPrincipal;
import org.keycloak.federation.kerberos.impl.KerberosUsernamePasswordAuthenticator;
import org.keycloak.federation.kerberos.impl.SPNEGOAuthenticator;
import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.UserCache;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.ReadOnlyUserModelDelegate;
import org.keycloak.policy.PasswordPolicyManagerProvider;
import org.keycloak.policy.PolicyError;
import org.keycloak.storage.DatastoreProvider;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.StoreManagers;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.UserStorageUtil;
import org.keycloak.storage.adapter.InMemoryUserAdapter;
import org.keycloak.storage.adapter.UpdateOnlyChangeUserModelDelegate;
import org.keycloak.storage.ldap.idm.model.LDAPDn;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.Condition;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQueryConditionsBuilder;
import org.keycloak.storage.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.storage.ldap.kerberos.LDAPProviderKerberosConfig;
import org.keycloak.storage.ldap.mappers.LDAPMappersComparator;
import org.keycloak.storage.ldap.mappers.LDAPOperationDecorator;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapperManager;
import org.keycloak.storage.ldap.mappers.PasswordUpdateCallback;
import org.keycloak.storage.user.ImportedUserValidation;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryMethodsProvider;
import org.keycloak.storage.user.UserRegistrationProvider;
import org.keycloak.userprofile.AttributeGroupMetadata;
import org.keycloak.userprofile.AttributeMetadata;
import org.keycloak.userprofile.UserProfileDecorator;
import org.keycloak.userprofile.UserProfileMetadata;
import org.keycloak.userprofile.UserProfileUtil;
import org.keycloak.utils.StreamsUtil;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class LDAPStorageProvider implements UserStorageProvider,
        CredentialInputValidator,
        CredentialInputUpdater,
        CredentialAuthentication,
        UserLookupProvider,
        UserRegistrationProvider,
        UserQueryMethodsProvider,
        ImportedUserValidation,
        UserProfileDecorator {
    private static final Logger logger = Logger.getLogger(LDAPStorageProvider.class);
    private static final int DEFAULT_MAX_RESULTS = Integer.MAX_VALUE >> 1;
    public static final List<String> INTERNAL_ATTRIBUTES = List.of(UserModel.LOCALE);

    protected LDAPStorageProviderFactory factory;
    protected KeycloakSession session;
    protected UserStorageProviderModel model;
    protected LDAPIdentityStore ldapIdentityStore;
    protected EditMode editMode;
    protected LDAPProviderKerberosConfig kerberosConfig;
    protected PasswordUpdateCallback updater;
    protected LDAPStorageMapperManager mapperManager;
    protected LDAPStorageUserManager userManager;
    private LDAPMappersComparator ldapMappersComparator;

    // these exist to make sure that we only hit ldap once per transaction
    //protected Map<String, UserModel> noImportSessionCache = new HashMap<>();


    protected final Set<String> supportedCredentialTypes = new HashSet<>();

    public LDAPStorageProvider(LDAPStorageProviderFactory factory, KeycloakSession session, ComponentModel model, LDAPIdentityStore ldapIdentityStore) {
        this.factory = factory;
        this.session = session;
        this.model = new UserStorageProviderModel(model);
        this.ldapIdentityStore = ldapIdentityStore;
        this.kerberosConfig = new LDAPProviderKerberosConfig(model);
        this.editMode = ldapIdentityStore.getConfig().getEditMode();
        this.mapperManager = new LDAPStorageMapperManager(this);
        this.userManager = new LDAPStorageUserManager(this);

        supportedCredentialTypes.add(PasswordCredentialModel.TYPE);
        if (kerberosConfig.isAllowKerberosAuthentication()) {
            supportedCredentialTypes.add(UserCredentialModel.KERBEROS);
        }

        ldapMappersComparator = new LDAPMappersComparator(getLdapIdentityStore().getConfig());
    }

    public void setUpdater(PasswordUpdateCallback updater) {
        this.updater = updater;
    }

    public KeycloakSession getSession() {
        return session;
    }

    public LDAPIdentityStore getLdapIdentityStore() {
        return this.ldapIdentityStore;
    }

    public EditMode getEditMode() {
        return editMode;
    }

    public UserStorageProviderModel getModel() {
        return model;
    }

    public LDAPProviderKerberosConfig getKerberosConfig() {
        return kerberosConfig;
    }

    public LDAPStorageMapperManager getMapperManager() {
        return mapperManager;
    }

    public LDAPStorageUserManager getUserManager() {
        return userManager;
    }


    @Override
    public UserModel validate(RealmModel realm, UserModel local) {
        LDAPObject ldapObject = loadAndValidateUser(realm, local);
        if (ldapObject == null) {
            return null;
        }

        return proxy(realm, local, ldapObject, false);
    }

    protected UserModel proxy(RealmModel realm, UserModel local, LDAPObject ldapObject, boolean newUser) {
        UserModel existing = userManager.getManagedProxiedUser(local.getId());
        if (existing != null) {
            return existing;
        }

        // We need to avoid having CachedUserModel as cache is upper-layer then LDAP. Hence having CachedUserModel here may cause StackOverflowError
        if (local instanceof CachedUserModel) {
            StoreManagers datastoreProvider = (StoreManagers) session.getProvider(DatastoreProvider.class);
            local = datastoreProvider.userStorageManager().getUserById(realm, local.getId());

            existing = userManager.getManagedProxiedUser(local.getId());
            if (existing != null) {
                return existing;
            }
        }

        UserModel proxied = local;

        checkDNChanged(realm, local, ldapObject);

        switch (editMode) {
            case READ_ONLY:
                if (model.isImportEnabled()) {
                    proxied = new ReadonlyLDAPUserModelDelegate(local);
                } else {
                    proxied = new ReadOnlyUserModelDelegate(local);
                }
                break;
            case WRITABLE:
            case UNSYNCED:
                // Any attempt to write data, which are not supported by the LDAP schema, should fail
                // This check is skipped when register new user as there are many "generic" attributes always written (EG. enabled, emailVerified) and those are usually unsupported by LDAP schema
                if (!model.isImportEnabled() && !newUser) {
                    UserModel readOnlyDelegate = new ReadOnlyUserModelDelegate(local, ReadOnlyException::new);
                    proxied = new LDAPWritesOnlyUserModelDelegate(readOnlyDelegate, this);
                }
                break;
        }

        AtomicReference<UserModel> proxy = new AtomicReference<>(proxied);
        realm.getComponentsStream(model.getId(), LDAPStorageMapper.class.getName())
                .sorted(ldapMappersComparator.sortAsc())
                .forEachOrdered(mapperModel -> {
                    LDAPStorageMapper ldapMapper = mapperManager.getMapper(mapperModel);
                    proxy.set(ldapMapper.proxy(ldapObject, proxy.get(), realm));
                });
        proxied = proxy.get();

        if (!model.isImportEnabled()) {
            proxied = new UpdateOnlyChangeUserModelDelegate(proxied);
        }

        userManager.setManagedProxiedUser(proxied, ldapObject);

        return proxied;
    }

    private void checkDNChanged(RealmModel realm, UserModel local, LDAPObject ldapObject) {
        String dnFromDB = local.getFirstAttribute(LDAPConstants.LDAP_ENTRY_DN);
        String ldapDn = ldapObject.getDn() == null? null : ldapObject.getDn().toString();
        if (ldapDn != null && !ldapDn.equals(dnFromDB)) {
            logger.debugf("Updated LDAP DN of user '%s' to '%s'", local.getUsername(), ldapDn);
            local.setSingleAttribute(LDAPConstants.LDAP_ENTRY_DN, ldapDn);

            UserCache userCache = UserStorageUtil.userCache(session);
            if (userCache != null) {
                userCache.evict(realm, local);
            }
        }
    }

    @Override
    public boolean supportsCredentialAuthenticationFor(String type) {
        return type.equals(UserCredentialModel.KERBEROS) && kerberosConfig.isAllowKerberosAuthentication();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        List<LDAPObject> ldapObjects;
        if (LDAPConstants.LDAP_ID.equals(attrName)) {
            // search by UUID attribute
            LDAPObject ldapObject = loadLDAPUserByUuid(realm, attrValue);
            ldapObjects = ldapObject == null? Collections.emptyList() : Collections.singletonList(ldapObject);
        } else if (LDAPConstants.LDAP_ENTRY_DN.equals(attrName)) {
            // search by DN attribute
            LDAPObject ldapObject = loadLDAPUserByDN(realm, LDAPDn.fromString(attrValue));
            ldapObjects = ldapObject == null? Collections.emptyList() : Collections.singletonList(ldapObject);
        } else {
            try (LDAPQuery ldapQuery = LDAPUtils.createQueryForUserSearch(this, realm)) {
                LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();

                Condition attrCondition = conditionsBuilder.equal(attrName, attrValue);
                ldapQuery.addWhereCondition(attrCondition);

                ldapObjects = ldapQuery.getResultList();
            }
        }

        return ldapObjects.stream().map(ldapUser -> {
            String ldapUsername = LDAPUtils.getUsername(ldapUser, this.ldapIdentityStore.getConfig());
            UserModel localUser = UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(realm, ldapUsername);
            if (localUser == null) {
                return importUserFromLDAP(session, realm, ldapUser);
            } else {
                return proxy(realm, localUser, ldapUser, false);
            }
        });
    }

    public boolean synchronizeRegistrations() {
        return "true".equalsIgnoreCase(model.getConfig().getFirst(LDAPConstants.SYNC_REGISTRATIONS)) && editMode == UserStorageProvider.EditMode.WRITABLE;
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        if (!synchronizeRegistrations()) {
            return null;
        }
        final UserModel user;
        if (model.isImportEnabled()) {
            user = UserStoragePrivateUtil.userLocalStorage(session).addUser(realm, username);
            user.setFederationLink(model.getId());
        } else {
            user = new InMemoryUserAdapter(session, realm, new StorageId(model.getId(), username).getId());
            user.setUsername(username);
        }
        LDAPObject ldapUser = LDAPUtils.addUserToLDAP(this, realm, user, ldapObject -> {
            LDAPUtils.checkUuid(ldapObject, ldapIdentityStore.getConfig());
            user.setSingleAttribute(LDAPConstants.LDAP_ID, ldapObject.getUuid());
            user.setSingleAttribute(LDAPConstants.LDAP_ENTRY_DN, ldapObject.getDn().toString());
        });

        // Add the user to the default groups and add default required actions
        UserModel proxy = proxy(realm, user, ldapUser, true);
        proxy.grantRole(realm.getDefaultRole());

        realm.getDefaultGroupsStream().forEach(proxy::joinGroup);

        realm.getRequiredActionProvidersStream()
                .filter(RequiredActionProviderModel::isEnabled)
                .filter(RequiredActionProviderModel::isDefaultAction)
                .map(RequiredActionProviderModel::getAlias)
                .forEachOrdered(proxy::addRequiredAction);

        return proxy;
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        if (editMode == UserStorageProvider.EditMode.READ_ONLY || editMode == UserStorageProvider.EditMode.UNSYNCED) {
            logger.warnf("User '%s' can't be deleted in LDAP as editMode is '%s'. Deleting user just from Keycloak DB, but he will be re-imported from LDAP again once searched in Keycloak", user.getUsername(), editMode.toString());
            return true;
        }

        LDAPObject ldapObject = loadAndValidateUser(realm, user);
        if (ldapObject == null) {
            if (model.isRemoveInvalidUsersEnabled()) {
                logger.warnf("User '%s' can't be deleted from LDAP as it doesn't exist here", user.getUsername());
                return false;
            }
            return true;
        }

        ldapIdentityStore.remove(ldapObject);
        userManager.removeManagedUserEntry(user.getId());

        return true;
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        UserModel alreadyLoadedInSession = userManager.getManagedProxiedUser(id);
        if (alreadyLoadedInSession != null) return alreadyLoadedInSession;

        StorageId storageId = new StorageId(id);
        return getUserByUsername(realm, storageId.getExternalId());
    }

    /**
     * LDAP search supports {@link UserModel#SEARCH}, {@link UserModel#EXACT} and
     * all the other user attributes that are managed by a mapper (method
     * <em>getUserAttributes</em>).
     */
    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        String search = params.get(UserModel.SEARCH);
        Stream<LDAPObject> result = search != null ?
                searchLDAP(realm, search, firstResult, maxResults) :
                searchLDAPByAttributes(realm, params, firstResult, maxResults);

        if (model.isImportEnabled()) {
            result = result.filter(filterLocalUsers(realm));
        }
        return StreamsUtil.paginatedStream(
                // search users but not force import returning null as they were returned before by the DB
                result.map(ldapObject -> importUserFromLDAP(session, realm, ldapObject, ImportType.NOT_FORCED_RETURN_NULL))
                        .filter(Objects::nonNull),
                firstResult, maxResults);
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        int first = firstResult == null ? 0 : firstResult;
        int max = maxResults == null ? DEFAULT_MAX_RESULTS : maxResults;

        return realm.getComponentsStream(model.getId(), LDAPStorageMapper.class.getName())
            .sorted(ldapMappersComparator.sortAsc())
            .map(mapperModel ->
                mapperManager.getMapper(mapperModel).getGroupMembers(realm, group, first, max))
            .filter(((Predicate<List>) List::isEmpty).negate())
            .map(List::stream)
            .findFirst().orElse(Stream.empty());
    }

    @Override
    public Stream<UserModel> getRoleMembersStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
        int first = firstResult == null ? 0 : firstResult;
        int max = maxResults == null ? DEFAULT_MAX_RESULTS : maxResults;

        return realm.getComponentsStream(model.getId(), LDAPStorageMapper.class.getName())
                .sorted(ldapMappersComparator.sortAsc())
                .map(mapperModel -> mapperManager.getMapper(mapperModel).getRoleMembers(realm, role, first, max))
                .filter(((Predicate<List>) List::isEmpty).negate())
                .map(List::stream)
                .findFirst().orElse(Stream.empty());
    }

    public List<UserModel> loadUsersByUsernames(List<String> usernames, RealmModel realm) {
        List<UserModel> result = new ArrayList<>();
        for (String username : usernames) {
            UserModel kcUser = session.users().getUserByUsername(realm, username);
            if (kcUser == null) {
                logger.warnf("User '%s' referenced by membership wasn't found in LDAP", username);
            } else if (model.isImportEnabled() && !model.getId().equals(kcUser.getFederationLink())) {
                logger.warnf("Incorrect federation provider of user '%s'", kcUser.getUsername());
            } else {
                result.add(kcUser);
            }
        }
        return result;
    }

    private Stream<LDAPObject> loadUsersByDNsChunk(RealmModel realm, String rdnAttr, Collection<LDAPDn> dns) {
        try (LDAPQuery ldapQuery = LDAPUtils.createQueryForUserSearch(this, realm)) {
            final LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();
            final Set<LDAPDn> dnSet = new HashSet<>(dns);
            final Condition[] conditions = dns.stream()
                    .map(dn -> conditionsBuilder.equal(rdnAttr, dn.getFirstRdn().getAttrValue(rdnAttr)))
                    .toArray(Condition[]::new);
            ldapQuery.addWhereCondition(conditionsBuilder.orCondition(conditions));
            return ldapQuery.getResultList().stream().filter(ldapUser -> dnSet.contains(ldapUser.getDn()));
        }
    }

    public Stream<UserModel> loadUsersByDNs(RealmModel realm, Collection<LDAPDn> dns, int firstResult, int maxResults) {
        final String rdnAttr = ldapIdentityStore.getConfig().getRdnLdapAttribute();
        final LDAPDn usersDn = LDAPDn.fromString(ldapIdentityStore.getConfig().getUsersDn());
        final int chunkSize = ldapIdentityStore.getConfig().getMaxConditions();
        return StreamsUtil.chunkedStream(
                        dns.stream().filter(dn -> dn.getFirstRdn().getAttrValue(rdnAttr) != null && dn.isDescendantOf(usersDn)),
                        chunkSize)
                .map(chunk -> loadUsersByDNsChunk(realm, rdnAttr, chunk))
                .flatMap(Function.identity())
                .skip(firstResult)
                .limit(maxResults)
                // do no force the import and return the current existing user if available
                .map(ldapUser -> importUserFromLDAP(session, realm, ldapUser, ImportType.NOT_FORCED_RETURN_EXISTING));
    }

    private Stream<LDAPObject> loadUsersByUniqueAttributeChunk(RealmModel realm, String uidName, Collection<String> uids) {
        try (LDAPQuery ldapQuery = LDAPUtils.createQueryForUserSearch(this, realm)) {
            LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();
            Condition[] conditions = uids.stream()
                    .map(uid -> conditionsBuilder.equal(uidName, uid))
                    .toArray(Condition[]::new);
            ldapQuery.addWhereCondition(conditionsBuilder.orCondition(conditions));
            return ldapQuery.getResultList().stream();
        }
    }

    public Stream<UserModel> loadUsersByUniqueAttribute(RealmModel realm, String uidName, Collection<String> uids, int firstResult, int maxResults) {
        final int chunkSize = ldapIdentityStore.getConfig().getMaxConditions();
        return StreamsUtil.chunkedStream(uids.stream(), chunkSize)
                .map(chunk -> loadUsersByUniqueAttributeChunk(realm, uidName, chunk))
                .flatMap(Function.identity())
                .skip(firstResult)
                .limit(maxResults)
                // do no force the import and return the current existing user if available
                .map(ldapUser -> importUserFromLDAP(session, realm, ldapUser, ImportType.NOT_FORCED_RETURN_EXISTING));
    }

    private Condition createSearchCondition(LDAPQueryConditionsBuilder conditionsBuilder, String name, boolean equals, String value) {
        if (equals) {
            return conditionsBuilder.equal(name, value);
        }

        // perform a substring search based on *
        String[] values = value.split("\\Q*\\E+", -1); // split by *
        String start = null, end = null;
        String[] middle = null;
        if (!values[0].isEmpty()) {
            start = values[0];
        }
        if (values.length > 1 && !values[values.length -1].isEmpty()) {
            end = values[values.length - 1];
        }
        if (values.length > 2) {
            middle = Arrays.copyOfRange(values, 1, values.length - 1);
        }

        if (start == null && middle == null && end == null) {
            // just searching using empty string or *
            return conditionsBuilder.present(name);
        }

        // return proper substring search
        return conditionsBuilder.substring(name, start, middle, end);
    }

    /**
     * Searches LDAP using logical conjunction of params. It uses the LDAP mappers
     * (method <em>getUserAttributes</em>) to control what attributes are
     * managed by the ldap server. If one attribute is not defined by the
     * mappers then empty stream is returned (the attribute is not mapped
     * into ldap, therefore no ldap user can have the specified value).
     */
    private Stream<LDAPObject> searchLDAPByAttributes(RealmModel realm, Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        // get the attributes that are managed by the configured ldap mappers
        Set<String> managedAttrs = realm.getComponentsStream(model.getId(), LDAPStorageMapper.class.getName())
                .map(mapperManager::getMapper)
                .map(LDAPStorageMapper::getUserAttributes)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        final boolean exact = Boolean.parseBoolean(attributes.get(UserModel.EXACT));
        try (LDAPQuery ldapQuery = LDAPUtils.createQueryForUserSearch(this, realm)) {

            LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();

            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                String attrName = entry.getKey();
                if (LDAPConstants.LDAP_ID.equals(attrName)) {
                    String uuidLDAPAttributeName = this.ldapIdentityStore.getConfig().getUuidLDAPAttributeName();
                    Condition usernameCondition = conditionsBuilder.equal(uuidLDAPAttributeName, entry.getValue());
                    ldapQuery.addWhereCondition(usernameCondition);
                } else if (LDAPConstants.LDAP_ENTRY_DN.equals(attrName)) {
                    ldapQuery.setSearchDn(entry.getValue());
                    ldapQuery.setSearchScope(SearchControls.OBJECT_SCOPE);
                } else if (managedAttrs.contains(attrName)) {
                    // we can search any attribute that is mapped to a user attribute
                    switch (attrName) {
                        case UserModel.USERNAME:
                        case UserModel.EMAIL:
                        case UserModel.FIRST_NAME:
                        case UserModel.LAST_NAME:
                            if (exact) {
                                ldapQuery.addWhereCondition(conditionsBuilder.equal(attrName, entry.getValue()));
                            } else {
                                // doing a *value* search
                                ldapQuery.addWhereCondition(conditionsBuilder.substring(attrName, null, new String[]{entry.getValue()}, null));
                            }
                            break;
                        default:
                            // custom attributes are only equals
                            ldapQuery.addWhereCondition(conditionsBuilder.equal(attrName, entry.getValue()));
                            break;
                    }
                } else if (!attrName.equals(UserModel.EXACT)
                        && !attrName.equals(UserModel.INCLUDE_SERVICE_ACCOUNT)
                        && !(UserModel.ENABLED.equals(attrName) && Boolean.parseBoolean(entry.getValue()))) {
                    // if the attr is not mapped just return empty stream
                    // skip special names and enabled if looking for true (enabled is not mapped so it's always true)
                    logger.debugf("Searching in LDAP using unmapped attribute [%s], returning empty stream", attrName);
                    return Stream.empty();
                }
            }
            return paginatedSearchLDAP(ldapQuery, firstResult, maxResults);
        }
    }

    /**
     * Searches LDAP using logical disjunction of params. It supports
     * <ul>
     *     <li>{@link UserModel#FIRST_NAME}</li>
     *     <li>{@link UserModel#LAST_NAME}</li>
     *     <li>{@link UserModel#EMAIL}</li>
     *     <li>{@link UserModel#USERNAME}</li>
     * </ul>
     *
     * It uses multiple LDAP calls and results are combined together with respect to firstResult and maxResults
     *
     * This method serves for {@code search} param of {@link org.keycloak.services.resources.admin.UsersResource#getUsers}
     */
    private Stream<LDAPObject> searchLDAP(RealmModel realm, String search, Integer firstResult, Integer maxResults) {

        try (LDAPQuery ldapQuery = LDAPUtils.createQueryForUserSearch(this, realm)) {
            LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();

            for (String s : search.split("\\s+")) {
                boolean equals = false;
                List<Condition> conditions = new LinkedList<>();
                if (s.startsWith("\"") && s.endsWith("\"")) {
                    // exact search
                    s = s.substring(1, s.length() - 1);
                    equals = true;
                } else if (!s.endsWith("*")) {
                    // default to prefix search
                    s += "*";
                }

                conditions.add(createSearchCondition(conditionsBuilder, UserModel.USERNAME, equals, s));
                conditions.add(createSearchCondition(conditionsBuilder, UserModel.EMAIL, equals, s));
                conditions.add(createSearchCondition(conditionsBuilder, UserModel.FIRST_NAME, equals, s));
                conditions.add(createSearchCondition(conditionsBuilder, UserModel.LAST_NAME, equals, s));

                ldapQuery.addWhereCondition(conditionsBuilder.orCondition(conditions.toArray(Condition[]::new)));
            }

            return paginatedSearchLDAP(ldapQuery, firstResult, maxResults);
        }
    }

    /**
     * @param local
     * @return ldapUser corresponding to local user or null if user is no longer in LDAP
     */
    protected LDAPObject loadAndValidateUser(RealmModel realm, UserModel local) {
        // getFirstAttribute triggers validation and another call to this method, so we run it before checking the cache
        String uuidLdapAttribute = local.getFirstAttribute(LDAPConstants.LDAP_ID);

        LDAPObject existing = userManager.getManagedLDAPObject(local.getId());
        if (existing != null) {
            return existing;
        }

        LDAPObject ldapUser = loadLDAPUserByUuid(realm, uuidLdapAttribute);
        if(ldapUser == null){
            return null;
        }
        userManager.setManagedLDAPObject(local.getId(), ldapUser);
        LDAPUtils.checkUuid(ldapUser, ldapIdentityStore.getConfig());

        if (ldapUser.getUuid().equals(local.getFirstAttribute(LDAPConstants.LDAP_ID))) {
            return ldapUser;
        } else {
            logger.warnf("LDAP User invalid. ID doesn't match. ID from LDAP [%s], LDAP ID from local DB: [%s]", ldapUser.getUuid(), local.getFirstAttribute(LDAPConstants.LDAP_ID));
            return null;
        }
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        LDAPObject ldapUser = loadLDAPUserByUsername(realm, username);
        if (ldapUser == null) {
            return null;
        }

        return importUserFromLDAP(session, realm, ldapUser);
    }

    protected UserModel importUserFromLDAP(KeycloakSession session, RealmModel realm, LDAPObject ldapUser) {
        return importUserFromLDAP(session, realm, ldapUser, ImportType.FORCED);
    }

    private void doImportUser(final RealmModel realm, final UserModel user, final LDAPObject ldapUser) {
        user.setEnabled(true);

        importUserAttributes(realm, user, ldapUser);

        String userDN = ldapUser.getDn().toString();
        if (model.isImportEnabled()) user.setFederationLink(model.getId());
        user.setSingleAttribute(LDAPConstants.LDAP_ID, ldapUser.getUuid());
        user.setSingleAttribute(LDAPConstants.LDAP_ENTRY_DN, userDN);
        if(getLdapIdentityStore().getConfig().isTrustEmail()){
            user.setEmailVerified(true);
        }
        if (kerberosConfig.isAllowKerberosAuthentication() && kerberosConfig.getKerberosPrincipalAttribute() != null) {
            String kerberosPrincipal = ldapUser.getAttributeAsString(kerberosConfig.getKerberosPrincipalAttribute());
            if (kerberosPrincipal == null) {
                logger.warnf("Kerberos principal attribute not found on LDAP user [%s]. Configured kerberos principal attribute name is [%s]", ldapUser.getDn(), kerberosConfig.getKerberosPrincipalAttribute());
            } else {
                KerberosPrincipal kerberosPrinc = new KerberosPrincipal(kerberosPrincipal);
                user.setSingleAttribute(KerberosConstants.KERBEROS_PRINCIPAL, kerberosPrinc.toString());
            }
        }
        logger.debugf("Imported new user from LDAP to Keycloak DB. Username: [%s], Email: [%s], LDAP_ID: [%s], LDAP Entry DN: [%s]",
                user.getUsername(), user.getEmail(), ldapUser.getUuid(), userDN);
    }

    protected enum ImportType {
        FORCED, // the import is forced
        NOT_FORCED_RETURN_NULL, // the import is not forced and null is returned when a previous user exists
        NOT_FORCED_RETURN_EXISTING  // the import is not forced and existing user is returned
    };

    protected UserModel importUserFromLDAP(KeycloakSession session, RealmModel realm, LDAPObject ldapUser, ImportType importType) {
        String ldapUsername = LDAPUtils.getUsername(ldapUser, ldapIdentityStore.getConfig());
        LDAPUtils.checkUuid(ldapUser, ldapIdentityStore.getConfig());
        if (importType == null) {
            importType = ImportType.FORCED;
        }

        UserModel imported = null;
        UserModel existingLocalUser = null;
        final UserProvider userProvider = UserStoragePrivateUtil.userLocalStorage(session);
        try {
            if (model.isImportEnabled()) {
                // Search if there is already an existing user, which means the username might have changed in LDAP without Keycloak knowing about it
                existingLocalUser = userProvider.searchForUserByUserAttributeStream(realm, LDAPConstants.LDAP_ID, ldapUser.getUuid())
                        .findFirst().orElse(null);
                if (existingLocalUser != null) {
                    imported = existingLocalUser;
                    if (importType == ImportType.NOT_FORCED_RETURN_NULL) {
                        // import not forced and return null
                        return null;
                    } else if (importType == ImportType.NOT_FORCED_RETURN_EXISTING) {
                        // import not forced and return the current DB user
                        return proxy(realm, imported, ldapUser, false);
                    }
                    // import is forced, need to evict the existing user from cache
                    if (UserStorageUtil.userCache(session) != null) {
                        UserStorageUtil.userCache(session).evict(realm, existingLocalUser);
                    }
                } else {
                    imported = userProvider.addUser(realm, ldapUsername);
                }
            } else {
                InMemoryUserAdapter adapter = new InMemoryUserAdapter(session, realm, new StorageId(model.getId(), ldapUsername).getId());
                adapter.addDefaults();
                imported = adapter;
            }
            doImportUser(realm, imported, ldapUser);
        } catch (ModelDuplicateException e) {
            logger.warnf(e, "Duplicated user importing from LDAP. LDAP Entry DN: [%s], LDAP_ID: [%s]", ldapUser.getDn(), ldapUser.getUuid());
            if (importType != ImportType.FORCED && existingLocalUser == null) {
                // try to continue if import was not forced, delete created db user if necessary
                if (model.isImportEnabled() && imported != null) {
                    userProvider.removeUser(realm, imported);
                }
                return null;
            }
            throw e;
        }

        UserModel proxy = proxy(realm, imported, ldapUser, false);
        return proxy;
    }

    protected LDAPObject queryByEmail(RealmModel realm, String email) {
        try (LDAPQuery ldapQuery = LDAPUtils.createQueryForUserSearch(this, realm)) {
            LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();

            // Mapper should replace "email" in parameter name with correct LDAP mapped attribute
            Condition emailCondition = conditionsBuilder.equal(UserModel.EMAIL, email);
            ldapQuery.addWhereCondition(emailCondition);

            return ldapQuery.getFirstResult();
        }
    }


    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        LDAPObject ldapUser = queryByEmail(realm, email);
        if (ldapUser == null) {
            return null;
        }

        // Check here if user already exists
        String ldapUsername = LDAPUtils.getUsername(ldapUser, ldapIdentityStore.getConfig());
        UserModel user = UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(realm, ldapUsername);

        if (user != null) {
            LDAPUtils.checkUuid(ldapUser, ldapIdentityStore.getConfig());
            // If email attribute mapper is set to "Always Read Value From LDAP" the user may be in Keycloak DB with an old email address
            if (ldapUser.getUuid().equals(user.getFirstAttribute(LDAPConstants.LDAP_ID))) {
                importUserAttributes(realm, user, ldapUser);
                return proxy(realm, user, ldapUser, false);
            }
            throw new ModelDuplicateException("User with username '" + ldapUsername + "' already exists in Keycloak. It conflicts with LDAP user with email '" + email + "'");
        }

        return importUserFromLDAP(session, realm, ldapUser);
    }

    @Override
    public void preRemove(RealmModel realm) {
        // complete Don't think we have to do anything
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        // TODO: Maybe mappers callback to ensure role deletion propagated to LDAP by RoleLDAPFederationMapper?
    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {

    }

    public boolean validPassword(RealmModel realm, UserModel user, String password) {
        if (kerberosConfig.isAllowKerberosAuthentication() && kerberosConfig.isUseKerberosForPasswordAuthentication()) {
            // Use Kerberos JAAS (Krb5LoginModule)
            KerberosUsernamePasswordAuthenticator authenticator = factory.createKerberosUsernamePasswordAuthenticator(kerberosConfig);
            String kerberosUsername = user.getFirstAttribute(KerberosConstants.KERBEROS_PRINCIPAL);
            // Fallback to username (backwards compatibility)
            if (kerberosUsername == null) kerberosUsername = user.getUsername();

            return authenticator.validUser(kerberosUsername, password);
        } else {
            // Use Naming LDAP API
            LDAPObject ldapUser = loadAndValidateUser(realm, user);
            if (ldapUser == null) {
                // user was removed from ldap - password verification must fail.
                return false;
            }

            try {
                ldapIdentityStore.validatePassword(ldapUser, password);
                return true;
            } catch (AuthenticationException ae) {
                AtomicReference<Boolean> processed = new AtomicReference<>(false);
                realm.getComponentsStream(model.getId(), LDAPStorageMapper.class.getName())
                        .sorted(ldapMappersComparator.sortDesc())
                        .forEachOrdered(mapperModel -> {
                            if (logger.isTraceEnabled()) {
                                logger.tracef("Using mapper %s during import user from LDAP", mapperModel);
                            }
                            LDAPStorageMapper ldapMapper = mapperManager.getMapper(mapperModel);
                            processed.set(processed.get() || ldapMapper.onAuthenticationFailure(ldapUser, user, ae, realm));
                        });
                return processed.get();
            }
        }
    }


    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!PasswordCredentialModel.TYPE.equals(input.getType()) || ! (input instanceof UserCredentialModel)) return false;
        if (editMode == UserStorageProvider.EditMode.READ_ONLY) {
            throw new ReadOnlyException("Federated storage is not writable");

        } else if (editMode == UserStorageProvider.EditMode.WRITABLE) {
            LDAPIdentityStore ldapIdentityStore = getLdapIdentityStore();
            String password = input.getChallengeResponse();
            LDAPObject ldapUser = loadAndValidateUser(realm, user);
            if (ldapUser == null) {
                logger.warnf("User '%s' can't be updated in LDAP as it doesn't exist there", user.getUsername());
                return false;
            }
            if (ldapIdentityStore.getConfig().isValidatePasswordPolicy()) {
                PolicyError error = session.getProvider(PasswordPolicyManagerProvider.class).validate(realm, user, password);
                if (error != null) throw new ModelException(error.getMessage(), error.getParameters());
            }
            try {
                LDAPOperationDecorator operationDecorator = null;
                if (updater != null) {
                    operationDecorator = updater.beforePasswordUpdate(user, ldapUser, (UserCredentialModel)input);
                }

                ldapIdentityStore.updatePassword(ldapUser, password, operationDecorator);

                if (updater != null) updater.passwordUpdated(user, ldapUser, (UserCredentialModel)input);
                return true;
            } catch (ModelException me) {
                if (updater != null) {
                    updater.passwordUpdateFailed(user, ldapUser, (UserCredentialModel)input, me);
                    return false;
                } else {
                    throw me;
                }
            }

        } else {
            return false;
        }
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {

    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
        return Stream.empty();
    }

    public Set<String> getSupportedCredentialTypes() {
        return new HashSet<>(this.supportedCredentialTypes);
    }


    @Override
    public boolean supportsCredentialType(String credentialType) {
        return getSupportedCredentialTypes().contains(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return getSupportedCredentialTypes().contains(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!(input instanceof UserCredentialModel)) return false;
        if (input.getType().equals(PasswordCredentialModel.TYPE) && !((UserCredentialManager) user.credentialManager()).isConfiguredLocally(PasswordCredentialModel.TYPE)) {
            return validPassword(realm, user, input.getChallengeResponse());
        } else {
            return false; // invalid cred type
        }
    }

    @Override
    public CredentialValidationOutput authenticate(RealmModel realm, CredentialInput cred) {
        if (!(cred instanceof UserCredentialModel)) return CredentialValidationOutput.fallback();
        UserCredentialModel credential = (UserCredentialModel)cred;
        if (credential.getType().equals(UserCredentialModel.KERBEROS)) {
            if (kerberosConfig.isAllowKerberosAuthentication()) {
                SPNEGOAuthenticator spnegoAuthenticator = (SPNEGOAuthenticator) credential.getNote(KerberosConstants.AUTHENTICATED_SPNEGO_CONTEXT);
                if (spnegoAuthenticator != null) {
                    logger.debugf("SPNEGO authentication already performed by previous provider. Provider '%s' will try to lookup user with principal kerberos principal '%s'", this, spnegoAuthenticator.getAuthenticatedKerberosPrincipal());
                } else {
                    String spnegoToken = credential.getChallengeResponse();
                    spnegoAuthenticator = factory.createSPNEGOAuthenticator(spnegoToken, kerberosConfig);

                    spnegoAuthenticator.authenticate();
                }

                Map<String, String> state = new HashMap<>();
                if (spnegoAuthenticator.isAuthenticated()) {
                    KerberosPrincipal kerberosPrincipal = spnegoAuthenticator.getAuthenticatedKerberosPrincipal();
                    UserModel user = findOrCreateAuthenticatedUser(realm, kerberosPrincipal);

                    if (user == null) {
                        logger.debugf("Kerberos/SPNEGO authentication succeeded with kerberos principal [%s], but couldn't find or create user with federation provider [%s]", kerberosPrincipal.toString(), model.getName());

                        // Adding the authenticated SPNEGO, in case that other LDAP/Kerberos providers in the chain are able to lookup user from their LDAP
                        // This can be the case with more complex setup (like MSAD Forest Trust environment)
                        // Note that SPNEGO authentication cannot be done again by the other provider due the Kerberos replay protection
                        credential.setNote(KerberosConstants.AUTHENTICATED_SPNEGO_CONTEXT, spnegoAuthenticator);
                        return CredentialValidationOutput.fallback();
                    } else {
                        String delegationCredential = spnegoAuthenticator.getSerializedDelegationCredential();
                        if (delegationCredential != null) {
                            state.put(KerberosConstants.GSS_DELEGATION_CREDENTIAL, delegationCredential);
                        }

                        return new CredentialValidationOutput(user, CredentialValidationOutput.Status.AUTHENTICATED, state);
                    }
                }  else if (spnegoAuthenticator.getResponseToken() != null) {
                    // Case when SPNEGO handshake requires multiple steps
                    logger.tracef("SPNEGO Handshake will continue");
                    state.put(KerberosConstants.RESPONSE_TOKEN, spnegoAuthenticator.getResponseToken());
                    return new CredentialValidationOutput(null, CredentialValidationOutput.Status.CONTINUE, state);
                } else {
                    logger.tracef("SPNEGO Handshake not successful");
                    return CredentialValidationOutput.fallback();
                }
            }
        }

        return CredentialValidationOutput.fallback();
    }

    @Override
    public void close() {
    }

    /**
     * Called after successful kerberos authentication
     *
     * @param realm realm
     * @param kerberosPrincipal kerberos principal of the authenticated user
     * @return found or newly created user
     */
    protected UserModel findOrCreateAuthenticatedUser(RealmModel realm, KerberosPrincipal kerberosPrincipal) {
        String kerberosPrincipalAttrName = kerberosConfig.getKerberosPrincipalAttribute();
        UserModel user;
        if (kerberosPrincipalAttrName != null) {
            logger.tracef("Trying to find user with kerberos principal [%s] in local storage.", kerberosPrincipal.toString());
            user = UserStoragePrivateUtil.userLocalStorage(session).searchForUserByUserAttributeStream(realm, KerberosConstants.KERBEROS_PRINCIPAL, kerberosPrincipal.toString())
                    .findFirst().orElse(null);
        } else {
            // For this case, assuming that for kerberos principal "john@KEYCLOAK.ORG", the username would be "john" (backwards compatibility)
            logger.tracef("Trying to find user in local storage based on username [%s]. Full kerberos principal [%s]", kerberosPrincipal.getPrefix(), kerberosPrincipal);
            user = UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(realm, kerberosPrincipal.getPrefix());
        }

        if (user != null) {
            logger.debugf("Kerberos authenticated user [%s] found in Keycloak storage", user.getUsername());
            if (!model.getId().equals(user.getFederationLink())) {
                logger.warnf("User with username [%s] already exists, but is not linked to provider [%s]. Kerberos principal is [%s]", user.getUsername(), model.getName(), kerberosPrincipal);
                return null;
            } else {
                LDAPObject ldapObject = loadAndValidateUser(realm, user);
                if (kerberosPrincipalAttrName != null && ldapObject != null &&
                        !kerberosPrincipal.toString().equalsIgnoreCase(ldapObject.getAttributeAsString(kerberosPrincipalAttrName))) {
                    logger.warnf("User with username [%s] aready exists and is linked to provider [%s] but is not valid. Authenticated kerberos principal is [%s], but LDAP user has different kerberos principal [%s]",
                            user.getUsername(),  model.getName(), kerberosPrincipal, ldapObject.getAttributeAsString(kerberosPrincipalAttrName));
                    ldapObject = null;
                }

                if (ldapObject != null) {
                    return proxy(realm, user, ldapObject, false);
                } else {
                    logger.warnf("User with username [%s] aready exists and is linked to provider [%s] but is not valid. Stale LDAP_ID on local user is: %s",
                            user.getUsername(),  model.getName(), user.getFirstAttribute(LDAPConstants.LDAP_ID));
                    logger.warn("Will re-create user");
                    UserCache userCache = UserStorageUtil.userCache(session);
                    if (userCache != null) {
                        userCache.evict(realm, user);
                    }
                    new UserManager(session).removeUser(realm, user, UserStoragePrivateUtil.userLocalStorage(session));
                }
            }
        }

        if (kerberosPrincipalAttrName != null) {
            logger.debugf("Trying to find kerberos authenticated user [%s] in LDAP. Kerberos principal attribute is [%s]", kerberosPrincipal.toString(), kerberosPrincipalAttrName);
            try (LDAPQuery ldapQuery = LDAPUtils.createQueryForUserSearch(this, realm)) {
                LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();
                Condition krbPrincipalCondition = conditionsBuilder.equal(kerberosPrincipalAttrName, kerberosPrincipal.toString());
                ldapQuery.addWhereCondition(krbPrincipalCondition);
                LDAPObject ldapUser = ldapQuery.getFirstResult();

                if (ldapUser == null) {
                    logger.warnf("Not found LDAP user with kerberos principal [%s]. Kerberos principal attribute is [%s].", kerberosPrincipal.toString(), kerberosPrincipalAttrName);
                    return null;
                }

                return importUserFromLDAP(session, realm, ldapUser);
            }
        } else {
            // Creating user to local storage
            logger.debugf("Kerberos authenticated user [%s] not in Keycloak storage. Creating him", kerberosPrincipal.toString());
            return getUserByUsername(realm, kerberosPrincipal.getPrefix());
        }
    }

    public LDAPObject loadLDAPUserByUsername(RealmModel realm, String username) {
        try (LDAPQuery ldapQuery = LDAPUtils.createQueryForUserSearch(this, realm)) {
            LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();

            String usernameMappedAttribute = this.ldapIdentityStore.getConfig().getUsernameLdapAttribute();
            Condition usernameCondition = conditionsBuilder.equal(usernameMappedAttribute, username);
            ldapQuery.addWhereCondition(usernameCondition);

            LDAPObject ldapUser = ldapQuery.getFirstResult();
            if (ldapUser == null) {
                return null;
            }

            return ldapUser;
        }
    }

    public LDAPObject loadLDAPUserByUuid(RealmModel realm, String uuid) {
        if(uuid == null){
            return null;
        }
        try (LDAPQuery ldapQuery = LDAPUtils.createQueryForUserSearch(this, realm)) {
            LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();

            String uuidLDAPAttributeName = this.ldapIdentityStore.getConfig().getUuidLDAPAttributeName();
            Condition usernameCondition = conditionsBuilder.equal(uuidLDAPAttributeName, uuid);
            ldapQuery.addWhereCondition(usernameCondition);

            return ldapQuery.getFirstResult();
        }
    }

    public LDAPObject loadLDAPUserByDN(RealmModel realm, LDAPDn dn) {
        if (dn == null || !dn.isDescendantOf(LDAPDn.fromString(ldapIdentityStore.getConfig().getUsersDn()))) {
            // no need to search
            return null;
        }
        try (LDAPQuery ldapQuery = LDAPUtils.createQueryForUserSearch(this, realm)) {
            ldapQuery.setSearchDn(dn.getLdapName());
            ldapQuery.setSearchScope(SearchControls.OBJECT_SCOPE);
            return ldapQuery.getFirstResult();
        }
    }

    private Predicate<LDAPObject> filterLocalUsers(RealmModel realm) {
        return ldapObject -> UserStoragePrivateUtil.userLocalStorage(session).getUserByUsername(realm, LDAPUtils.getUsername(ldapObject, LDAPStorageProvider.this.ldapIdentityStore.getConfig())) == null;
    }

    /**
     * This method leverages existing pagination support in {@link LDAPQuery#getResultList()}. It sets the limit for the query
     * based on {@code firstResult}, {@code maxResults} and {@link LDAPConfig#getBatchSizeForSync()}.
     *
     * <p/>
     * Internally it uses {@link Stream#iterate(java.lang.Object, java.util.function.Predicate, java.util.function.UnaryOperator)}
     * to ensure there will be obtained required number of users considering a fact that some of the returned ldap users could be
     * filtered out (as they might be already imported in local storage). The returned {@code Stream<LDAPObject>} will be filled
     * "on demand".
     */
    private Stream<LDAPObject> paginatedSearchLDAP(LDAPQuery ldapQuery, Integer firstResult, Integer maxResults) {
        LDAPConfig ldapConfig = ldapQuery.getLdapProvider().getLdapIdentityStore().getConfig();

        if (ldapConfig.isPagination()) {

            final int limit;
            if (maxResults != null && maxResults >= 0) {
                if (firstResult != null && firstResult > 0) {
                    limit = Integer.min(ldapConfig.getBatchSizeForSync(), Integer.sum(firstResult, maxResults));
                } else {
                    limit = Integer.min(ldapConfig.getBatchSizeForSync(), maxResults);
                }
            } else {
                if (firstResult != null && firstResult > 0) {
                    limit = Integer.min(ldapConfig.getBatchSizeForSync(), firstResult);
                } else {
                    limit = ldapConfig.getBatchSizeForSync();
                }
            }

            return Stream.iterate(ldapQuery,
                    query -> {
                        //the very 1st page - Pagination context might not yet be present
                        if (query.getPaginationContext() == null) {
                            try {
                                query.initPagination();
                                //returning true for first iteration as the LDAP was not queried yet
                                return true;
                            } catch (NameNotFoundException | CommunicationException e) {
                                logger.errorf(e, "Failed to init LDAP query pagination %s", query);
                                return false;
                            } catch (NamingException e) {
                                throw new ModelException("Querying of LDAP failed " + query, e);
                            }
                        }
                        return query.getPaginationContext().hasNextPage();
                    },
                    query -> query
            ).flatMap(query -> {
                        query.setLimit(limit);
                        List<LDAPObject> ldapObjects;

                        try {
                            ldapObjects = query.getResultList();
                        } catch (ModelException mde) {
                            if (mde.isCausedBy(NameNotFoundException.class, CommunicationException.class)) {
                                logger.errorf(mde, "Failed to query LDAP %s", query);
                                return Stream.empty();
                            } else {
                                throw mde;
                            }
                        }

                        if (ldapObjects.isEmpty()) {
                            return Stream.empty();
                        }
                        return ldapObjects.stream();
                    });
        }

        return ldapQuery.getResultList().stream();
    }

    @Override
    public String toString() {
        return "LDAPStorageProvider - " + getModel().getName();
    }

    @Override
    public List<AttributeMetadata> decorateUserProfile(String providerId, UserProfileMetadata metadata) {
        int guiOrder = (int) metadata.getAttributes().stream()
                .map(AttributeMetadata::getName)
                .distinct()
                .count();
        RealmModel realm = session.getContext().getRealm();
        // 1 - get configured attributes from LDAP mappers and add them to the user profile (if they not already present)
        List<String> attributes = realm.getComponentsStream(model.getId(), LDAPStorageMapper.class.getName())
                .sorted(ldapMappersComparator.sortAsc())
                .flatMap(mapperModel -> {
                    LDAPStorageMapper ldapMapper = mapperManager.getMapper(mapperModel);
                    return ldapMapper.getUserAttributes().stream();
                }).toList();

        List<AttributeMetadata> metadatas = new ArrayList<>();

        for (String attrName : attributes) {
            AttributeMetadata attributeMetadata = UserProfileUtil.createAttributeMetadata(attrName, metadata, guiOrder++, getModel().getName());

            if (attributeMetadata != null) {
                metadatas.add(attributeMetadata);
            }
        }

        // 2 - metadata attributes
        Set<String> metadataAttributes = new HashSet<>(List.of(LDAPConstants.LDAP_ID, LDAPConstants.LDAP_ENTRY_DN));
        if (getKerberosConfig().isAllowKerberosAuthentication()) {
            metadataAttributes.add(KerberosConstants.KERBEROS_PRINCIPAL);
        }

        AttributeGroupMetadata metadataGroup = UserProfileUtil.lookupUserMetadataGroup(session);

        for (String attrName : metadataAttributes) {
            AttributeMetadata attributeAdded = UserProfileUtil.createAttributeMetadata(attrName, metadata, metadataGroup, guiOrder++, getModel().getName());
            if (attributeAdded == null) {
                guiOrder--;
            } else {
                metadatas.add(attributeAdded);
            }
        }

        // 3 - make all attributes read-only for LDAP users in case that LDAP itself is read-only
        if (getEditMode() == EditMode.READ_ONLY) {
            Stream.concat(metadata.getAttributes().stream(), metadatas.stream())
                    .filter((m) -> !INTERNAL_ATTRIBUTES.contains(m.getName()))
                    .forEach(attrMetadata -> attrMetadata.addWriteCondition(AttributeMetadata.ALWAYS_FALSE));
        }

        return metadatas;
    }

    @Override
    public Stream<CredentialModel> getCredentials(RealmModel realm, UserModel user) {
        LDAPObject ldapObject = loadLDAPUserByUuid(realm, user.getFirstAttribute(LDAPConstants.LDAP_ID));

        if (ldapObject == null) {
            LDAPConfig config = getLdapIdentityStore().getConfig();
            throw new IllegalStateException("LDAP object not found for user with attribute [" + config.getUuidLDAPAttributeName() + "] and value [" + user.getFirstAttribute(LDAPConstants.LDAP_ID) + "]");
        }

        CredentialModel credential = new CredentialModel();

        credential.setType(PasswordCredentialModel.TYPE);
        credential.setFederationLink(model.getId());
        credential.setCreatedDate(getPasswordChangedTime(ldapObject));

        return Stream.of(credential);
    }

    private long getPasswordChangedTime(LDAPObject ldapObject) {
        String attributeName = getLdapIdentityStore().getPasswordModificationTimeAttributeName();
        String value = ldapObject.getAttributeAsString(attributeName);

        if (StringUtil.isBlank(value)) {
            return -1L;
        }

        if (LDAPConstants.PWD_LAST_SET.equals(attributeName)) {
            return (Long.parseLong(value) / 10000L) - 11644473600000L;
        }
        return LDAPUtils.generalizedTimeToDate(value).getTime();
    }

    private void importUserAttributes(RealmModel realm, UserModel user, LDAPObject ldapUser) {
        realm.getComponentsStream(model.getId(), LDAPStorageMapper.class.getName())
                .sorted(ldapMappersComparator.sortDesc())
                .forEachOrdered(mapperModel -> {
                    if (logger.isTraceEnabled()) {
                        logger.tracef("Using mapper %s during import user from LDAP", mapperModel);
                    }
                    LDAPStorageMapper ldapMapper = mapperManager.getMapper(mapperModel);
                    ldapMapper.onImportUserFromLDAP(ldapUser, user, realm, true);
                });
    }
}
