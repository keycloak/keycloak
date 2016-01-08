package org.keycloak.federation.ldap.mappers;

import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.UserModelDelegate;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class HardcodedLDAPRoleMapper extends AbstractLDAPFederationMapper {

    private static final Logger logger = Logger.getLogger(HardcodedLDAPRoleMapper.class);

    public static final String ROLE = "role";

    public HardcodedLDAPRoleMapper(UserFederationMapperModel mapperModel, LDAPFederationProvider ldapProvider, RealmModel realm) {
        super(mapperModel, ldapProvider, realm);
    }

    @Override
    public void beforeLDAPQuery(LDAPQuery query) {
    }

    @Override
    public UserModel proxy(LDAPObject ldapUser, UserModel delegate) {
        return new UserModelDelegate(delegate) {

            @Override
            public Set<RoleModel> getRealmRoleMappings() {
                Set<RoleModel> roles = super.getRealmRoleMappings();

                RoleModel role = getRole();
                if (role != null && role.getContainer().equals(realm)) {
                    roles.add(role);
                }

                return roles;
            }

            @Override
            public Set<RoleModel> getClientRoleMappings(ClientModel app) {
                Set<RoleModel> roles = super.getClientRoleMappings(app);

                RoleModel role = getRole();
                if (role != null && role.getContainer().equals(app)) {
                    roles.add(role);
                }

                return roles;
            }

            @Override
            public boolean hasRole(RoleModel role) {
                return super.hasRole(role) || role.equals(getRole());
            }

            @Override
            public Set<RoleModel> getRoleMappings() {
                Set<RoleModel> roles = super.getRoleMappings();

                RoleModel role = getRole();
                if (role != null) {
                    roles.add(role);
                }

                return roles;
            }

            @Override
            public void deleteRoleMapping(RoleModel role) {
                if (role.equals(getRole())) {
                    throw new ModelException("Not possible to delete role. It's hardcoded by LDAP mapper");
                } else {
                    super.deleteRoleMapping(role);
                }
            }
        };
    }

    @Override
    public void onRegisterUserToLDAP(LDAPObject ldapUser, UserModel localUser) {

    }

    @Override
    public void onImportUserFromLDAP(LDAPObject ldapUser, UserModel user, boolean isCreate) {

    }

    private RoleModel getRole() {
        String roleName = mapperModel.getConfig().get(HardcodedLDAPRoleMapper.ROLE);
        RoleModel role = KeycloakModelUtils.getRoleFromString(realm, roleName);
        if (role == null) {
            logger.warnf("Hardcoded role '%s' configured in mapper '%s' is not available anymore");
        }
        return role;
    }
}
