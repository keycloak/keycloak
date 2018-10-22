package org.keycloak.performance.templates.idm.authorization;

import static java.util.stream.Collectors.toSet;
import org.keycloak.performance.dataset.idm.authorization.ResourceServer;
import org.keycloak.performance.dataset.idm.authorization.ScopePermission;
import org.keycloak.performance.iteration.RandomSublist;
import org.keycloak.performance.util.ValidateNumber;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class ScopePermissionTemplate extends PolicyTemplate<ScopePermission, ScopePermissionRepresentation> {

    public static final String SCOPE_PERMISSIONS_PER_RESOURCE_SERVER = "scopePermissionsPerResourceServer";
    public static final String SCOPES_PER_SCOPE_PERMISSION = "scopesPerScopePermission";
    public static final String POLICIES_PER_SCOPE_PERMISSION = "policiesPerScopePermission";

    public final int scopePermissionsPerResourceServer;
    public final int scopesPerScopePermission;
    public final int policiesPerScopePermission;

    public ScopePermissionTemplate(ResourceServerTemplate resourceServerTemplate) {
        super(resourceServerTemplate);
        this.scopePermissionsPerResourceServer = getConfiguration().getInt(SCOPE_PERMISSIONS_PER_RESOURCE_SERVER, 0);
        this.scopesPerScopePermission = getConfiguration().getInt(SCOPES_PER_SCOPE_PERMISSION, 0); // should be 1 but that doesn't work with 0 resource servers
        this.policiesPerScopePermission = getConfiguration().getInt(POLICIES_PER_SCOPE_PERMISSION, 0);
    }

    @Override
    public int getEntityCountPerParent() {
        return scopePermissionsPerResourceServer;
    }

    @Override
    public void validateConfiguration() {
        logger().info(String.format("%s: %s", SCOPE_PERMISSIONS_PER_RESOURCE_SERVER, scopePermissionsPerResourceServer));
        ValidateNumber.minValue(scopePermissionsPerResourceServer, 0);

        logger().info(String.format("%s: %s", SCOPES_PER_SCOPE_PERMISSION, scopesPerScopePermission));
        ValidateNumber.isInRange(scopesPerScopePermission, 0, resourceServerTemplate().scopeTemplate.scopesPerResourceServer); // TODO should be >=1 but that doesn't work with 0 resource servers

        logger().info(String.format("%s: %s", POLICIES_PER_SCOPE_PERMISSION, policiesPerScopePermission));
        ValidateNumber.isInRange(policiesPerScopePermission, 0, resourceServerTemplate().maxPolicies);
    }

    @Override
    public ScopePermission newEntity(ResourceServer parentEntity, int index) {
        return new ScopePermission(parentEntity, index);
    }

    @Override
    public void processMappings(ScopePermission permission) {

        permission.setScopes(new RandomSublist<>(
                permission.getResourceServer().getScopes(), // original list
                permission.hashCode(), // random seed
                scopesPerScopePermission, // sublist size
                false // unique randoms?
        ));
        permission.getRepresentation().setScopes(
                permission.getScopes().stream()
                        .map(r -> r.getId()).filter(id -> id != null).collect(toSet())
        );

        permission.setPolicies(new RandomSublist<>(
                permission.getResourceServer().getAllPolicies(), // original list
                permission.hashCode(), // random seed
                policiesPerScopePermission, // sublist size
                false // unique randoms?
        ));
        permission.getRepresentation().setPolicies(permission.getPolicies()
                .stream().map(p -> p.getId())
                .filter(id -> id != null) // need non-null policy IDs
                .collect(toSet()));
    }

}
