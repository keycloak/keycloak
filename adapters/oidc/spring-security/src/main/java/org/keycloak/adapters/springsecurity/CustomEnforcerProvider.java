package org.keycloak.adapters.springsecurity;

import org.jboss.logging.Logger;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.authorization.CustomEnforcerInformationProvider;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.adapters.authorization.ceip.CustomEnforcer;
import org.keycloak.adapters.authorization.ceip.CustomEnforcerResource;
import org.keycloak.adapters.springsecurity.facade.SimpleHttpFacade;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class CustomEnforcerProvider extends KeycloakDeploymentBuilder implements CustomEnforcerInformationProvider {

    private static PolicyEnforcer customPolicyEnforcer = null;
    private static Logger LOGGER = Logger.getLogger(CustomEnforcerProvider.class);
    private static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private static Object customEnforcerLock = new Object();
    private static CustomEnforcerProvider customEnforcerProvider;
//    private boolean auditLogFlag = true;

    private CustomEnforcerProvider(){
        getPolicyEnforcer();
    }

    public void authorize(HttpServletRequest request, HttpServletResponse response, Map<String, Set<String>> permissionMap) {
        try {
            OIDCHttpFacade httpFacade = new SimpleHttpFacade(request, response);
            CustomEnforcer customEnforcer = CustomEnforcer.getCustomEnforcerInstance(customPolicyEnforcer);
            Map<String,CustomEnforcerResource> resolvedMap = resolvePermissionMap(permissionMap);
            customEnforcer.authorize(httpFacade, resolvedMap);
            if(customPolicyEnforcer.getEnforcerConfig().getCustomEnforcerConfig().isAuditLoggingEnabled())
                writeAuditLogs(request, response, resolvedMap);

        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    public void createResource(ResourceRepresentation resource) {
        if (!resource.getName().isEmpty()) {
            CustomEnforcer.getCustomEnforcerInstance(customPolicyEnforcer).createResource(resource);
        }
    }

    public void deleteResource(String resourceName) {
        if (!resourceName.isEmpty() && customPolicyEnforcer.getPaths().containsKey(resourceName)) {
            CustomEnforcer.getCustomEnforcerInstance(customPolicyEnforcer).deleteResource(resourceName);
        }
    }

    public static CustomEnforcerProvider getCustomEnforcerProvider(){
        if (customEnforcerProvider != null) {
            return customEnforcerProvider;
        }
        synchronized (customEnforcerLock) {
            if (customEnforcerProvider == null) {
                customEnforcerProvider = new CustomEnforcerProvider();
            }
        }
        return customEnforcerProvider;
    }

    protected Map<String, CustomEnforcerResource> resolvePermissionMap(Map<String, Set<String>> permissionMap) {
        Map<String, CustomEnforcerResource> resolvedMap = new HashMap();
        try {
            for (Map.Entry<String, Set<String>> resource : permissionMap.entrySet()) {
                if (customPolicyEnforcer.getPathMatcher().matches(resource.getKey()) != null) {
                    PolicyEnforcerConfig.PathConfig pathConfig = customPolicyEnforcer.getPathMatcher().matches(resource.getKey());
                    Set<String> requestedScopes =  resource.getValue();
                    if(pathConfig.getScopes().containsAll(requestedScopes)) {
                        CustomEnforcerResource customResource = new CustomResource(pathConfig.getName(),resource.getValue());
                        resolvedMap.put(pathConfig.getId(), customResource);
                    }
                    else
                        throw new RuntimeException("Scope specified for resource "+ pathConfig.getPath() + " does not exist");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return resolvedMap;
    }

    private PolicyEnforcer getPolicyEnforcer() {
        if(customPolicyEnforcer!=null)
            return customPolicyEnforcer;
        try {
            InputStream configStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("keycloak.json");
            AdapterConfig adapterConfig = loadAdapterConfig(configStream);
            KeycloakDeployment deploy = internalBuild(adapterConfig);
            customPolicyEnforcer = deploy.getPolicyEnforcer();
            if(!customPolicyEnforcer.getEnforcerConfig().getCustomEnforcerConfig().isCustomEnforcerEnabled())
                throw new RuntimeException("Custom Enforcer is not set in keycloak.json");


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return customPolicyEnforcer;
    }

    protected void writeAuditLogs(HttpServletRequest request, HttpServletResponse response, Map<String, CustomEnforcerResource> permissionMap) {
        AccessToken accessToken = null;
        String user = "unknown";
        KeycloakSecurityContext securityContext = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
        if(securityContext!=null)
            accessToken = securityContext.getToken();
        if(accessToken!=null)
            user = accessToken.getPreferredUsername();
        StringBuilder action = new StringBuilder();
        for (Map.Entry<String, CustomEnforcerResource> resource : permissionMap.entrySet()) {
            action.append("Resource:# " + resource.getValue().getResourceName() + " scopes : " + resource.getValue().getScopes() + ", ");
        }
        LOGGER.info("ip: "+ getClientIpAddress(request)+ ", resource: "+request.getRequestURI()+ ", status: "+response.getStatus() + ", user: " + user + ", action: "+action+", date: "+ dateFormat.format(new Date()));
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-FORWARDED-FOR");
        if (xForwardedForHeader == null) {
            return request.getRemoteAddr();
        } else {
            return new StringTokenizer(xForwardedForHeader, ",").nextToken().trim();
        }
    }

    class CustomResource implements CustomEnforcerResource {
        private String name;
        private Set<String> scopes;

        CustomResource(String name, Set<String> scopes){
            this.name=name;
            this.scopes=scopes;
        }

        public String getResourceName(){
            return name;
        }
        public Set<String> getScopes(){
            return scopes;
        }
    }
}
