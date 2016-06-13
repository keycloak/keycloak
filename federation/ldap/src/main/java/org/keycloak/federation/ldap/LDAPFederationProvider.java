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

package org.keycloak.federation.ldap;

import org.jboss.logging.Logger;
import org.keycloak.federation.kerberos.impl.KerberosUsernamePasswordAuthenticator;
import org.keycloak.federation.kerberos.impl.SPNEGOAuthenticator;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQueryConditionsBuilder;
import org.keycloak.federation.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.federation.ldap.kerberos.LDAPProviderKerberosConfig;
import org.keycloak.federation.ldap.mappers.LDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.LDAPMappersComparator;
import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.mappers.UserFederationMapper;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.services.managers.UserManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.AuthenticationException;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class LDAPFederationProvider implements UserFederationProvider {
    private static final Logger logger = Logger.getLogger(LDAPFederationProvider.class);

    protected LDAPFederationProviderFactory factory;
    protected KeycloakSession session;
    protected UserFederationProviderModel model;
    protected LDAPIdentityStore ldapIdentityStore;
    protected EditMode editMode;
    protected LDAPProviderKerberosConfig kerberosConfig;

    protected final Set<String> supportedCredentialTypes = new HashSet<>();

    public LDAPFederationProvider(LDAPFederationProviderFactory factory, KeycloakSession session, UserFederationProviderModel model, LDAPIdentityStore ldapIdentityStore) {
        this.factory = factory;
        this.session = session;
        this.model = model;
        this.ldapIdentityStore = ldapIdentityStore;
        this.kerberosConfig = new LDAPProviderKerberosConfig(model);
        this.editMode = ldapIdentityStore.getConfig().getEditMode();

        supportedCredentialTypes.add(UserCredentialModel.PASSWORD);
        if (kerberosConfig.isAllowKerberosAuthentication()) {
            supportedCredentialTypes.add(UserCredentialModel.KERBEROS);
        }
    }

    public KeycloakSession getSession() {
        return session;
    }

    public UserFederationProviderModel getModel() {
        return model;
    }

    public LDAPIdentityStore getLdapIdentityStore() {
        return this.ldapIdentityStore;
    }

    public EditMode getEditMode() {
        return editMode;
    }

    @Override
    public UserModel validateAndProxy(RealmModel realm, UserModel local) {
        LDAPObject ldapObject = loadAndValidateUser(realm, local);
        if (ldapObject == null) {
            return null;
        }

        return proxy(realm, local, ldapObject);
    }

    protected UserModel proxy(RealmModel realm, UserModel local, LDAPObject ldapObject) {
        UserModel proxied = local;
        switch (editMode) {
            case READ_ONLY:
                proxied = new ReadonlyLDAPUserModelDelegate(local, this);
                break;
            case WRITABLE:
                proxied = new WritableLDAPUserModelDelegate(local, this, ldapObject);
                break;
            case UNSYNCED:
                proxied = new UnsyncedLDAPUserModelDelegate(local, this);
        }

        Set<UserFederationMapperModel> federationMappers = realm.getUserFederationMappersByFederationProvider(model.getId());
        List<UserFederationMapperModel> sortedMappers = sortMappersAsc(federationMappers);
        for (UserFederationMapperModel mapperModel : sortedMappers) {
            LDAPFederationMapper ldapMapper = getMapper(mapperModel);
            proxied = ldapMapper.proxy(mapperModel, this, ldapObject, proxied, realm);
        }

        return proxied;
    }

    @Override
    public Set<String> getSupportedCredentialTypes(UserModel local) {
        Set<String> supportedCredentialTypes = new HashSet<String>(this.supportedCredentialTypes);
        if (editMode == EditMode.UNSYNCED ) {
            for (UserCredentialValueModel cred : local.getCredentialsDirectly()) {
                if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
                    // User has changed password in KC local database. Use KC password instead of LDAP password
                    supportedCredentialTypes.remove(UserCredentialModel.PASSWORD);
                }
            }
        }
        return supportedCredentialTypes;
    }

    @Override
    public Set<String> getSupportedCredentialTypes() {
        return new HashSet<String>(this.supportedCredentialTypes);
    }

    @Override
    public boolean synchronizeRegistrations() {
        return "true".equalsIgnoreCase(model.getConfig().get(LDAPConstants.SYNC_REGISTRATIONS)) && editMode == EditMode.WRITABLE;
    }

    @Override
    public UserModel register(RealmModel realm, UserModel user) {
        if (editMode == EditMode.READ_ONLY || editMode == EditMode.UNSYNCED) throw new IllegalStateException("Registration is not supported by this ldap server");
        if (!synchronizeRegistrations()) throw new IllegalStateException("Registration is not supported by this ldap server");

        LDAPObject ldapUser = LDAPUtils.addUserToLDAP(this, realm, user);
        LDAPUtils.checkUuid(ldapUser, ldapIdentityStore.getConfig());
        user.setSingleAttribute(LDAPConstants.LDAP_ID, ldapUser.getUuid());
        user.setSingleAttribute(LDAPConstants.LDAP_ENTRY_DN, ldapUser.getDn().toString());

        return proxy(realm, user, ldapUser);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        if (editMode == EditMode.READ_ONLY || editMode == EditMode.UNSYNCED) {
            logger.warnf("User '%s' can't be deleted in LDAP as editMode is '%s'. Deleting user just from Keycloak DB, but he will be re-imported from LDAP again once searched in Keycloak", user.getUsername(), editMode.toString());
            return true;
        }

        LDAPObject ldapObject = loadAndValidateUser(realm, user);
        if (ldapObject == null) {
            logger.warnf("User '%s' can't be deleted from LDAP as it doesn't exist here", user.getUsername());
            return false;
        }

        ldapIdentityStore.remove(ldapObject);
        return true;
    }

    @Override
    public List<UserModel> searchByAttributes(Map<String, String> attributes, RealmModel realm, int maxResults) {
        List<UserModel> searchResults =new LinkedList<UserModel>();

        List<LDAPObject> ldapUsers = searchLDAP(realm, attributes, maxResults);
        for (LDAPObject ldapUser : ldapUsers) {
            String ldapUsername = LDAPUtils.getUsername(ldapUser, this.ldapIdentityStore.getConfig());
            if (session.userStorage().getUserByUsername(ldapUsername, realm) == null) {
                UserModel imported = importUserFromLDAP(session, realm, ldapUser);
                searchResults.add(imported);
            }
        }

        return searchResults;
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        Set<UserFederationMapperModel> federationMappers = realm.getUserFederationMappersByFederationProvider(model.getId());
        for (UserFederationMapperModel mapperModel : federationMappers) {
            LDAPFederationMapper ldapMapper = getMapper(mapperModel);
            List<UserModel> users = ldapMapper.getGroupMembers(mapperModel, this, realm, group, firstResult, maxResults);

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
            } else if (!model.getId().equals(kcUser.getFederationLink())) {
                logger.warnf("Incorrect federation provider of user '%s'", kcUser.getUsername());
            } else {
                result.add(kcUser);
            }
        }
        return result;
    }

    protected List<LDAPObject> searchLDAP(RealmModel realm, Map<String, String> attributes, int maxResults) {

        List<LDAPObject> results = new ArrayList<LDAPObject>();
        if (attributes.containsKey(USERNAME)) {
            LDAPObject user = loadLDAPUserByUsername(realm, attributes.get(USERNAME));
            if (user != null) {
                results.add(user);
            }
        }

        if (attributes.containsKey(EMAIL)) {
            LDAPObject user = queryByEmail(realm, attributes.get(EMAIL));
            if (user != null) {
                results.add(user);
            }
        }

        if (attributes.containsKey(FIRST_NAME) || attributes.containsKey(LAST_NAME)) {
            LDAPQuery ldapQuery = LDAPUtils.createQueryForUserSearch(this, realm);
            LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();

            // Mapper should replace parameter with correct LDAP mapped attributes
            if (attributes.containsKey(FIRST_NAME)) {
                ldapQuery.addWhereCondition(conditionsBuilder.equal(FIRST_NAME, attributes.get(FIRST_NAME)));
            }
            if (attributes.containsKey(LAST_NAME)) {
                ldapQuery.addWhereCondition(conditionsBuilder.equal(LAST_NAME, attributes.get(LAST_NAME)));
            }

            List<LDAPObject> ldapObjects = ldapQuery.getResultList();
            results.addAll(ldapObjects);
        }

        return results;
    }

    /**
     * @param local
     * @return ldapUser corresponding to local user or null if user is no longer in LDAP
     */
    protected LDAPObject loadAndValidateUser(RealmModel realm, UserModel local) {
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
    public boolean isValid(RealmModel realm, UserModel local) {
        return loadAndValidateUser(realm, local) != null;
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
        String ldapUsername = LDAPUtils.getUsername(ldapUser, ldapIdentityStore.getConfig());
        LDAPUtils.checkUuid(ldapUser, ldapIdentityStore.getConfig());

        UserModel imported = session.userStorage().addUser(realm, ldapUsername);
        imported.setEnabled(true);

        Set<UserFederationMapperModel> federationMappers = realm.getUserFederationMappersByFederationProvider(getModel().getId());
        List<UserFederationMapperModel> sortedMappers = sortMappersDesc(federationMappers);
        for (UserFederationMapperModel mapperModel : sortedMappers) {
            if (logger.isTraceEnabled()) {
                logger.tracef("Using mapper %s during import user from LDAP", mapperModel);
            }
            LDAPFederationMapper ldapMapper = getMapper(mapperModel);
            ldapMapper.onImportUserFromLDAP(mapperModel, this, ldapUser, imported, realm, true);
        }

        String userDN = ldapUser.getDn().toString();
        imported.setFederationLink(model.getId());
        imported.setSingleAttribute(LDAPConstants.LDAP_ID, ldapUser.getUuid());
        imported.setSingleAttribute(LDAPConstants.LDAP_ENTRY_DN, userDN);

        logger.debugf("Imported new user from LDAP to Keycloak DB. Username: [%s], Email: [%s], LDAP_ID: [%s], LDAP Entry DN: [%s]", imported.getUsername(), imported.getEmail(),
                ldapUser.getUuid(), userDN);
        return proxy(realm, imported, ldapUser);
    }

    protected LDAPObject queryByEmail(RealmModel realm, String email) {
        LDAPQuery ldapQuery = LDAPUtils.createQueryForUserSearch(this, realm);
        LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();

        // Mapper should replace "email" in parameter name with correct LDAP mapped attribute
        Condition emailCondition = conditionsBuilder.equal(UserModel.EMAIL, email);
        ldapQuery.addWhereCondition(emailCondition);

        return ldapQuery.getFirstResult();
    }


    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        LDAPObject ldapUser = queryByEmail(realm, email);
        if (ldapUser == null) {
            return null;
        }

        // Check here if user already exists
        String ldapUsername = LDAPUtils.getUsername(ldapUser, ldapIdentityStore.getConfig());
        if (session.userStorage().getUserByUsername(ldapUsername, realm) != null) {
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

                // Check if any mapper provides callback for handle LDAP AuthenticationException
                Set<UserFederationMapperModel> federationMappers = realm.getUserFederationMappersByFederationProvider(getModel().getId());
                boolean processed = false;
                for (UserFederationMapperModel mapperModel : federationMappers) {
                    LDAPFederationMapper ldapMapper = getMapper(mapperModel);
                    processed = processed || ldapMapper.onAuthenticationFailure(mapperModel, this, ldapUser, user, ae, realm);
                }

                return processed;
            }
        }
    }


    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, List<UserCredentialModel> input) {
        for (UserCredentialModel cred : input) {
            if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
                return validPassword(realm, user, cred.getValue());
            } else {
                return false; // invalid cred type
            }
        }
        return true;
    }

    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, UserCredentialModel... input) {
        return validCredentials(realm, user, Arrays.asList(input));
    }

    @Override
    public CredentialValidationOutput validCredentials(RealmModel realm, UserCredentialModel credential) {
        if (credential.getType().equals(UserCredentialModel.KERBEROS)) {
            if (kerberosConfig.isAllowKerberosAuthentication()) {
                String spnegoToken = credential.getValue();
                SPNEGOAuthenticator spnegoAuthenticator = factory.createSPNEGOAuthenticator(spnegoToken, kerberosConfig);

                spnegoAuthenticator.authenticate();

                Map<String, String> state = new HashMap<String, String>();
                if (spnegoAuthenticator.isAuthenticated()) {

                    // TODO: This assumes that LDAP "uid" is equal to kerberos principal name. Like uid "hnelson" and kerberos principal "hnelson@KEYCLOAK.ORG".
                    // Check if it's correct or if LDAP attribute for mapping kerberos principal should be available (For ApacheDS it seems to be attribute "krb5PrincipalName" but on MSAD it's likely different)
                    String username = spnegoAuthenticator.getAuthenticatedUsername();
                    UserModel user = findOrCreateAuthenticatedUser(realm, username);

                    if (user == null) {
                        logger.warnf("Kerberos/SPNEGO authentication succeeded with username [%s], but couldn't find or create user with federation provider [%s]", username, model.getDisplayName());
                        return CredentialValidationOutput.failed();
                    } else {
                        String delegationCredential = spnegoAuthenticator.getSerializedDelegationCredential();
                        if (delegationCredential != null) {
                            state.put(KerberosConstants.GSS_DELEGATION_CREDENTIAL, delegationCredential);
                        }

                        return new CredentialValidationOutput(user, CredentialValidationOutput.Status.AUTHENTICATED, state);
                    }
                }  else {
                    state.put(KerberosConstants.RESPONSE_TOKEN, spnegoAuthenticator.getResponseToken());
                    return new CredentialValidationOutput(null, CredentialValidationOutput.Status.CONTINUE, state);
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
        UserModel user = session.userStorage().getUserByUsername(username, realm);
        if (user != null) {
            logger.debugf("Kerberos authenticated user [%s] found in Keycloak storage", username);
            if (!model.getId().equals(user.getFederationLink())) {
                logger.warnf("User with username [%s] already exists, but is not linked to provider [%s]", username, model.getDisplayName());
                return null;
            } else {
                LDAPObject ldapObject = loadAndValidateUser(realm, user);
                if (ldapObject != null) {
                    return proxy(realm, user, ldapObject);
                } else {
                    logger.warnf("User with username [%s] aready exists and is linked to provider [%s] but is not valid. Stale LDAP_ID on local user is: %s",
                            username,  model.getDisplayName(), user.getFirstAttribute(LDAPConstants.LDAP_ID));
                    logger.warn("Will re-create user");
                    new UserManager(session).removeUser(realm, user, session.userStorage());
                }
            }
        }

        // Creating user to local storage
        logger.debugf("Kerberos authenticated user [%s] not in Keycloak storage. Creating him", username);
        return getUserByUsername(realm, username);
    }

    public LDAPObject loadLDAPUserByUsername(RealmModel realm, String username) {
        LDAPQuery ldapQuery = LDAPUtils.createQueryForUserSearch(this, realm);
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

    public LDAPFederationMapper getMapper(UserFederationMapperModel mapperModel) {
        LDAPFederationMapper ldapMapper = (LDAPFederationMapper) getSession().getProvider(UserFederationMapper.class, mapperModel.getFederationMapperType());
        if (ldapMapper == null) {
            throw new ModelException("Can't find mapper type with ID: " + mapperModel.getFederationMapperType());
        }

        return ldapMapper;
    }


    public List<UserFederationMapperModel> sortMappersAsc(Collection<UserFederationMapperModel> mappers) {
        return LDAPMappersComparator.sortAsc(getLdapIdentityStore().getConfig(), mappers);
    }

    protected List<UserFederationMapperModel> sortMappersDesc(Collection<UserFederationMapperModel> mappers) {
        return LDAPMappersComparator.sortDesc(getLdapIdentityStore().getConfig(), mappers);
    }
}
