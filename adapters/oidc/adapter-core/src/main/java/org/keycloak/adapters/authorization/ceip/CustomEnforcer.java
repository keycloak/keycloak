package org.keycloak.adapters.authorization.ceip;

import org.keycloak.AuthorizationContext;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.authorization.KeycloakAdapterPolicyEnforcer;
import org.jboss.logging.Logger;
import org.keycloak.adapters.authorization.PolicyEnforcer;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.ClientAuthorizationContext;
import org.keycloak.authorization.client.resource.PermissionResource;
import org.keycloak.authorization.client.resource.ProtectionResource;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import java.util.*;

public class CustomEnforcer extends KeycloakAdapterPolicyEnforcer {

    private static Logger LOGGER = Logger.getLogger(CustomEnforcer.class);
    private PolicyEnforcer customEnforcer;
    private static Object customEnforcerLock = new Object();
    private static CustomEnforcer CustomEnforcerInstance = null;
    private static final String HTTP_METHOD_DELETE = "DELETE";


    private CustomEnforcer(PolicyEnforcer policyEnforcer) {
        super(policyEnforcer);
        this.customEnforcer = policyEnforcer;
    }

    public static CustomEnforcer getCustomEnforcerInstance(PolicyEnforcer customPolicyEnforcer) {
        if (CustomEnforcerInstance != null) {
            return CustomEnforcerInstance;
        }
        synchronized (customEnforcerLock) {
            if (CustomEnforcerInstance == null) {
                CustomEnforcerInstance = new CustomEnforcer(customPolicyEnforcer);
            }
        }
        return CustomEnforcerInstance;
    }

    public AuthorizationContext authorize(OIDCHttpFacade httpFacade, Map<String,CustomEnforcerResource> permissionMap) {
        KeycloakSecurityContext securityContext = httpFacade.getSecurityContext();
        if (securityContext == null) {
            if (!isDefaultAccessDeniedUri(httpFacade.getRequest())) {
                if (!permissionMap.isEmpty()) {
                    challenge(permissionMap, httpFacade);
                } else {
                    handleAccessDenied(httpFacade);
                }
            }
            return createEmptyAuthorizationContext(false);
        }

        AccessToken accessToken = securityContext.getToken();
        if (accessToken != null) {
            if (isAuthorized(permissionMap, accessToken, httpFacade, null)) {
                try {
                    return createAuthorizationContext(accessToken, null);
                } catch (Exception e) {
                    throw new RuntimeException("Error processing path [" + httpFacade.getRequest().getURI() + "].", e);
                }
            }

            if (!challenge(permissionMap, httpFacade)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debugf("Challenge not sent, sending default forbidden response. Path [%s]", httpFacade.getRequest().getURI());
                }
                handleAccessDenied(httpFacade);
            }
        }

