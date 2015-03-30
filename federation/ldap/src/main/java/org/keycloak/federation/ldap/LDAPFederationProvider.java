package org.keycloak.federation.ldap;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.picketlink.idm.IdentityManagementException;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.RelationshipManager;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.Grant;
import org.picketlink.idm.model.basic.Role;
import org.picketlink.idm.model.basic.User;
import org.picketlink.idm.query.IdentityQuery;
import org.picketlink.idm.query.RelationshipQuery;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:jli@vizuri.com">Jiehuan Li</a>
 * @version $Revision: 1 $
 */
public class LDAPFederationProvider implements UserFederationProvider {
    private static final Logger logger = Logger.getLogger(LDAPFederationProvider.class);
    public static final String LDAP_ID = "LDAP_ID";
    public static final String SYNC_REGISTRATIONS = "syncRegistrations";
    public static final String EDIT_MODE = "editMode";

    protected KeycloakSession session;
    protected UserFederationProviderModel model;
    protected PartitionManager partitionManager;
    protected EditMode editMode;

    protected static final Set<String> supportedCredentialTypes = new HashSet<String>();

    static
    {
        supportedCredentialTypes.add(UserCredentialModel.PASSWORD);
    }

    public LDAPFederationProvider(KeycloakSession session, UserFederationProviderModel model, PartitionManager partitionManager) {
        this.session = session;
        this.model = model;
        this.partitionManager = partitionManager;
        String editModeString = model.getConfig().get(EDIT_MODE);
        if (editModeString == null) {
            editMode = EditMode.READ_ONLY;
        } else {
            editMode = EditMode.valueOf(editModeString);
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
        if (editMode == EditMode.UNSYNCED ) {
            for (UserCredentialValueModel cred : local.getCredentialsDirectly()) {
                if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
                    return Collections.emptySet();
                }
            }
        }
        return supportedCredentialTypes;
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
        try {
            return LDAPUtils.validatePassword(partitionManager, username, password);
        } catch (IdentityManagementException ie) {
            throw convertIDMException(ie);
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
    public void close() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    protected void importPicketlinkUsers(RealmModel realm, List<User> users, UserFederationProviderModel fedModel) {
        for (User picketlinkUser : users) {
            importPicketlinkUser(realm, picketlinkUser, fedModel);
        }
    }
    
    @Override
	public RoleModel createRole(RealmModel realm, RoleModel role) {
		if (editMode == EditMode.READ_ONLY || editMode == EditMode.UNSYNCED) throw new IllegalStateException("Role creation is not supported by this ldap server");

        try {
            Role picketlinkRole = LDAPUtils.addRole(this.partitionManager, role.getName());
            role.setAttribute(LDAP_ID, picketlinkRole.getId());
            return proxy(role);
        } catch (IdentityManagementException ie) {
            throw convertIDMException(ie);
        }
	}
	
	@Override
	public void grantRole(RealmModel realm, UserModel user, RoleModel role) {
		if (editMode == EditMode.READ_ONLY || editMode == EditMode.UNSYNCED) throw new IllegalStateException("Role mapping synchronization to ldap is not supported by this ldap server");

        try {
            LDAPUtils.grantRole(this.partitionManager, user.getUsername(), role.getName());
        } catch (IdentityManagementException ie) {
            throw convertIDMException(ie);
        }
	}

	@Override
	public void revokeRole(RealmModel realm, UserModel user, RoleModel role) {
		if (editMode == EditMode.READ_ONLY || editMode == EditMode.UNSYNCED) throw new IllegalStateException("Role mapping synchronization to ldap is not supported by this ldap server");

        try {
            LDAPUtils.revokeRole(this.partitionManager, user.getUsername(), role.getName());
        } catch (IdentityManagementException ie) {
            throw convertIDMException(ie);
        }
	}

	@Override
	public boolean removeRole(RealmModel realm, RoleModel role) {
		if (editMode == EditMode.READ_ONLY || editMode == EditMode.UNSYNCED) throw new IllegalStateException("Role deletion is not supported by this ldap server");

        try {
            return LDAPUtils.removeRole(this.partitionManager, role.getName());
        } catch (IdentityManagementException ie) {
            throw convertIDMException(ie);
        }
	}
	
	protected void importPicketlinkRoles(RealmModel realm, List<Role> roles, UserFederationProviderModel fedModel) {
        for (Role picketlinkRole : roles) {
            String roleName = picketlinkRole.getName();
            RoleModel currentRole = null;
            currentRole = getRole(realm, roleName);
            
            if (currentRole == null) {
                // Add new role to Keycloak
                importRoleFromPicketlink(realm, picketlinkRole);
                logger.debugf("Added new role from LDAP: %s", roleName);
            } else {
                if ((fedModel.getId().equals(currentRole.getFederationLink())) && (picketlinkRole.getId().equals(currentRole.getAttribute(LDAPFederationProvider.LDAP_ID)))) {
                    // Update keycloak role
                    logger.debugf("Nothing to update for role from LDAP: %s", roleName);
                } else {
                    logger.warnf("Role '%s' is not updated during sync as this role is not linked to federation provider '%s'", roleName, fedModel.getDisplayName());
                }
            }
        }
    }

	private RoleModel getRole(RealmModel realm, String rolename) {
		Set<RoleModel> allRoles = realm.getRoles();
		for (RoleModel role : allRoles) {
			if (role.getName().equals(rolename)) {
				return role;
			}
		}
		return null;
	}
    
    protected RoleModel importRoleFromPicketlink(RealmModel realm, Role picketlinkRole) {

        if (picketlinkRole.getName() == null) {
            throw new ModelException("Role returned from LDAP has null rolename! Check configuration of your LDAP mappings. ID of role from LDAP: " + picketlinkRole.getId());
        }

        RoleModel imported = realm.addRole(picketlinkRole.getName());
        imported.setAttribute(LDAP_ID, picketlinkRole.getId());
        return proxy(imported);
    }
    
    public RoleModel proxy(RoleModel local) {
        switch (editMode) {
            case READ_ONLY:
               return new ReadonlyLDAPRoleModelDelegate(local, this);
            case WRITABLE:
               return new WritableLDAPRoleModelDelegate(local, this);
            case UNSYNCED:
               return new UnsyncedLDAPRoleModelDelegate(local, this);
        }
       return local;
   }
    
    protected void importPicketlinkUsers(RealmModel realm, List<User> users, UserFederationProviderModel fedModel, RelationshipManager relationshipManager) {
        for (User picketlinkUser : users) {
            UserModel currentUser = importPicketlinkUser(realm, picketlinkUser, fedModel);
            
            // Grant roles to this user based on ldap member or memberof information.
            RelationshipQuery<Grant> grantQuery = relationshipManager.createRelationshipQuery(Grant.class);            
            grantQuery.setParameter(Grant.ASSIGNEE, picketlinkUser);
            List<Grant> grants = grantQuery.getResultList();

            // Remove all keycloak existing realm role mappings for this user
            for (RoleModel role : currentUser.getRealmRoleMappings()) {
            	currentUser.deleteRoleMapping(role);
            }
            
            for (Grant grant : grants) {
            	// Iterating over the user roles
            	Role picketlinkRole = grant.getRole();
            	if (picketlinkRole != null) {
            		RoleModel role = getRole(realm, picketlinkRole.getName());
            		if (role != null)
            			currentUser.grantRole(role);
            		else
            			logger.errorf("Can not find matching keycloak role for picketlink role '%s' for user '%s'", picketlinkRole.getName(), currentUser.getUsername());
            	} else {
            		logger.errorf("Picketlink returned a 'null' role in grant for user '%s'", currentUser.getUsername());
            	}
            }
        }
    }
    
    protected UserModel importPicketlinkUser(RealmModel realm, User picketlinkUser, UserFederationProviderModel fedModel) {
    	String username = picketlinkUser.getLoginName();
    	UserModel currentUser = session.userStorage().getUserByUsername(username, realm);

    	if (currentUser == null) {
    		// Add new user to Keycloak
    		importUserFromPicketlink(realm, picketlinkUser);
    		logger.debugf("Added new user from LDAP: %s", username);
    		currentUser = session.userStorage().getUserByUsername(username, realm);
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
    	
    	return currentUser;
    }
    
    @Override
    public boolean supportRoles() {
        return "true".equalsIgnoreCase(model.getConfig().get(LDAPConstants.SUPPORT_ROLES));
    }
}
