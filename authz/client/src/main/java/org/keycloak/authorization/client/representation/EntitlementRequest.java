package org.keycloak.authorization.client.representation;

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

    public void setRpt(String rpt) {
        this.rpt = rpt;
    }

    public void setPermissions(List<PermissionRequest> permissions) {
        this.permissions = permissions;
    }

    public void addPermission(PermissionRequest request) {
        getPermissions().add(request);
    }
}