        return createEmptyAuthorizationContext(false);
    }

    public void createResource(ResourceRepresentation resource) {
        if (!resource.getName().isEmpty()) {
            AuthzClient authzclient = getPolicyEnforcer().getClient();
            String token = getPolicyEnforcer().getClient().obtainAccessToken().getToken();
            resource = authzclient.protection(token).resource().create(resource);
        }
    }

    public void deleteResource(String resourceName) {
        AuthzClient authzclient = getPolicyEnforcer().getClient();
        String token = getPolicyEnforcer().getClient().obtainAccessToken().getToken();
        if (getPolicyEnforcer().getPaths().containsKey(resourceName)) {
            String resourceId = getPolicyEnforcer().getPaths().get(resourceName).getId();
            authzclient.protection(token).resource().delete(resourceId);
        }
    }

    protected boolean challenge(Map<String,CustomEnforcerResource> permissionMap, OIDCHttpFacade httpFacade) {
        HttpFacade.Response response = httpFacade.getResponse();
        AuthzClient authzClient = getAuthzClient();
        String ticket = getPermissionTicket(permissionMap, authzClient);
        if (ticket != null) {
            response.setStatus(401);
            response.setHeader("WWW-Authenticate", new StringBuilder("UMA realm=\"").append(authzClient.getConfiguration().getRealm()).append("\"").append(",as_uri=\"")
                    .append(authzClient.getServerConfiguration().getIssuer()).append("\"").append(",ticket=\"").append(ticket).append("\"").toString());
        } else {
            response.setStatus(403);
        }
        return true;
    }

    protected AuthorizationContext createAuthorizationContext(AccessToken
                                                                    accessToken, PolicyEnforcerConfig.PathConfig pathConfig) {
        return new ClientAuthorizationContext(accessToken, pathConfig, getAuthzClient());
    }

    protected String getPermissionTicket(Map<String,CustomEnforcerResource> permissionMap, AuthzClient
            authzClient) {
        if (customEnforcer.getEnforcerConfig().getUserManagedAccess() != null) {
            ProtectionResource protection = authzClient.protection();
            PermissionResource permission = protection.permission();
            List<PermissionRequest> permissionRequests = new ArrayList<PermissionRequest>();
            for (Map.Entry<String, CustomEnforcerResource> resource : permissionMap.entrySet()) {
                PermissionRequest permissionRequest = new PermissionRequest();
                permissionRequest.setResourceId(resource.getKey());
                permissionRequest.setScopes(resource.getValue().getScopes());
                permissionRequests.add(permissionRequest);

            }
            if (!permissionMap.isEmpty())
                return permission.create(permissionRequests).getTicket();
        }
        return null;
    }

    protected boolean isAuthorized(Map<String, CustomEnforcerResource> permissionMap, AccessToken
            accessToken, OIDCHttpFacade httpFacade, Map<String, List<String>> claims) {
        HttpFacade.Request request = httpFacade.getRequest();

        if (isDefaultAccessDeniedUri(request)) {
            return true;
        }

        AccessToken.Authorization authorization = accessToken.getAuthorization();

        if (authorization == null) {
            return false;
        }

        Collection<Permission> grantedPermissions = authorization.getPermissions();
        HashMap<String, Permission> grantedPermissionMap = new HashMap<>();
        for (Permission permission : grantedPermissions) {
            grantedPermissionMap.put(permission.getResourceId(), permission);
        }

        for (Map.Entry<String, CustomEnforcerResource> resource : permissionMap.entrySet()) {
            if (!grantedPermissionMap.containsKey(resource.getKey()))
                return false;
            PolicyEnforcerConfig.MethodConfig methodConfig = new PolicyEnforcerConfig.MethodConfig();
            methodConfig.setScopes (new ArrayList<>(resource.getValue().getScopes()));
            if (hasResourceScopePermission(methodConfig, grantedPermissionMap.get(resource.getKey()))) {
                if (HTTP_METHOD_DELETE.equalsIgnoreCase(request.getMethod())) {
                    getPolicyEnforcer().getPathMatcher().removeFromCache(request.getRelativePath());
                }
                return true;
            }
        }
        return false;
    }

    protected boolean hasResourceScopePermission(PolicyEnforcerConfig.MethodConfig methodConfig, Permission
            permission) {
        List<String> requiredScopes = methodConfig.getScopes();
        Set<String> allowedScopes = permission.getScopes();

        if (allowedScopes.isEmpty()) {
            return true;
        }
        return allowedScopes.containsAll(requiredScopes);
    }

    private AuthorizationContext createEmptyAuthorizationContext(final boolean granted) {
        return new ClientAuthorizationContext(getAuthzClient()) {
            @Override
            public boolean hasPermission(String resourceName, String scopeName) {
                return granted;
            }

            @Override
            public boolean hasResourcePermission(String resourceName) {
                return granted;
            }

            @Override
            public boolean hasScopePermission(String scopeName) {
                return granted;
            }

            @Override
            public List<Permission> getPermissions() {
                return Collections.EMPTY_LIST;
            }

            @Override
            public boolean isGranted() {
                return granted;
            }
        };
    }

    private boolean isDefaultAccessDeniedUri(HttpFacade.Request request) {
        String accessDeniedPath = getEnforcerConfig().getOnDenyRedirectTo();
        return accessDeniedPath != null && request.getURI().contains(accessDeniedPath);
    }



}
