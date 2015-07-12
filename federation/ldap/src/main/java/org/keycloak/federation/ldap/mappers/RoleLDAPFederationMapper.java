package org.keycloak.federation.ldap.mappers;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.logging.Logger;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.idm.model.LDAPDn;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.query.Condition;
import org.keycloak.federation.ldap.idm.query.QueryParameter;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQueryConditionsBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.UserModelDelegate;

/**
 * Map realm roles or roles of particular client to LDAP roles
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RoleLDAPFederationMapper extends AbstractLDAPFederationMapper {

    private static final Logger logger = Logger.getLogger(RoleLDAPFederationMapper.class);

    // LDAP DN where are roles of this tree saved.
    public static final String ROLES_DN = "roles.dn";

    // Name of LDAP attribute, which is used in role objects for name and RDN of role. Usually it will be "cn"
    public static final String ROLE_NAME_LDAP_ATTRIBUTE = "role.name.ldap.attribute";

    // Name of LDAP attribute on role, which is used for membership mappings. Usually it will be "member"
    public static final String MEMBERSHIP_LDAP_ATTRIBUTE = "membership.ldap.attribute";

    // Object classes of the role object.
    public static final String ROLE_OBJECT_CLASSES = "role.object.classes";

    // Boolean option. If true, we will map LDAP roles to realm roles. If false, we will map to client roles (client specified by option CLIENT_ID)
    public static final String USE_REALM_ROLES_MAPPING = "use.realm.roles.mapping";

    // ClientId, which we want to map roles. Applicable just if "USE_REALM_ROLES_MAPPING" is false
    public static final String CLIENT_ID = "client.id";

    // See docs for Mode enum
    public static final String MODE = "mode";


    // List of IDs of UserFederationMapperModels where syncRolesFromLDAP was already called in this KeycloakSession. This is to improve performance
    // TODO: Rather address this with caching at LDAPIdentityStore level?
    private Set<String> rolesSyncedModels = new TreeSet<>();

    @Override
    public void onImportUserFromLDAP(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapUser, UserModel user, RealmModel realm, boolean isCreate) {
        syncRolesFromLDAP(mapperModel, ldapProvider, realm);

        Mode mode = getMode(mapperModel);

        // For now, import LDAP role mappings just during create
        if (mode == Mode.IMPORT && isCreate) {

            List<LDAPObject> ldapRoles = getLDAPRoleMappings(mapperModel, ldapProvider, ldapUser);

            // Import role mappings from LDAP into Keycloak DB
            String roleNameAttr = getRoleNameLdapAttribute(mapperModel);
            for (LDAPObject ldapRole : ldapRoles) {
                String roleName = ldapRole.getAttributeAsString(roleNameAttr);

                RoleContainerModel roleContainer = getTargetRoleContainer(mapperModel, realm);
                RoleModel role = roleContainer.getRole(roleName);

                logger.debugf("Granting role [%s] to user [%s] during import from LDAP", roleName, user.getUsername());
                user.grantRole(role);
            }
        }
    }

    @Override
    public void onRegisterUserToLDAP(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapUser, UserModel localUser, RealmModel realm) {
        syncRolesFromLDAP(mapperModel, ldapProvider, realm);
    }

    // Sync roles from LDAP tree and create them in local Keycloak DB (if they don't exist here yet)
    protected void syncRolesFromLDAP(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, RealmModel realm) {
        if (!rolesSyncedModels.contains(mapperModel.getId())) {
            logger.debugf("Syncing roles from LDAP into Keycloak DB. Mapper is [%s], LDAP provider is [%s]", mapperModel.getName(), ldapProvider.getModel().getDisplayName());

            LDAPQuery ldapQuery = createRoleQuery(mapperModel, ldapProvider);

            // Send query
            List<LDAPObject> ldapRoles = ldapQuery.getResultList();

            RoleContainerModel roleContainer = getTargetRoleContainer(mapperModel, realm);
            String rolesRdnAttr = getRoleNameLdapAttribute(mapperModel);
            for (LDAPObject ldapRole : ldapRoles) {
                String roleName = ldapRole.getAttributeAsString(rolesRdnAttr);

                if (roleContainer.getRole(roleName) == null) {
                    logger.infof("Syncing role [%s] from LDAP to keycloak DB", roleName);
                    roleContainer.addRole(roleName);
                }
            }

            rolesSyncedModels.add(mapperModel.getId());
        }
    }

    public LDAPQuery createRoleQuery(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider) {
        LDAPQuery ldapQuery = new LDAPQuery(ldapProvider);

        // For now, use same search scope, which is configured "globally" and used for user's search.
        ldapQuery.setSearchScope(ldapProvider.getLdapIdentityStore().getConfig().getSearchScope());

        String rolesDn = getRolesDn(mapperModel);
        ldapQuery.setSearchDn(rolesDn);

        Collection<String> roleObjectClasses = getRoleObjectClasses(mapperModel, ldapProvider);
        ldapQuery.addObjectClasses(roleObjectClasses);

        String rolesRdnAttr = getRoleNameLdapAttribute(mapperModel);
        String membershipAttr = getMembershipLdapAttribute(mapperModel);
        ldapQuery.addReturningLdapAttribute(rolesRdnAttr);
        ldapQuery.addReturningLdapAttribute(membershipAttr);

        return ldapQuery;
    }

    protected RoleContainerModel getTargetRoleContainer(UserFederationMapperModel mapperModel, RealmModel realm) {
        boolean realmRolesMapping = parseBooleanParameter(mapperModel, USE_REALM_ROLES_MAPPING);
        if (realmRolesMapping) {
            return realm;
        } else {
            String clientId = mapperModel.getConfig().get(CLIENT_ID);
            if (clientId == null) {
                throw new ModelException("Using client roles mapping is requested, but parameter client.id not found!");
            }
            ClientModel client = realm.getClientByClientId(clientId);
            if (client == null) {
                throw new ModelException("Can't found requested client with clientId: " + clientId);
            }
            return client;
        }
    }

    protected String getRolesDn(UserFederationMapperModel mapperModel) {
        String rolesDn = mapperModel.getConfig().get(ROLES_DN);
        if (rolesDn == null) {
            throw new ModelException("Roles DN is null! Check your configuration");
        }
        return rolesDn;
    }

    protected String getRoleNameLdapAttribute(UserFederationMapperModel mapperModel) {
        String rolesRdnAttr = mapperModel.getConfig().get(ROLE_NAME_LDAP_ATTRIBUTE);
        return rolesRdnAttr!=null ? rolesRdnAttr : LDAPConstants.CN;
    }

    protected String getMembershipLdapAttribute(UserFederationMapperModel mapperModel) {
        String membershipAttrName = mapperModel.getConfig().get(MEMBERSHIP_LDAP_ATTRIBUTE);
        return membershipAttrName!=null ? membershipAttrName : LDAPConstants.MEMBER;
    }

    protected Collection<String> getRoleObjectClasses(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider) {
        String objectClasses = mapperModel.getConfig().get(ROLE_OBJECT_CLASSES);
        if (objectClasses == null) {
            // For Active directory, the default is 'group' . For other servers 'groupOfNames'
            objectClasses = ldapProvider.getLdapIdentityStore().getConfig().isActiveDirectory() ? LDAPConstants.GROUP : LDAPConstants.GROUP_OF_NAMES;
        }
        String[] objClasses = objectClasses.split(",");

        Set<String> trimmed = new HashSet<>();
        for (String objectClass : objClasses) {
            objectClass = objectClass.trim();
            if (objectClass.length() > 0) {
                trimmed.add(objectClass);
            }
        }
        return trimmed;
    }

    private Mode getMode(UserFederationMapperModel mapperModel) {
        String modeString = mapperModel.getConfig().get(MODE);
        if (modeString == null || modeString.isEmpty()) {
            throw new ModelException("Mode is missing! Check your configuration");
        }

        return Enum.valueOf(Mode.class, modeString.toUpperCase());
    }

    public LDAPObject createLDAPRole(UserFederationMapperModel mapperModel, String roleName, LDAPFederationProvider ldapProvider) {
        LDAPObject ldapObject = new LDAPObject();
        String roleNameAttribute = getRoleNameLdapAttribute(mapperModel);
        ldapObject.setRdnAttributeName(roleNameAttribute);
        ldapObject.setObjectClasses(getRoleObjectClasses(mapperModel, ldapProvider));
        ldapObject.setSingleAttribute(roleNameAttribute, roleName);

        LDAPDn roleDn = LDAPDn.fromString(getRolesDn(mapperModel));
        roleDn.addFirst(roleNameAttribute, roleName);
        ldapObject.setDn(roleDn);

        logger.infof("Creating role [%s] to LDAP with DN [%s]", roleName, roleDn.toString());
        ldapProvider.getLdapIdentityStore().add(ldapObject);
        return ldapObject;
    }

    public void addRoleMappingInLDAP(UserFederationMapperModel mapperModel, String roleName, LDAPFederationProvider ldapProvider, LDAPObject ldapUser) {
        LDAPObject ldapRole = loadLDAPRoleByName(mapperModel, ldapProvider, roleName);
        if (ldapRole == null) {
            ldapRole = createLDAPRole(mapperModel, roleName, ldapProvider);
        }

        Set<String> memberships = getExistingMemberships(mapperModel, ldapRole);

        // Remove membership placeholder if present
        for (String membership : memberships) {
            if (membership.trim().length() == 0) {
                memberships.remove(membership);
                break;
            }
        }

        memberships.add(ldapUser.getDn().toString());
        ldapRole.setAttribute(getMembershipLdapAttribute(mapperModel), memberships);

        ldapProvider.getLdapIdentityStore().update(ldapRole);
    }

    public void deleteRoleMappingInLDAP(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapUser, LDAPObject ldapRole) {
        Set<String> memberships = getExistingMemberships(mapperModel, ldapRole);
        memberships.remove(ldapUser.getDn().toString());

        // Some membership placeholder needs to be always here as "member" is mandatory attribute on some LDAP servers. But not on active directory! (Empty membership is not allowed here)
        if (memberships.size() == 0 && !ldapProvider.getLdapIdentityStore().getConfig().isActiveDirectory()) {
            memberships.add(LDAPConstants.EMPTY_MEMBER_ATTRIBUTE_VALUE);
        }

        ldapRole.setAttribute(getMembershipLdapAttribute(mapperModel), memberships);
        ldapProvider.getLdapIdentityStore().update(ldapRole);
    }

    public LDAPObject loadLDAPRoleByName(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, String roleName) {
        LDAPQuery ldapQuery = createRoleQuery(mapperModel, ldapProvider);
        Condition roleNameCondition = new LDAPQueryConditionsBuilder().equal(new QueryParameter(getRoleNameLdapAttribute(mapperModel)), roleName);
        ldapQuery.where(roleNameCondition);
        return ldapQuery.getFirstResult();
    }

    protected Set<String> getExistingMemberships(UserFederationMapperModel mapperModel, LDAPObject ldapRole) {
        String memberAttrName = getMembershipLdapAttribute(mapperModel);
        Set<String> memberships = ldapRole.getAttributeAsSet(memberAttrName);
        if (memberships == null) {
            memberships = new HashSet<>();
        }
        return memberships;
    }

    protected List<LDAPObject> getLDAPRoleMappings(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapUser) {
        LDAPQuery ldapQuery = createRoleQuery(mapperModel, ldapProvider);
        String membershipAttr = getMembershipLdapAttribute(mapperModel);
        Condition membershipCondition = new LDAPQueryConditionsBuilder().equal(new QueryParameter(membershipAttr), ldapUser.getDn().toString());
        ldapQuery.where(membershipCondition);
        return ldapQuery.getResultList();
    }

    @Override
    public UserModel proxy(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapUser, UserModel delegate, RealmModel realm) {
        final Mode mode = getMode(mapperModel);

        // For IMPORT mode, all operations are performed against local DB
        if (mode == Mode.IMPORT) {
            return delegate;
        } else {
            return new LDAPRoleMappingsUserDelegate(delegate, mapperModel, ldapProvider, ldapUser, realm, mode);
        }
    }

    @Override
    public void beforeLDAPQuery(UserFederationMapperModel mapperModel, LDAPQuery query) {
    }



    public class LDAPRoleMappingsUserDelegate extends UserModelDelegate {

        private final UserFederationMapperModel mapperModel;
        private final LDAPFederationProvider ldapProvider;
        private final LDAPObject ldapUser;
        private final RealmModel realm;
        private final Mode mode;

        // Avoid loading role mappings from LDAP more times per-request
        private Set<RoleModel> cachedLDAPRoleMappings;

        public LDAPRoleMappingsUserDelegate(UserModel user, UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapUser,
                                            RealmModel realm, Mode mode) {
            super(user);
            this.mapperModel = mapperModel;
            this.ldapProvider = ldapProvider;
            this.ldapUser = ldapUser;
            this.realm = realm;
            this.mode = mode;
        }

        @Override
        public Set<RoleModel> getRealmRoleMappings() {
            RoleContainerModel roleContainer = getTargetRoleContainer(mapperModel, realm);
            if (roleContainer.equals(realm)) {
                Set<RoleModel> ldapRoleMappings = getLDAPRoleMappingsConverted(mapperModel, ldapProvider, ldapUser, roleContainer);

                if (mode == Mode.LDAP_ONLY) {
                    // Use just role mappings from LDAP
                    return ldapRoleMappings;
                } else {
                    // Merge mappings from both DB and LDAP
                    Set<RoleModel> modelRoleMappings = super.getRealmRoleMappings();
                    ldapRoleMappings.addAll(modelRoleMappings);
                    return ldapRoleMappings;
                }
            } else {
                return super.getRealmRoleMappings();
            }
        }

        @Override
        public Set<RoleModel> getClientRoleMappings(ClientModel client) {
            RoleContainerModel roleContainer = getTargetRoleContainer(mapperModel, realm);
            if (roleContainer.equals(client)) {
                Set<RoleModel> ldapRoleMappings = getLDAPRoleMappingsConverted(mapperModel, ldapProvider, ldapUser, roleContainer);

                if (mode == Mode.LDAP_ONLY) {
                    // Use just role mappings from LDAP
                    return ldapRoleMappings;
                } else {
                    // Merge mappings from both DB and LDAP
                    Set<RoleModel> modelRoleMappings = super.getClientRoleMappings(client);
                    ldapRoleMappings.addAll(modelRoleMappings);
                    return ldapRoleMappings;
                }
            } else {
                return super.getClientRoleMappings(client);
            }
        }

        @Override
        public boolean hasRole(RoleModel role) {
            Set<RoleModel> roles = getRoleMappings();
            return KeycloakModelUtils.hasRole(roles, role);
        }

        @Override
        public void grantRole(RoleModel role) {
            if (mode == Mode.LDAP_ONLY) {
                RoleContainerModel roleContainer = getTargetRoleContainer(mapperModel, realm);

                if (role.getContainer().equals(roleContainer)) {

                    // We need to create new role mappings in LDAP
                    cachedLDAPRoleMappings = null;
                    addRoleMappingInLDAP(mapperModel, role.getName(), ldapProvider, ldapUser);
                } else {
                    super.grantRole(role);
                }
            } else {
                super.grantRole(role);
            }
        }

        @Override
        public Set<RoleModel> getRoleMappings() {
            Set<RoleModel> modelRoleMappings = super.getRoleMappings();

            RoleContainerModel targetRoleContainer = getTargetRoleContainer(mapperModel, realm);
            Set<RoleModel> ldapRoleMappings = getLDAPRoleMappingsConverted(mapperModel, ldapProvider, ldapUser, targetRoleContainer);

            if (mode == Mode.LDAP_ONLY) {
                // For LDAP-only we want to retrieve role mappings of target container just from LDAP
                Set<RoleModel> modelRolesCopy = new HashSet<>(modelRoleMappings);
                for (RoleModel role : modelRolesCopy) {
                    if (role.getContainer().equals(targetRoleContainer)) {
                        modelRoleMappings.remove(role);
                    }
                }
            }

            modelRoleMappings.addAll(ldapRoleMappings);
            return modelRoleMappings;
        }

        protected Set<RoleModel> getLDAPRoleMappingsConverted(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, LDAPObject ldapUser, RoleContainerModel roleContainer) {
            if (cachedLDAPRoleMappings != null) {
                return new HashSet<>(cachedLDAPRoleMappings);
            }

            List<LDAPObject> ldapRoles = getLDAPRoleMappings(mapperModel, ldapProvider, ldapUser);

            Set<RoleModel> roles = new HashSet<>();
            String roleNameLdapAttr = getRoleNameLdapAttribute(mapperModel);
            for (LDAPObject role : ldapRoles) {
                String roleName = role.getAttributeAsString(roleNameLdapAttr);
                RoleModel modelRole = roleContainer.getRole(roleName);
                if (modelRole == null) {
                    // Add role to local DB
                    modelRole = roleContainer.addRole(roleName);
                }
                roles.add(modelRole);
            }

            cachedLDAPRoleMappings = new HashSet<>(roles);

            return roles;
        }

        @Override
        public void deleteRoleMapping(RoleModel role) {
            RoleContainerModel roleContainer = getTargetRoleContainer(mapperModel, realm);
            if (role.getContainer().equals(roleContainer)) {

                LDAPQuery ldapQuery = createRoleQuery(mapperModel, ldapProvider);
                LDAPQueryConditionsBuilder conditionsBuilder = new LDAPQueryConditionsBuilder();
                Condition roleNameCondition = conditionsBuilder.equal(new QueryParameter(getRoleNameLdapAttribute(mapperModel)), role.getName());
                Condition membershipCondition = conditionsBuilder.equal(new QueryParameter(getMembershipLdapAttribute(mapperModel)), ldapUser.getDn().toString());
                ldapQuery.where(roleNameCondition).where(membershipCondition);
                LDAPObject ldapRole = ldapQuery.getFirstResult();

                if (ldapRole == null) {
                    // Role mapping doesn't exist in LDAP. For LDAP_ONLY mode, we don't need to do anything. For READ_ONLY, delete it in local DB.
                    if (mode == Mode.READ_ONLY) {
                        super.deleteRoleMapping(role);
                    }
                } else {
                    // Role mappings exists in LDAP. For LDAP_ONLY mode, we can just delete it in LDAP. For READ_ONLY we can't delete it -> throw error
                    if (mode == Mode.READ_ONLY) {
                        throw new ModelException("Not possible to delete LDAP role mappings as mapper mode is READ_ONLY");
                    } else {
                        // Delete ldap role mappings
                        cachedLDAPRoleMappings = null;
                        deleteRoleMappingInLDAP(mapperModel, ldapProvider, ldapUser, ldapRole);
                    }
                }
            } else {
                super.deleteRoleMapping(role);
            }
        }
    }

    public enum Mode {
        /**
         * All role mappings are retrieved from LDAP and saved into LDAP
         */
        LDAP_ONLY,

        /**
         * Read-only LDAP mode. Role mappings are retrieved from LDAP for particular user just at the time when he is imported and then
         * they are saved to local keycloak DB. Then all role mappings are always retrieved from keycloak DB, never from LDAP.
         * Creating or deleting of role mapping is propagated only to DB.
         *
         * This is read-only mode LDAP mode and it's good for performance, but when user is put to some role directly in LDAP, it
         * won't be seen by Keycloak
         */
        IMPORT,

        /**
         * Read-only LDAP mode. Role mappings are retrieved from both LDAP and DB and merged together. New role grants are not saved to LDAP but to DB.
         * Deleting role mappings, which is mapped to LDAP, will throw an error.
         */
        READ_ONLY
    }
}
