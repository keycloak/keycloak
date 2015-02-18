package org.keycloak.federation.ldap;

import org.jboss.logging.Logger;
import org.keycloak.federation.kerberos.impl.KerberosUsernamePasswordAuthenticator;
import org.keycloak.federation.kerberos.impl.SPNEGOAuthenticator;
import org.keycloak.federation.ldap.kerberos.LDAPProviderKerberosConfig;
import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KerberosConstants;
import org.picketlink.idm.IdentityManagementException;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.User;
import org.picketlink.idm.query.IdentityQuery;

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
    public static final String LDAP_ID = "LDAP_ID";
    public static final String SYNC_REGISTRATIONS = "syncRegistrations";
    public static final String EDIT_MODE = "editMode";

    protected LDAPFederationProviderFactory factory;
    protected KeycloakSession session;
    protected UserFederationProviderModel model;
    protected PartitionManager partitionManager;
    protected EditMode editMode;
    protected LDAPProviderKerberosConfig kerberosConfig;

    protected final Set<String> supportedCredentialTypes = new HashSet<String>();

    public LDAPFederationProvider(LDAPFederationProviderFactory factory, KeycloakSession session, UserFederationProviderModel model, PartitionManager partitionManager) {
        this.factory = factory;
        this.session = session;
        this.model = model;
        this.partitionManager = partitionManager;
        this.kerberosConfig = new LDAPProviderKerberosConfig(model);
        String editModeString = model.getConfig().get(EDIT_MODE);
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

    private ModelException convertIDMException(IdentityManagementException ie) {
        Throwable realCause = ie;
        while (realCause.getCause() != null) {
            realCause = realCause.getCause();
        }

        // Use the message from the realCause
        return new ModelException(realCause.getMessage(), ie);
    }

    public KeycloakSession getSession() {
        return session;
    }

    public UserFederationProviderModel getModel() {
        return model;
    }

    public PartitionManager getPartitionManager() {
        return partitionManager;
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
        return "true".equalsIgnoreCase(model.getConfig().get(SYNC_REGISTRATIONS)) && editMode == EditMode.WRITABLE;
    }

    @Override
    public UserModel register(RealmModel realm, UserModel user) {
        if (editMode == EditMode.READ_ONLY || editMode == EditMode.UNSYNCED) throw new IllegalStateException("Registration is not supported by this ldap server");;
        if (!synchronizeRegistrations()) throw new IllegalStateException("Registration is not supported by this ldap server");

        try {
            User picketlinkUser = LDAPUtils.addUser(this.partitionManager, user.getUsername(), user.getFirstName(), user.getLastName(), user.getEmail());
            user.setAttribute(LDAP_ID, picketlinkUser.getId());
            return proxy(user);
        } catch (IdentityManagementException ie) {
            throw convertIDMException(ie);
        }

    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        if (editMode == EditMode.READ_ONLY || editMode == EditMode.UNSYNCED) {
            logger.warnf("User '%s' can't be deleted in LDAP as editMode is '%s'", user.getUsername(), editMode.toString());
            return false;
        }

        try {
            return LDAPUtils.removeUser(partitionManager, user.getUsername());
        } catch (IdentityManagementException ie) {
            throw convertIDMException(ie);
        }
    }

    @Override
    public List<UserModel> searchByAttributes(Map<String, String> attributes, RealmModel realm, int maxResults) {
        List<UserModel> searchResults =new LinkedList<UserModel>();
        try {
            Map<String, User> plUsers = searchPicketlink(attributes, maxResults);
            for (User user : plUsers.values()) {
                if (session.userStorage().getUserByUsername(user.getLoginName(), realm) == null) {
                    UserModel imported = importUserFromPicketlink(realm, user);
                    searchResults.add(imported);
                }
            }
        } catch (IdentityManagementException ie) {
            throw convertIDMException(ie);
        }
        return searchResults;
    }

    protected Map<String, User> searchPicketlink(Map<String, String> attributes, int maxResults) {
        IdentityManager identityManager = getIdentityManager();
        Map<String, User> results = new HashMap<String, User>();
        if (attributes.containsKey(USERNAME)) {
            User user = BasicModel.getUser(identityManager, attributes.get(USERNAME));
            if (user != null) {
                results.put(user.getLoginName(), user);
            }
        }

        if (attributes.containsKey(EMAIL)) {
            User user = queryByEmail(identityManager, attributes.get(EMAIL));
            if (user != null) {
                results.put(user.getLoginName(), user);
            }
        }

        if (attributes.containsKey(FIRST_NAME) || attributes.containsKey(LAST_NAME)) {
            IdentityQuery<User> query = identityManager.createIdentityQuery(User.class);
            if (attributes.containsKey(FIRST_NAME)) {
                query.setParameter(User.FIRST_NAME, attributes.get(FIRST_NAME));
            }
            if (attributes.containsKey(LAST_NAME)) {
                query.setParameter(User.LAST_NAME, attributes.get(LAST_NAME));
            }
            query.setLimit(maxResults);
            List<User> agents = query.getResultList();
            for (User user : agents) {
                results.put(user.getLoginName(), user);
            }
        }

        return results;
    }

    @Override
    public boolean isValid(UserModel local) {
        try {
            User picketlinkUser = LDAPUtils.getUser(partitionManager, local.getUsername());
            if (picketlinkUser == null) {
                return false;
            }
            return picketlinkUser.getId().equals(local.getAttribute(LDAP_ID));
        } catch (IdentityManagementException ie) {
            throw convertIDMException(ie);
        }
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        try {
            User picketlinkUser = LDAPUtils.getUser(partitionManager, username);
            if (picketlinkUser == null) {
                return null;
            }

            // KEYCLOAK-808: Should we allow case-sensitivity to be configurable?
            if (!username.equals(picketlinkUser.getLoginName())) {
                logger.warnf("User found in LDAP but with different username. LDAP username: %s, Searched username: %s", username, picketlinkUser.getLoginName());
                return null;
            }

            return importUserFromPicketlink(realm, picketlinkUser);
        } catch (IdentityManagementException ie) {
            throw convertIDMException(ie);
        }
    }

    public IdentityManager getIdentityManager() {
        return partitionManager.createIdentityManager();
    }

    protected UserModel importUserFromPicketlink(RealmModel realm, User picketlinkUser) {
        String email = (picketlinkUser.getEmail() != null && picketlinkUser.getEmail().trim().length() > 0) ? picketlinkUser.getEmail() : null;

        if (picketlinkUser.getLoginName() == null) {
            throw new ModelException("User returned from LDAP has null username! Check configuration of your LDAP mappings. ID of user from LDAP: " + picketlinkUser.getId());
        }

        UserModel imported = session.userStorage().addUser(realm, picketlinkUser.getLoginName());
        imported.setEnabled(true);
        imported.setEmail(email);
        imported.setFirstName(picketlinkUser.getFirstName());
        imported.setLastName(picketlinkUser.getLastName());
        imported.setFederationLink(model.getId());
        imported.setAttribute(LDAP_ID, picketlinkUser.getId());

        logger.debugf("Added new user from LDAP. Username: " + imported.getUsername() + ", Email: ", imported.getEmail() + ", LDAP_ID: " + picketlinkUser.getId());
        return proxy(imported);
    }

    protected User queryByEmail(IdentityManager identityManager, String email) throws IdentityManagementException {
        return LDAPUtils.getUserByEmail(identityManager, email);
    }


    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        IdentityManager identityManager = getIdentityManager();

        try {
            User picketlinkUser = queryByEmail(identityManager, email);
            if (picketlinkUser == null) {
                return null;
            }

            // KEYCLOAK-808: Should we allow case-sensitivity to be configurable?
            if (!email.equals(picketlinkUser.getEmail())) {
                logger.warnf("User found in LDAP but with different email. LDAP email: %s, Searched email: %s", email, picketlinkUser.getEmail());
                return null;
            }

            return importUserFromPicketlink(realm, picketlinkUser);
        } catch (IdentityManagementException ie) {
            throw convertIDMException(ie);
        }
    }

    @Override
    public void preRemove(RealmModel realm) {
        // complete Don't think we have to do anything
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        // complete I don't think we have to do anything here
    }

    public boolean validPassword(String username, String password) {
        if (kerberosConfig.isAllowKerberosAuthentication() && kerberosConfig.isUseKerberosForPasswordAuthentication()) {
            // Use Kerberos JAAS (Krb5LoginModule)
            KerberosUsernamePasswordAuthenticator authenticator = factory.createKerberosUsernamePasswordAuthenticator(kerberosConfig);
            return authenticator.validUser(username, password);
        } else {
            // Use Naming LDAP API
            try {
                return LDAPUtils.validatePassword(partitionManager, username, password);
            } catch (IdentityManagementException ie) {
                throw convertIDMException(ie);
            }
        }
    }


    @Override
    public boolean validCredentials(RealmModel realm, UserModel user, List<UserCredentialModel> input) {
        for (UserCredentialModel cred : input) {
            if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
                return validPassword(user.getUsername(), cred.getValue());
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

                if (spnegoAuthenticator.isAuthenticated()) {
                    Map<String, Object> state = new HashMap<String, Object>();
                    state.put(KerberosConstants.GSS_DELEGATION_CREDENTIAL, spnegoAuthenticator.getDelegationCredential());

                    // TODO: This assumes that LDAP "uid" is equal to kerberos principal name. Like uid "hnelson" and kerberos principal "hnelson@KEYCLOAK.ORG".
                    // Check if it's correct or if LDAP attribute for mapping kerberos principal should be available (For ApacheDS it seems to be attribute "krb5PrincipalName" but on MSAD it's likely different)
                    String username = spnegoAuthenticator.getAuthenticatedUsername();
                    UserModel user = findOrCreateAuthenticatedUser(realm, username);

                    return new CredentialValidationOutput(user, CredentialValidationOutput.Status.AUTHENTICATED, state);
                }  else {
                    Map<String, Object> state = new HashMap<String, Object>();
                    state.put(KerberosConstants.RESPONSE_TOKEN, spnegoAuthenticator.getResponseToken());
                    return new CredentialValidationOutput(null, CredentialValidationOutput.Status.CONTINUE, state);
                }
            }
        }

        return CredentialValidationOutput.failed();
    }

    @Override
    public void close() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    protected void importPicketlinkUsers(RealmModel realm, List<User> users, UserFederationProviderModel fedModel) {
        for (User picketlinkUser : users) {
            String username = picketlinkUser.getLoginName();
            UserModel currentUser = session.userStorage().getUserByUsername(username, realm);

            if (currentUser == null) {
                // Add new user to Keycloak
                importUserFromPicketlink(realm, picketlinkUser);
            } else {
                if ((fedModel.getId().equals(currentUser.getFederationLink())) && (picketlinkUser.getId().equals(currentUser.getAttribute(LDAPFederationProvider.LDAP_ID)))) {
                    // Update keycloak user
                    String email = (picketlinkUser.getEmail() != null && picketlinkUser.getEmail().trim().length() > 0) ? picketlinkUser.getEmail() : null;
                    currentUser.setEmail(email);
                    currentUser.setFirstName(picketlinkUser.getFirstName());
                    currentUser.setLastName(picketlinkUser.getLastName());
                    logger.debugf("Updated user from LDAP: %s", currentUser.getUsername());
                } else {
                    logger.warnf("User '%s' is not updated during sync as he is not linked to federation provider '%s'", username, fedModel.getDisplayName());
                }
            }
        }
    }

    /**
     * Called after successful kerberos authentication
     *
     * @param realm
     * @param username username without realm prefix
     * @return
     */
    protected UserModel findOrCreateAuthenticatedUser(RealmModel realm, String username) {
        UserModel user = session.userStorage().getUserByUsername(username, realm);
        if (user != null) {
            logger.debug("Kerberos authenticated user " + username + " found in Keycloak storage");
            if (!isValid(user)) {
                throw new IllegalStateException("User with username " + username + " already exists, but is not linked to provider [" + model.getDisplayName() +
                        "] or LDAP_ID is not correct. LDAP_ID on user is: " + user.getAttribute(LDAP_ID));
            }

            return proxy(user);
        } else {
            // Creating user to local storage
            return getUserByUsername(realm, username);
        }
    }
}
