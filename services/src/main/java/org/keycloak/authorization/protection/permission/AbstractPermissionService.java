package org.keycloak.authorization.protection.permission;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.admin.representation.ResourceRepresentation;
import org.keycloak.authorization.admin.representation.ScopeRepresentation;
import org.keycloak.authorization.common.KeycloakIdentity;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.protection.permission.representation.PermissionRequest;
import org.keycloak.authorization.protection.permission.representation.PermissionResponse;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.services.ErrorResponseException;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AbstractPermissionService {

    private final AuthorizationProvider authorization;
    private final KeycloakIdentity identity;
    private final ResourceServer resourceServer;

    public AbstractPermissionService(KeycloakIdentity identity, ResourceServer resourceServer, AuthorizationProvider authorization) {
        this.identity = identity;
        this.resourceServer = resourceServer;
        this.authorization = authorization;
    }

    public Response create(List<PermissionRequest> request) {
        if (request == null) {
            throw new ErrorResponseException("invalid_permission_request", "Invalid permission request.", Response.Status.BAD_REQUEST);
        }

        List<ResourceRepresentation> resource = verifyRequestedResource(request);

        return Response.status(Response.Status.CREATED).entity(new PermissionResponse(createPermissionTicket(resource))).build();
    }

    private List<ResourceRepresentation> verifyRequestedResource(List<PermissionRequest> request) {
        StoreFactory storeFactory = authorization.getStoreFactory();
        return request.stream().map(request1 -> {
            String resourceSetId = request1.getResourceSetId();
            String resourceSetName = request1.getResourceSetName();

            if (resourceSetId == null && resourceSetName == null) {
                throw new ErrorResponseException("invalid_resource_set_id", "Resource id or name not provided.", Response.Status.BAD_REQUEST);
            }

            Resource resource;

            if (resourceSetId != null) {
                resource = storeFactory.getResourceStore().findById(resourceSetId);
            } else {
                resource = storeFactory.getResourceStore().findByName(resourceSetName, this.resourceServer.getId());
            }

            if (resource == null) {
                if (resourceSetId != null) {
                    throw new ErrorResponseException("nonexistent_resource_set_id", "Resource set with id[" + resourceSetId + "] does not exists in this server.", Response.Status.BAD_REQUEST);
                } else {
                    throw new ErrorResponseException("nonexistent_resource_set_name", "Resource set with name[" + resourceSetName + "] does not exists in this server.", Response.Status.BAD_REQUEST);
                }
            }

            return new ResourceRepresentation(resource.getName(), verifyRequestedScopes(request1, resource));
        }).collect(Collectors.toList());
    }

    private Set<ScopeRepresentation> verifyRequestedScopes(PermissionRequest request, Resource resource) {
        return request.getScopes().stream().map(scopeName -> {
            for (Scope scope : resource.getScopes()) {
                if (scope.getName().equals(scopeName)) {
                    return new ScopeRepresentation(scopeName);
                }
            }

            for (Resource baseResource : authorization.getStoreFactory().getResourceStore().findByType(resource.getType())) {
                if (baseResource.getOwner().equals(resource.getResourceServer().getClientId())) {
                    for (Scope baseScope : baseResource.getScopes()) {
                        if (baseScope.getName().equals(scopeName)) {
                            return new ScopeRepresentation(scopeName);
                        }
                    }
                }
            }

            throw new ErrorResponseException("invalid_scope", "Scope [" + scopeName + " is not valid.", Response.Status.BAD_REQUEST);
        }).collect(Collectors.toSet());
    }

    private String createPermissionTicket(List<ResourceRepresentation> resources) {
        return new JWSBuilder().jsonContent(new PermissionTicket(resources, this.resourceServer.getId(), this.identity.getAccessToken()))
                .rsa256(this.authorization.getKeycloakSession().getContext().getRealm().getPrivateKey());
    }
}
