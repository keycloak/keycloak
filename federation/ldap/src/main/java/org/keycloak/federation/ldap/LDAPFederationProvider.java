package org.keycloak.federation.ldap;

import org.jboss.logging.Logger;
import org.keycloak.federation.kerberos.impl.KerberosUsernamePasswordAuthenticator;
import org.keycloak.federation.kerberos.impl.SPNEGOAuthenticator;
import org.keycloak.federation.ldap.idm.model.LDAPUser;
import org.keycloak.federation.ldap.idm.query.internal.IdentityQuery;
import org.keycloak.federation.ldap.idm.query.internal.IdentityQueryBuilder;
import org.keycloak.federation.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.federation.ldap.kerberos.LDAPProviderKerberosConfig;
import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserFederationSyncResult;
import org.keycloak.models.UserModel;
import org.keycloak.constants.KerberosConstants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    protected final Set<String> supportedCredentialTypes = new HashSet<String>();

    public LDAPFederationProvider(LDAPFederationProviderFactory factory, KeycloakSession session, UserFederationProviderModel model, LDAPIdentityStore ldapIdentityStore) {
        this.factory = factory;
        this.session = session;
        this.model = model;
        this.ldapIdentityStore = ldapIdentityStore;
        this.kerberosConfig = new LDAPProviderKerberosConfig(model);
        String editModeString = model.getConfig().get(LDAPConstants.EDIT_MODE);
        if (editModeString == null) {
            editMode = EditMode.READ_ONLY;
        } else {
            editMode = EditMode.valueOf(editModeString);
        }

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

    @Override
    public UserModel proxy(UserModel local) {
         switch (editMode) {
             case READ_ONLY:
                return new ReadonlyLDAPUserModelDelegate(local, this);
             case WRITABLE:
                return new WritableLDAPUserModelDelegate(local, this);
             case UNSYNCED:
                return new UnsyncedLDAPUserModelDelegate(local, this);
         }
        return local;
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

        LDAPUser ldapUser = LDAPUtils.addUser(this.ldapIdentityStore, user.getUsername(), user.getFirstName(), user.getLastName(), user.getEmail());
        user.setAttribute(LDAPConstants.LDAP_ID, ldapUser.getId());
        user.setAttribute(LDAPConstants.LDAP_ENTRY_DN, ldapUser.getEntryDN());
        return proxy(user);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        if (editMode == EditMode.READ_ONLY || editMode == EditMode.UNSYNCED) {
            logger.warnf("User '%s' can't be deleted in LDAP as editMode is '%s'", user.getUsername(), editMode.toString());
            return false;
        }

        return LDAPUtils.removeUser(this.ldapIdentityStore, user.getUsername());
    }

    @Override
    public List<UserModel> searchByAttributes(Map<String, String> attributes, RealmModel realm, int maxResults) {
        List<UserModel> searchResults =new LinkedList<UserModel>();

        Map<String, LDAPUser> ldapUsers = searchLDAP(attributes, maxResults);
        for (LDAPUser ldapUser : ldapUsers.values()) {
            if (session.userStorage().getUserByUsername(ldapUser.getLoginName(), realm) == null) {
                UserModel imported = importUserFromLDAP(realm, ldapUser);
                searchResults.add(imported);
            }
        }

        return searchResults;
    }

    protected Map<String, LDAPUser> searchLDAP(Map<String, String> attributes, int maxResults) {

        Map<String, LDAPUser> results = new HashMap<String, LDAPUser>();
        if (attributes.containsKey(USERNAME)) {
            LDAPUser user = LDAPUtils.getUser(this.ldapIdentityStore, attributes.get(USERNAME));
            if (user != null) {
                results.put(user.getLoginName(), user);
            }
        }

        if (attributes.containsKey(EMAIL)) {
            LDAPUser user = queryByEmail(attributes.get(EMAIL));
            if (user != null) {
                results.put(user.getLoginName(), user);
            }
        }

        if (attributes.containsKey(FIRST_NAME) || attributes.containsKey(LAST_NAME)) {
            IdentityQueryBuilder queryBuilder = this.ldapIdentityStore.createQueryBuilder();
            IdentityQuery<LDAPUser> query = queryBuilder.createIdentityQuery(LDAPUser.class);
            if (attributes.containsKey(FIRST_NAME)) {
                query.where(queryBuilder.equal(LDAPUser.FIRST_NAME, attributes.get(FIRST_NAME)));
            }
            if (attributes.containsKey(LAST_NAME)) {
                query.where(queryBuilder.equal(LDAPUser.LAST_NAME, attributes.get(LAST_NAME)));
            }
            query.setLimit(maxResults);
            List<LDAPUser> users = query.getResultList();
            for (LDAPUser user : users) {
                results.put(user.getLoginName(), user);
            }
        }

        return results;
    }

    @Override
    public boolean isValid(UserModel local) {
        LDAPUser ldapUser = LDAPUtils.getUser(this.ldapIdentityStore, local.getUsername());
        if (ldapUser == null) {
            return false;
        }
        return ldapUser.getId().equals(local.getAttribute(LDAPConstants.LDAP_ID));
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        LDAPUser ldapUser = LDAPUtils.getUser(this.ldapIdentityStore, username);
        if (ldapUser == null) {
            return null;
        }

        // KEYCLOAK-808: Should we allow case-sensitivity to be configurable?
        if (!username.equals(ldapUser.getLoginName())) {
            logger.warnf("User found in LDAP but with different username. LDAP username: %s, Searched username: %s", username, ldapUser.getLoginName());
            return null;
        }

        return importUserFromLDAP(realm, ldapUser);
    }

    protected UserModel importUserFromLDAP(RealmModel realm, LDAPUser ldapUser) {
        String email = (ldapUser.getEmail() != null && ldapUser.getEmail().trim().length() > 0) ? ldapUser.getEmail() : null;

        if (ldapUser.getLoginName() == null) {
            throw new ModelException("User returned from LDAP has null username! Check configuration of your LDAP mappings. ID of user from LDAP: " + ldapUser.getId());
        }

        UserModel imported = session.userStorage().addUser(realm, ldapUser.getLoginName());
        imported.setEnabled(true);
        imported.setEmail(email);
        imported.setFirstName(ldapUser.getFirstName());
        imported.setLastName(ldapUser.getLastName());
        imported.setFederationLink(model.getId());
        imported.setAttribute(LDAPConstants.LDAP_ID, ldapUser.getId());
        imported.setAttribute(LDAPConstants.LDAP_ENTRY_DN, ldapUser.getEntryDN());

        logger.debugf("Imported new user from LDAP to Keycloak DB. Username: [%s], Email: [%s], LDAP_ID: [%s], LDAP Entry DN: [%s]", imported.getUsername(), imported.getEmail(),
                ldapUser.getId(), ldapUser.getEntryDN());
        return proxy(imported);
    }

    protected LDAPUser queryByEmail(String email) {
        return LDAPUtils.getUserByEmail(this.ldapIdentityStore, email);
    }


    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        LDAPUser ldapUser = queryByEmail(email);
        if (ldapUser == null) {
            return null;
        }

        // KEYCLOAK-808: Should we allow case-sensitivity to be configurable?
        if (!email.equals(ldapUser.getEmail())) {
            logger.warnf("User found in LDAP but with different email. LDAP email: %s, Searched email: %s", email, ldapUser.getEmail());
            return null;
        }

        return importUserFromLDAP(realm, ldapUser);
    }

    @Override
    public void preRemove(RealmModel realm) {
        // complete Don't think we have to do anything
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        // complete I don't think we have to do anything here
    }

    public boolean validPassword(UserModel user, String password) {
        if (kerberosConfig.isAllowKerberosAuthentication() && kerberosConfig.isUseKerberosForPasswordAuthentication()) {
            // Use Kerberos JAAS (Krb5LoginModule)
            KerberosUsernamePasswordAuthenticator authenticator = factory.createKerberosUsernamePasswordAuthenticator(kerberosConfig);
            return authenticator.validUser(user.getUsername(), password);
        } else {
            // Use Naming LDAP API
            return LDAPUtils.validatePassword(this.ldapIdentityStore, user, password);
        }
    }


    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, List<UserCredentialModel> input) {
        for (UserCredentialModel cred : input) {
            if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
                return validPassword(user, cred.getValue());
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

    protected UserFederationSyncResult importLDAPUsers(RealmModel realm, List<LDAPUser> ldapUsers, UserFederationProviderModel fedModel) {
        UserFederationSyncResult syncResult = new UserFederationSyncResult();

        for (LDAPUser ldapUser : ldapUsers) {
            String username = ldapUser.getLoginName();
            UserModel currentUser = session.userStorage().getUserByUsername(username, realm);

            if (currentUser == null) {
                // Add new user to Keycloak
                importUserFromLDAP(realm, ldapUser);
                syncResult.increaseAdded();
            } else {
                if ((fedModel.getId().equals(currentUser.getFederationLink())) && (ldapUser.getId().equals(currentUser.getAttribute(LDAPConstants.LDAP_ID)))) {
                    // Update keycloak user
                    String email = (ldapUser.getEmail() != null && ldapUser.getEmail().trim().length() > 0) ? ldapUser.getEmail() : null;
                    currentUser.setEmail(email);
                    currentUser.setFirstName(ldapUser.getFirstName());
                    currentUser.setLastName(ldapUser.getLastName());
                    logger.debugf("Updated user from LDAP: %s", currentUser.getUsername());
                    syncResult.increaseUpdated();
                } else {
                    logger.warnf("User '%s' is not updated during sync as he is not linked to federation provider '%s'", username, fedModel.getDisplayName());
                }
            }
        }

        return syncResult;
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
            } else if (isValid(user)) {
                return proxy(user);
            } else {
                logger.warnf("User with username [%s] aready exists and is linked to provider [%s] but is not valid. Stale LDAP_ID on local user is: %s",
                        username,  model.getDisplayName(), user.getAttribute(LDAPConstants.LDAP_ID));
                logger.warn("Will re-create user");
                session.userStorage().removeUser(realm, user);
            }
        }

        // Creating user to local storage
        logger.debugf("Kerberos authenticated user [%s] not in Keycloak storage. Creating him", username);
        return getUserByUsername(realm, username);
    }
}
