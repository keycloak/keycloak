package org.keycloak.authorization.entitlement.representation;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.authorization.authorization.representation.AuthorizationRequestMetadata;
import org.keycloak.authorization.protection.permission.representation.PermissionRequest;

/**
 * <p>An {@code {@link EntitlementRequest} represents a request sent to the server containing the permissions being requested.
 *
 * <p>Along with an entitlement request additional {@link AuthorizationRequestMetadata} information can be passed in order to define what clients expect from
 * the server when evaluating the requested permissions and when returning with a response.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class EntitlementRequest {

    private String rpt;
    private AuthorizationRequestMetadata metadata;

    private List<PermissionRequest> permissions = new ArrayList<>();

    /**
     * Returns the permissions being requested.
     *
     * @return the permissions being requested (not {@code null})
     */
    public List<PermissionRequest> getPermissions() {
        return permissions;
    }

    /**
     * Set the permissions being requested
     *
     * @param permissions the permissions being requests (not {@code null})
     */
    public void setPermissions(List<PermissionRequest> permissions) {
        this.permissions = permissions;
    }

    /**
     * Returns a {@code String} representing a previously issued RPT which permissions will be included the response in addition to the new ones being requested.
     *
     * @return a previously issued RPT (may be {@code null})
     */
    public String getRpt() {
        return rpt;
    }

    /**
     * A {@code String} representing a previously issued RPT which permissions will be included the response in addition to the new ones being requested.
     *
     * @param rpt a previously issued RPT. If {@code null}, only the requested permissions are evaluated
     */
    public void setRpt(String rpt) {
        this.rpt = rpt;
    }

    /**
     * Return the {@link Metadata} associated with this request.
     *
     * @return
     */
    public AuthorizationRequestMetadata getMetadata() {
        return metadata;
    }

    /**
     * The {@link Metadata} associated with this request. The metadata defines specific information that should be considered
     * by the server when evaluating and returning permissions.
     *
     * @param metadata the {@link Metadata} associated with this request (may be {@code null})
     */
    public void setMetadata(AuthorizationRequestMetadata metadata) {
        this.metadata = metadata;
    }
}
