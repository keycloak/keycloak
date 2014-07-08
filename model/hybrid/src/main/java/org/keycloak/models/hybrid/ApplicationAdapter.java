package org.keycloak.models.hybrid;

import org.keycloak.models.realms.Application;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ApplicationAdapter extends ClientAdapter implements ApplicationModel {

    private Application application;

    ApplicationAdapter(HybridModelProvider provider, Application application) {
        super(provider, application);
        this.application = application;
    }

    Application getApplication() {
        return application;
    }

    @Override
    public void updateApplication() {
        application.updateApplication();
    }

    @Override
    public String getName() {
        return application.getName();
    }

    @Override
    public void setName(String name) {
        application.setName(name);
    }

    @Override
    public boolean isSurrogateAuthRequired() {
        return application.isSurrogateAuthRequired();
    }

    @Override
    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        application.setSurrogateAuthRequired(surrogateAuthRequired);
    }

    @Override
    public String getManagementUrl() {
        return application.getManagementUrl();
    }

    @Override
    public void setManagementUrl(String url) {
        application.setManagementUrl(url);
    }

    @Override
    public String getBaseUrl() {
        return application.getBaseUrl();
    }

    @Override
    public void setBaseUrl(String url) {
        application.setBaseUrl(url);
    }

    @Override
    public List<String> getDefaultRoles() {
        return application.getDefaultRoles();
    }

    @Override
    public void addDefaultRole(String name) {
        if (getRole(name) == null) {
            addRole(name);
        }

        application.addDefaultRole(name);
    }

    @Override
    public void updateDefaultRoles(String[] defaultRoles) {
        application.updateDefaultRoles(defaultRoles);
    }

    @Override
    public Set<RoleModel> getApplicationScopeMappings(ClientModel client) {
        return provider.mappings().wrap(application.getApplicationScopeMappings(provider.mappings().unwrap(client)));
    }

    @Override
    public boolean isBearerOnly() {
        return application.isBearerOnly();
    }

    @Override
    public void setBearerOnly(boolean only) {
        application.setBearerOnly(only);
    }

    @Override
    public RoleModel getRole(String name) {
        return provider.mappings().wrap(application.getRole(name));
    }

    @Override
    public RoleModel addRole(String name) {
        return addRole(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RoleModel addRole(String id, String name) {
        return provider.mappings().wrap(application.addRole(id, name));
    }

    @Override
    public boolean removeRole(RoleModel role) {
        if (application.removeRole(provider.mappings().unwrap(role))) {
            provider.users().onRoleRemoved(role.getId());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Set<RoleModel> getRoles() {
        return provider.mappings().wrap(application.getRoles());
    }

}
