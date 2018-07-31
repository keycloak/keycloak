package org.keycloak.performance.templates.idm;

import org.keycloak.performance.dataset.idm.Realm;
import org.keycloak.performance.dataset.idm.RealmRole;
import org.keycloak.performance.templates.NestedEntityTemplate;
import org.keycloak.performance.util.ValidateNumber;
import org.keycloak.representations.idm.RoleRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class RealmRoleTemplate extends NestedEntityTemplate<Realm, RealmRole, RoleRepresentation> {

    public static final String REALM_ROLES_PER_REALM = "realmRolesPerRealm";

    public final int realmRolesPerRealm;
    public final int realmRolesTotal;

    public RealmRoleTemplate(RealmTemplate realmTemplate) {
        super(realmTemplate);
        this.realmRolesPerRealm = getConfiguration().getInt(REALM_ROLES_PER_REALM, 0);
        this.realmRolesTotal = realmRolesPerRealm * realmTemplate.realms;
    }

    public RealmTemplate realmTemplate() {
        return (RealmTemplate) getParentEntityTemplate();
    }
    
    @Override
    public int getEntityCountPerParent() {
        return realmRolesPerRealm;
    }

    @Override
    public void validateConfiguration() {
        logger().info(String.format("%s: %s, total: %s", REALM_ROLES_PER_REALM, realmRolesPerRealm, realmRolesTotal));
        ValidateNumber.minValue(realmRolesPerRealm, 0);
    }

    @Override
    public RealmRole newEntity(Realm parentEntity, int index) {
        return new RealmRole(parentEntity, index);
    }

    @Override
    public void processMappings(RealmRole role) {
    }

}
