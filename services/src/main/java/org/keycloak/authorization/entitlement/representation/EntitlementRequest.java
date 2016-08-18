package org.keycloak.authorization.entitlement.representation;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.authorization.protection.permission.representation.PermissionRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class EntitlementRequest {

    private String rpt;

    private List<PermissionRequest> permissions = new ArrayList<>();

    public List<PermissionRequest> getPermissions() {
        return permissions;
    }

    public String getRpt() {
        return rpt;
    }
}
