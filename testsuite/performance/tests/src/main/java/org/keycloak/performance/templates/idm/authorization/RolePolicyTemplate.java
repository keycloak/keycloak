package org.keycloak.performance.templates.idm.authorization;

import org.apache.commons.lang.Validate;
import org.keycloak.performance.dataset.Entity;
import org.keycloak.performance.dataset.idm.Role;
import org.keycloak.performance.dataset.idm.authorization.ResourceServer;
import org.keycloak.performance.dataset.idm.authorization.RolePolicyRoleDefinition;
import org.keycloak.performance.dataset.idm.authorization.RolePolicy;
import org.keycloak.performance.dataset.idm.authorization.RolePolicyRoleDefinitionSet;
import org.keycloak.performance.iteration.ListOfLists;
import org.keycloak.performance.iteration.RandomSublist;
import org.keycloak.performance.templates.NestedEntityTemplate;
import org.keycloak.performance.templates.NestedEntityTemplateWrapperList;
import org.keycloak.performance.templates.idm.ClientRoleTemplate;
import org.keycloak.performance.templates.idm.ClientTemplate;
import org.keycloak.performance.util.ValidateNumber;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class RolePolicyTemplate extends PolicyTemplate<RolePolicy, RolePolicyRepresentation> {

    public static final String ROLE_POLICIES_PER_RESOURCE_SERVER = "rolePoliciesPerResourceServer";

    public final int rolePoliciesPerResourceServer;

    public final RolePolicyRoleDefinitionTemplate roleDefinitionTemplate;

    public RolePolicyTemplate(ResourceServerTemplate resourceServerTemplate) {
        super(resourceServerTemplate);
        this.rolePoliciesPerResourceServer = getConfiguration().getInt(ROLE_POLICIES_PER_RESOURCE_SERVER, 0);
        this.roleDefinitionTemplate = new RolePolicyRoleDefinitionTemplate();
    }

    @Override
    public ResourceServerTemplate resourceServerTemplate() {
        return (ResourceServerTemplate) getParentEntityTemplate();
    }

    @Override
    public int getEntityCountPerParent() {
        return rolePoliciesPerResourceServer;
    }

    @Override
    public void validateConfiguration() {
        logger().info(String.format("%s: %s", ROLE_POLICIES_PER_RESOURCE_SERVER, rolePoliciesPerResourceServer));
        ValidateNumber.minValue(rolePoliciesPerResourceServer, 0);

        roleDefinitionTemplate.validateConfiguration();
    }

    @Override
    public RolePolicy newEntity(ResourceServer parentEntity, int index) {
        return new RolePolicy(parentEntity, index);
    }

    @Override
    public void processMappings(RolePolicy rolePolicy) {
        rolePolicy.setRoles(new ListOfLists<>(
                new RandomSublist(
                        rolePolicy.getResourceServer().getClient().getRealm().getRealmRoles(), // original list
                        rolePolicy.hashCode(), // random seed
                        roleDefinitionTemplate.realmRolesPerRolePolicy, // sublist size
                        false // unique randoms?
                ),
                new RandomSublist(
                        rolePolicy.getResourceServer().getClient().getRealm().getClientRoles(), // original list
                        rolePolicy.hashCode(), // random seed
                        roleDefinitionTemplate.clientRolesPerRolePolicy, // sublist size
                        false // unique randoms?
                )
        ));

        rolePolicy.getRepresentation().setRoles(new RolePolicyRoleDefinitionSet(
                new NestedEntityTemplateWrapperList<>(rolePolicy, roleDefinitionTemplate)
        ));
    }

    public class RolePolicyRoleDefinitionTemplate
            extends NestedEntityTemplate<RolePolicy, RolePolicyRoleDefinition, RolePolicyRepresentation.RoleDefinition> {

        public static final String REALM_ROLES_PER_ROLE_POLICY = "realmRolesPerRolePolicy";
        public static final String CLIENT_ROLES_PER_ROLE_POLICY = "clientRolesPerRolePolicy";

        public final int realmRolesPerRolePolicy;
        public final int clientRolesPerRolePolicy;

        public RolePolicyRoleDefinitionTemplate() {
            super(RolePolicyTemplate.this);
            this.realmRolesPerRolePolicy = getConfiguration().getInt(REALM_ROLES_PER_ROLE_POLICY, 0);
            this.clientRolesPerRolePolicy = getConfiguration().getInt(CLIENT_ROLES_PER_ROLE_POLICY, 0);
        }

        @Override
        public int getEntityCountPerParent() {
            return realmRolesPerRolePolicy + clientRolesPerRolePolicy;
        }

        @Override
        public void validateConfiguration() {
            logger().info(String.format("%s: %s", REALM_ROLES_PER_ROLE_POLICY, realmRolesPerRolePolicy));
            logger().info(String.format("%s: %s", CLIENT_ROLES_PER_ROLE_POLICY, clientRolesPerRolePolicy));

            ClientTemplate ct = resourceServerTemplate().clientTemplate();
            ClientRoleTemplate crt = ct.clientRoleTemplate;
            int realmRolesMax = ct.realmTemplate().realmRoleTemplate.realmRolesPerRealm;
            int clientRolesMax = ct.clientsPerRealm * crt.clientRolesPerClient;

            ValidateNumber.isInRange(realmRolesPerRolePolicy, 0, realmRolesMax);
            ValidateNumber.isInRange(clientRolesPerRolePolicy, 0, clientRolesMax);

        }

        @Override
        public RolePolicyRoleDefinition newEntity(RolePolicy rolePolicy, int index) {
            Validate.isTrue(rolePolicy.getRoles().size() == getEntityCountPerParent());
            String roleUUID = ((Role<Entity>) rolePolicy.getRoles().get(index)).getRepresentation().getId();
            return new RolePolicyRoleDefinition(rolePolicy, index, new RolePolicyRepresentation.RoleDefinition(roleUUID, false));
        }

        @Override
        public void processMappings(RolePolicyRoleDefinition entity) {
        }

    }

}
