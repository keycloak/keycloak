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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.AuthenticationException;

import org.jboss.logging.Logger;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialAuthentication;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.federation.kerberos.impl.KerberosUsernamePasswordAuthenticator;
import org.keycloak.federation.kerberos.impl.SPNEGOAuthenticator;
import org.keycloak.models.*;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.DefaultRoles;
import org.keycloak.models.utils.ReadOnlyUserModelDelegate;
import org.keycloak.policy.PasswordPolicyManagerProvider;
import org.keycloak.policy.PolicyError;
import org.keycloak.models.cache.UserCache;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.adapter.InMemoryUserAdapter;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.Condition;
import org.keycloak.storage.ldap.idm.query.EscapeStrategy;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQueryConditionsBuilder;
import org.keycloak.storage.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.storage.ldap.kerberos.LDAPProviderKerberosConfig;
import org.keycloak.storage.ldap.mappers.LDAPOperationDecorator;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapperManager;
import org.keycloak.storage.ldap.mappers.PasswordUpdateCallback;
import org.keycloak.storage.user.ImportedUserValidation;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

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
        UserQueryProvider,
        ImportedUserValidation {
    private static final Logger logger = Logger.getLogger(LDAPStorageProvider.class);

    protected LDAPStorageProviderFactory factory;
    protected KeycloakSession session;
    protected UserStorageProviderModel model;
    protected LDAPIdentityStore ldapIdentityStore;
    protected EditMode editMode;
    protected LDAPProviderKerberosConfig kerberosConfig;
    protected PasswordUpdateCallback updater;
    protected LDAPStorageMapperManager mapperManager;
    protected LDAPStorageUserManager userManager;

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

        return proxy(realm, local, ldapObject);
    }

    protected UserModel proxy(RealmModel realm, UserModel local, LDAPObject ldapObject) {
        UserModel existing = userManager.getManagedProxiedUser(local.getId());
        if (existing != null) {
            return existing;
        }

        // We need to avoid having CachedUserModel as cache is upper-layer then LDAP. Hence having CachedUserModel here may cause StackOverflowError
        if (local instanceof CachedUserModel) {
            local = session.userStorageManager().getUserById(local.getId(), realm);

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
                    proxied = new ReadonlyLDAPUserModelDelegate(local, this);
                } else {
                    proxied = new ReadOnlyUserModelDelegate(local);
                }
                break;
            case WRITABLE:
                proxied = new WritableLDAPUserModelDelegate(local, this, ldapObject);
                break;
            case UNSYNCED:
                proxied = new UnsyncedLDAPUserModelDelegate(local, this);
        }

        List<ComponentModel> mappers = realm.getComponents(model.getId(), LDAPStorageMapper.class.getName());
        List<ComponentModel> sortedMappers = mapperManager.sortMappersAsc(mappers);
        for (ComponentModel mapperModel : sortedMappers) {
            LDAPStorageMapper ldapMapper = mapperManager.getMapper(mapperModel);
            proxied = ldapMapper.proxy(ldapObject, proxied, realm);
        }

        userManager.setManagedProxiedUser(proxied, ldapObject);

        return proxied;
    }

    private void checkDNChanged(RealmModel realm, UserModel local, LDAPObject ldapObject) {
        String dnFromDB = local.getFirstAttribute(LDAPConstants.LDAP_ENTRY_DN);
        String ldapDn = ldapObject.getDn().toString();
        if (!ldapDn.equals(dnFromDB)) {
            logger.debugf("Updated LDAP DN of user '%s' to '%s'", local.getUsername(), ldapDn);
            local.setSingleAttribute(LDAPConstants.LDAP_ENTRY_DN, ldapDn);

            UserCache userCache = session.userCache();
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
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
    	 try (LDAPQuery ldapQuery = LDAPUtils.createQueryForUserSearch(this, realm)) {
             LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();

             Condition attrCondition = conditionsBuilder.equal(attrName, attrValue, EscapeStrategy.DEFAULT);
             ldapQuery.addWhereCondition(attrCondition);

             List<LDAPObject> ldapObjects = ldapQuery.getResultList();

             if (ldapObjects == null || ldapObjects.isEmpty()) {
                 return Collections.emptyList();
             }

             List<UserModel> searchResults = new LinkedList<UserModel>();

             for (LDAPObject ldapUser : ldapObjects) {
                 String ldapUsername = LDAPUtils.getUsername(ldapUser, this.ldapIdentityStore.getConfig());
                 if (session.userLocalStorage().getUserByUsername(ldapUsername, realm) == null) {
                     UserModel imported = importUserFromLDAP(session, realm, ldapUser);
                     searchResults.add(imported);
                 }
             }

             return searchResults;
         }
    }

    public boolean synchronizeRegistrations() {
        return "true".equalsIgnoreCase(model.getConfig().getFirst(LDAPConstants.SYNC_REGISTRATIONS)) && editMode == UserStorageProvider.EditMode.WRITABLE;
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        if (!synchronizeRegistrations()) {
            return null;
        }
        UserModel user = null;
        if (model.isImportEnabled()) {
            user = session.userLocalStorage().addUser(realm, username);
            user.setFederationLink(model.getId());
        } else {
            user = new InMemoryUserAdapter(session, realm, new StorageId(model.getId(), username).getId());
            user.setUsername(username);
        }
        LDAPObject ldapUser = LDAPUtils.addUserToLDAP(this, realm, user);
        LDAPUtils.checkUuid(ldapUser, ldapIdentityStore.getConfig());
        user.setSingleAttribute(LDAPConstants.LDAP_ID, ldapUser.getUuid());
        user.setSingleAttribute(LDAPConstants.LDAP_ENTRY_DN, ldapUser.getDn().toString());

        // Add the user to the default groups and add default required actions
        UserModel proxy = proxy(realm, user, ldapUser);
        DefaultRoles.addDefaultRoles(realm, proxy);

        for (GroupModel g : realm.getDefaultGroups()) {
            proxy.joinGroup(g);
        }
        for (RequiredActionProviderModel r : realm.getRequiredActionProviders()) {
            if (r.isEnabled() && r.isDefaultAction()) {
                proxy.addRequiredAction(r.getAlias());
            }
        }

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
            logger.warnf("User '%s' can't be deleted from LDAP as it doesn't exist here", user.getUsername());
            return false;
        }

        ldapIdentityStore.remove(ldapObject);
        userManager.removeManagedUserEntry(user.getId());

        return true;
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        UserModel alreadyLoadedInSession = userManager.getManagedProxiedUser(id);
        if (alreadyLoadedInSession != null) return alreadyLoadedInSession;

        StorageId storageId = new StorageId(id);
        return getUserByUsername(storageId.getExternalId(), realm);
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return 0;
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return searchForUser(search, realm, 0, Integer.MAX_VALUE - 1);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        Map<String, String> attributes = new HashMap<String, String>();
        int spaceIndex = search.lastIndexOf(' ');
        if (spaceIndex > -1) {
            String firstName = search.substring(0, spaceIndex).trim();
            String lastName = search.substring(spaceIndex).trim();
            attributes.put(UserModel.FIRST_NAME, firstName);
            attributes.put(UserModel.LAST_NAME, lastName);
        } else if (search.indexOf('@') > -1) {
            attributes.put(UserModel.USERNAME, search.trim().toLowerCase());
            attributes.put(UserModel.EMAIL, search.trim().toLowerCase());
        } else {
            attributes.put(UserModel.LAST_NAME, search.trim());
            attributes.put(UserModel.USERNAME, search.trim().toLowerCase());
        }
        return searchForUser(attributes, realm, firstResult, maxResults);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
        return searchForUser(params, realm, 0, Integer.MAX_VALUE - 1);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult, int maxResults) {
        List<UserModel> searchResults =new LinkedList<UserModel>();

        List<LDAPObject> ldapUsers = searchLDAP(realm, params, maxResults + firstResult);
        int counter = 0;
        for (LDAPObject ldapUser : ldapUsers) {
            if (counter++ < firstResult) continue;
            String ldapUsername = LDAPUtils.getUsername(ldapUser, this.ldapIdentityStore.getConfig());
            if (session.userLocalStorage().getUserByUsername(ldapUsername, realm) == null) {
                UserModel imported = importUserFromLDAP(session, realm, ldapUser);
                searchResults.add(imported);
            }
        }

        return searchResults;
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        return getGroupMembers(realm, group, 0, Integer.MAX_VALUE - 1);
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        List<ComponentModel> mappers = realm.getComponents(model.getId(), LDAPStorageMapper.class.getName());
        List<ComponentModel> sortedMappers = mapperManager.sortMappersAsc(mappers);
        for (ComponentModel mapperModel : sortedMappers) {
            LDAPStorageMapper ldapMapper = mapperManager.getMapper(mapperModel);
            List<UserModel> users = ldapMapper.getGroupMembers(realm, group, firstResult, maxResults);

            // Sufficient for now
            if (users.size() > 0) {
                return users;
            }
        }
        return Collections.emptyList();
    }

    public List<UserModel> loadUsersByUsernames(List<String> usernames, RealmModel realm) {
        List<UserModel> result = new ArrayList<>();
        for (String username : usernames) {
            UserModel kcUser = session.users().getUserByUsername(username, realm);
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

    protected List<LDAPObject> searchLDAP(RealmModel realm, Map<String, String> attributes, int maxResults) {

        List<LDAPObject> results = new ArrayList<LDAPObject>();
        if (attributes.containsKey(UserModel.USERNAME)) {
            try (LDAPQuery ldapQuery = LDAPUtils.createQueryForUserSearch(this, realm)) {
                LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();

                // Mapper should replace "username" in parameter name with correct LDAP mapped attribute
                Condition usernameCondition = conditionsBuilder.equal(UserModel.USERNAME, attributes.get(UserModel.USERNAME), EscapeStrategy.NON_ASCII_CHARS_ONLY);
                ldapQuery.addWhereCondition(usernameCondition);

                List<LDAPObject> ldapObjects = ldapQuery.getResultList();
                results.addAll(ldapObjects);
            }
        }

        if (attributes.containsKey(UserModel.EMAIL)) {
            try (LDAPQuery ldapQuery = LDAPUtils.createQueryForUserSearch(this, realm)) {
                LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();

                // Mapper should replace "email" in parameter name with correct LDAP mapped attribute
                Condition emailCondition = conditionsBuilder.equal(UserModel.EMAIL, attributes.get(UserModel.EMAIL), EscapeStrategy.NON_ASCII_CHARS_ONLY);
                ldapQuery.addWhereCondition(emailCondition);

                List<LDAPObject> ldapObjects = ldapQuery.getResultList();
                results.addAll(ldapObjects);
            }
        }

        if (attributes.containsKey(UserModel.FIRST_NAME) || attributes.containsKey(UserModel.LAST_NAME)) {
            try (LDAPQuery ldapQuery = LDAPUtils.createQueryForUserSearch(this, realm)) {
                LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();

                // Mapper should replace parameter with correct LDAP mapped attributes
                if (attributes.containsKey(UserModel.FIRST_NAME)) {
                    ldapQuery.addWhereCondition(conditionsBuilder.equal(UserModel.FIRST_NAME, attributes.get(UserModel.FIRST_NAME), EscapeStrategy.NON_ASCII_CHARS_ONLY));
                }
                if (attributes.containsKey(UserModel.LAST_NAME)) {
                    ldapQuery.addWhereCondition(conditionsBuilder.equal(UserModel.LAST_NAME, attributes.get(UserModel.LAST_NAME), EscapeStrategy.NON_ASCII_CHARS_ONLY));
                }

                List<LDAPObject> ldapObjects = ldapQuery.getResultList();
                results.addAll(ldapObjects);
            }
        }

        return results;
    }

    /**
     * @param local
     * @return ldapUser corresponding to local user or null if user is no longer in LDAP
     */
    protected LDAPObject loadAndValidateUser(RealmModel realm, UserModel local) {
        LDAPObject existing = userManager.getManagedLDAPUser(local.getId());
        if (existing != null) {
            return existing;
        }

        LDAPObject ldapUser = loadLDAPUserByUsername(realm, local.getUsername());
        if (ldapUser == null) {
            return null;
        }
        LDAPUtils.checkUuid(ldapUser, ldapIdentityStore.getConfig());

        if (ldapUser.getUuid().equals(local.getFirstAttribute(LDAPConstants.LDAP_ID))) {
            return ldapUser;
        } else {
            logger.warnf("LDAP User invalid. ID doesn't match. ID from LDAP [%s], LDAP ID from local DB: [%s]", ldapUser.getUuid(), local.getFirstAttribute(LDAPConstants.LDAP_ID));
            return null;
        }
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        LDAPObject ldapUser = loadLDAPUserByUsername(realm, username);
        if (ldapUser == null) {
            return null;
        }

        return importUserFromLDAP(session, realm, ldapUser);
    }

    protected UserModel importUserFromLDAP(KeycloakSession session, RealmModel realm, LDAPObject ldapUser) {
        String ldapUsername = LDAPUtils.getUsername(ldapUser, ldapIdentityStore.getConfig());
        LDAPUtils.checkUuid(ldapUser, ldapIdentityStore.getConfig());

        UserModel imported = null;
        if (model.isImportEnabled()) {
            imported = session.userLocalStorage().addUser(realm, ldapUsername);
        } else {
            InMemoryUserAdapter adapter = new InMemoryUserAdapter(session, realm, new StorageId(model.getId(), ldapUsername).getId());
            adapter.addDefaults();
            imported = adapter;
        }
        imported.setEnabled(true);

        List<ComponentModel> mappers = realm.getComponents(model.getId(), LDAPStorageMapper.class.getName());
        List<ComponentModel> sortedMappers = mapperManager.sortMappersDesc(mappers);
        for (ComponentModel mapperModel : sortedMappers) {
            if (logger.isTraceEnabled()) {
                logger.tracef("Using mapper %s during import user from LDAP", mapperModel);
            }
            LDAPStorageMapper ldapMapper = mapperManager.getMapper(mapperModel);
            ldapMapper.onImportUserFromLDAP(ldapUser, imported, realm, true);
        }

        String userDN = ldapUser.getDn().toString();
        if (model.isImportEnabled()) imported.setFederationLink(model.getId());
        imported.setSingleAttribute(LDAPConstants.LDAP_ID, ldapUser.getUuid());
        imported.setSingleAttribute(LDAPConstants.LDAP_ENTRY_DN, userDN);
        if(getLdapIdentityStore().getConfig().isTrustEmail()){
            imported.setEmailVerified(true);
        }
        logger.debugf("Imported new user from LDAP to Keycloak DB. Username: [%s], Email: [%s], LDAP_ID: [%s], LDAP Entry DN: [%s]", imported.getUsername(), imported.getEmail(),
                ldapUser.getUuid(), userDN);
        UserModel proxy = proxy(realm, imported, ldapUser);
        return proxy;
    }

    protected LDAPObject queryByEmail(RealmModel realm, String email) {
        try (LDAPQuery ldapQuery = LDAPUtils.createQueryForUserSearch(this, realm)) {
            LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();

            // Mapper should replace "email" in parameter name with correct LDAP mapped attribute
            Condition emailCondition = conditionsBuilder.equal(UserModel.EMAIL, email, EscapeStrategy.DEFAULT);
            ldapQuery.addWhereCondition(emailCondition);

            return ldapQuery.getFirstResult();
        }
    }


    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        LDAPObject ldapUser = queryByEmail(realm, email);
        if (ldapUser == null) {
            return null;
        }

        // Check here if user already exists
        String ldapUsername = LDAPUtils.getUsername(ldapUser, ldapIdentityStore.getConfig());
        UserModel user = session.userLocalStorage().getUserByUsername(ldapUsername, realm);

        if (user != null) {
            LDAPUtils.checkUuid(ldapUser, ldapIdentityStore.getConfig());
            // If email attribute mapper is set to "Always Read Value From LDAP" the user may be in Keycloak DB with an old email address
            if (ldapUser.getUuid().equals(user.getFirstAttribute(LDAPConstants.LDAP_ID))) return user;
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
            return authenticator.validUser(user.getUsername(), password);
        } else {
            // Use Naming LDAP API
            LDAPObject ldapUser = loadAndValidateUser(realm, user);

            try {
                ldapIdentityStore.validatePassword(ldapUser, password);
                return true;
            } catch (AuthenticationException ae) {
                boolean processed = false;
                List<ComponentModel> mappers = realm.getComponents(model.getId(), LDAPStorageMapper.class.getName());
                List<ComponentModel> sortedMappers = mapperManager.sortMappersDesc(mappers);
                for (ComponentModel mapperModel : sortedMappers) {
                    if (logger.isTraceEnabled()) {
                        logger.tracef("Using mapper %s during import user from LDAP", mapperModel);
                    }
                    LDAPStorageMapper ldapMapper = mapperManager.getMapper(mapperModel);
                    processed = processed || ldapMapper.onAuthenticationFailure(ldapUser, user, ae, realm);
                }
                return processed;
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
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
        return Collections.EMPTY_SET;
    }

    public Set<String> getSupportedCredentialTypes() {
        return new HashSet<String>(this.supportedCredentialTypes);
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
        if (input.getType().equals(PasswordCredentialModel.TYPE) && !session.userCredentialManager().isConfiguredLocally(realm, user, PasswordCredentialModel.TYPE)) {
            return validPassword(realm, user, input.getChallengeResponse());
        } else {
            return false; // invalid cred type
        }
    }

    @Override
    public CredentialValidationOutput authenticate(RealmModel realm, CredentialInput cred) {
        if (!(cred instanceof UserCredentialModel)) CredentialValidationOutput.failed();
        UserCredentialModel credential = (UserCredentialModel)cred;
        if (credential.getType().equals(UserCredentialModel.KERBEROS)) {
            if (kerberosConfig.isAllowKerberosAuthentication()) {
                String spnegoToken = credential.getChallengeResponse();
                SPNEGOAuthenticator spnegoAuthenticator = factory.createSPNEGOAuthenticator(spnegoToken, kerberosConfig);

                spnegoAuthenticator.authenticate();

                Map<String, String> state = new HashMap<String, String>();
                if (spnegoAuthenticator.isAuthenticated()) {

                    // TODO: This assumes that LDAP "uid" is equal to kerberos principal name. Like uid "hnelson" and kerberos principal "hnelson@KEYCLOAK.ORG".
                    // Check if it's correct or if LDAP attribute for mapping kerberos principal should be available (For ApacheDS it seems to be attribute "krb5PrincipalName" but on MSAD it's likely different)
                    String username = spnegoAuthenticator.getAuthenticatedUsername();
                    UserModel user = findOrCreateAuthenticatedUser(realm, username);

                    if (user == null) {
                        logger.warnf("Kerberos/SPNEGO authentication succeeded with username [%s], but couldn't find or create user with federation provider [%s]", username, model.getName());
                        return CredentialValidationOutput.failed();
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
                    return CredentialValidationOutput.failed();
                }
            }
        }

        return CredentialValidationOutput.failed();
    }

    @Override
    public void close() {
    }

    /**
     * Called after successful kerberos authentication
     *
     * @param realm realm
     * @param username username without realm prefix
     * @return finded or newly created user
     */
    protected UserModel findOrCreateAuthenticatedUser(RealmModel realm, String username) {
        UserModel user = session.userLocalStorage().getUserByUsername(username, realm);
        if (user != null) {
            logger.debugf("Kerberos authenticated user [%s] found in Keycloak storage", username);
            if (!model.getId().equals(user.getFederationLink())) {
                logger.warnf("User with username [%s] already exists, but is not linked to provider [%s]", username, model.getName());
                return null;
            } else {
                LDAPObject ldapObject = loadAndValidateUser(realm, user);
                if (ldapObject != null) {
                    return proxy(realm, user, ldapObject);
                } else {
                    logger.warnf("User with username [%s] aready exists and is linked to provider [%s] but is not valid. Stale LDAP_ID on local user is: %s",
                            username,  model.getName(), user.getFirstAttribute(LDAPConstants.LDAP_ID));
                    logger.warn("Will re-create user");
                    UserCache userCache = session.userCache();
                    if (userCache != null) {
                        userCache.evict(realm, user);
                    }
                    new UserManager(session).removeUser(realm, user, session.userLocalStorage());
                }
            }
        }

        // Creating user to local storage
        logger.debugf("Kerberos authenticated user [%s] not in Keycloak storage. Creating him", username);
        return getUserByUsername(username, realm);
    }

    public LDAPObject loadLDAPUserByUsername(RealmModel realm, String username) {
        try (LDAPQuery ldapQuery = LDAPUtils.createQueryForUserSearch(this, realm)) {
            LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();

            String usernameMappedAttribute = this.ldapIdentityStore.getConfig().getUsernameLdapAttribute();
            Condition usernameCondition = conditionsBuilder.equal(usernameMappedAttribute, username, EscapeStrategy.DEFAULT);
            ldapQuery.addWhereCondition(usernameCondition);

            LDAPObject ldapUser = ldapQuery.getFirstResult();
            if (ldapUser == null) {
                return null;
            }

            return ldapUser;
        }
    }


}
