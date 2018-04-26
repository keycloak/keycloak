package org.keycloak.performance.templates.idm.authorization;

import java.util.List;
import static java.util.stream.Collectors.toSet;
import org.apache.commons.lang.Validate;
import org.keycloak.performance.dataset.idm.User;
import org.keycloak.performance.dataset.idm.authorization.Resource;
import org.keycloak.performance.dataset.idm.authorization.ResourceServer;
import org.keycloak.performance.iteration.RandomSublist;
import org.keycloak.performance.templates.NestedEntityTemplate;
import org.keycloak.performance.util.ValidateNumber;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class ResourceTemplate extends NestedEntityTemplate<ResourceServer, Resource, ResourceRepresentation> {

    public static final String RESOURCES_PER_RESOURCE_SERVER = "resourcesPerResourceServer";
    public static final String SCOPES_PER_RESOURCE = "scopesPerResource";

    public final int resourcesPerResourceServer;
    public final int scopesPerResource;

    public ResourceTemplate(ResourceServerTemplate resourceServerTemplate) {
        super(resourceServerTemplate);
        this.resourcesPerResourceServer = getConfiguration().getInt(RESOURCES_PER_RESOURCE_SERVER, 0);
        this.scopesPerResource = getConfiguration().getInt(SCOPES_PER_RESOURCE, 0);
    }

    @Override
    public int getEntityCountPerParent() {
        return resourcesPerResourceServer;
    }

    @Override
    public void validateConfiguration() {
        logger().info(String.format("%s: %s", RESOURCES_PER_RESOURCE_SERVER, resourcesPerResourceServer));
        ValidateNumber.minValue(resourcesPerResourceServer, 0);

        logger().info(String.format("%s: %s", SCOPES_PER_RESOURCE, scopesPerResource));
        ValidateNumber.isInRange(scopesPerResource, 0,
                ((ResourceServerTemplate) getParentEntityTemplate()).scopeTemplate.scopesPerResourceServer);
    }

    @Override
    public Resource newEntity(ResourceServer parentEntity, int index) {
        return new Resource(parentEntity, index);
    }

    @Override
    public void processMappings(Resource resource) {

        if (resource.getRepresentation().getOwnerManagedAccess()) {
            List<User> users = resource.getResourceServer().getClient().getRealm().getUsers();
            String ownerId = users.get(resource.indexBasedRandomInt(users.size())).getId(); // random user from the realm
            Validate.notNull(ownerId, "Unable to assign user as owner of resource. Id not set.");
            resource.getRepresentation().setOwner(ownerId);
        }

        resource.setScopes(new RandomSublist<>(
                resource.getResourceServer().getScopes(), // original list
                resource.hashCode(), // random seed
                scopesPerResource, // sublist size
                false // unique randoms?
        ));
        resource.getRepresentation().setScopes(
                resource.getScopes().stream().map(s -> s.getRepresentation()).collect(toSet()));
    }

}
