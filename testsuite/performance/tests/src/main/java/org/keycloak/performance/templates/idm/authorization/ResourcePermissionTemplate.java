package org.keycloak.performance.templates.idm.authorization;

import static java.util.stream.Collectors.toSet;
import org.keycloak.performance.dataset.idm.authorization.ResourceServer;
import org.keycloak.performance.dataset.idm.authorization.ResourcePermission;
import org.keycloak.performance.iteration.RandomSublist;
import org.keycloak.performance.util.ValidateNumber;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class ResourcePermissionTemplate extends PolicyTemplate<ResourcePermission, ResourcePermissionRepresentation> {

    public static final String RESOURCE_PERMISSIONS_PER_RESOURCE_SERVER = "resourcePermissionsPerResourceServer";
    public static final String RESOURCES_PER_RESOURCE_PERMISSION = "resourcesPerResourcePermission";
    public static final String POLICIES_PER_RESOURCE_PERMISSION = "policiesPerResourcePermission";

    public final int resourcePermissionsPerResourceServer;
    public final int resourcesPerResourcePermission;
    public final int policiesPerResourcePermission;

    public ResourcePermissionTemplate(ResourceServerTemplate resourceServerTemplate) {
        super(resourceServerTemplate);
        this.resourcePermissionsPerResourceServer = getConfiguration().getInt(RESOURCE_PERMISSIONS_PER_RESOURCE_SERVER, 0);
        this.resourcesPerResourcePermission = getConfiguration().getInt(RESOURCES_PER_RESOURCE_PERMISSION, 0); // should be 1 but that doesn't work with 0 resource servers
        this.policiesPerResourcePermission = getConfiguration().getInt(POLICIES_PER_RESOURCE_PERMISSION, 0);
    }

    @Override
    public int getEntityCountPerParent() {
        return resourcePermissionsPerResourceServer;
    }

    @Override
    public void validateConfiguration() {
        logger().info(String.format("%s: %s", RESOURCE_PERMISSIONS_PER_RESOURCE_SERVER, resourcePermissionsPerResourceServer));
        ValidateNumber.minValue(resourcePermissionsPerResourceServer, 0);

        logger().info(String.format("%s: %s", RESOURCES_PER_RESOURCE_PERMISSION, resourcesPerResourcePermission));
        ValidateNumber.isInRange(resourcesPerResourcePermission, 0, resourceServerTemplate().resourceTemplate.resourcesPerResourceServer); // TODO should be >=1 but that doesn't work with 0 resource servers

        logger().info(String.format("%s: %s", POLICIES_PER_RESOURCE_PERMISSION, policiesPerResourcePermission));
        ValidateNumber.isInRange(policiesPerResourcePermission, 0, resourceServerTemplate().maxPolicies);
    }

    @Override
    public ResourcePermission newEntity(ResourceServer parentEntity, int index) {
        return new ResourcePermission(parentEntity, index);
    }

    @Override
    public void processMappings(ResourcePermission permission) {
        String resourceType = permission.getRepresentation().getResourceType();
        if (resourceType == null || "".equals(resourceType)) {
            permission.setResources(new RandomSublist<>(
                    permission.getResourceServer().getResources(), // original list
                    permission.hashCode(), // random seed
                    resourcesPerResourcePermission, // sublist size
                    false // unique randoms?
            ));
            permission.getRepresentation().setResources(
                    permission.getResources().stream()
                            .map(r -> r.getId()).filter(id -> id != null).collect(toSet())
            );
        }

        permission.setPolicies(new RandomSublist<>(
                permission.getResourceServer().getAllPolicies(), // original list
                permission.hashCode(), // random seed
                policiesPerResourcePermission, // sublist size
                false // unique randoms?
        ));
        permission.getRepresentation().setPolicies(permission.getPolicies()
                .stream().map(p -> p.getId())
                .filter(id -> id != null) // need non-null policy IDs
                .collect(toSet()));
    }

}
