<%@page import="org.keycloak.AuthorizationContext" %>
<%@ page import="org.keycloak.common.util.KeycloakUriBuilder" %>
<%@ page import="org.keycloak.constants.ServiceUrlConstants" %>
<%@ page import="org.keycloak.KeycloakSecurityContext" %>
<%@ page import="org.keycloak.representations.authorization.Permission" %>

<%
    KeycloakSecurityContext keycloakSecurityContext = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
    AuthorizationContext authzContext = keycloakSecurityContext.getAuthorizationContext();
%>

<html>
<body>
    <h2>Click <a href="<%= KeycloakUriBuilder.fromUri("/auth").path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH)
            .queryParam("redirect_uri", "/servlet-authz-app").build("servlet-authz").toString()%>">here</a> to logout.</h2>
    <h2>This is a public resource. Try to access one of these <i>protected</i> resources:</h2>

    <p><a href="protected/dynamicMenu.jsp">Dynamic Menu</a></p>
    <p><a href="protected/premium/onlyPremium.jsp">User Premium</a></p>
    <p><a href="protected/admin/onlyAdmin.jsp">Administration</a></p>

    <h3>Your permissions are:</h3>

    <ul>
        <%
            for (Permission permission : authzContext.getPermissions()) {
        %>
        <li>
            <p>Resource: <%= permission.getResourceSetName() %></p>
            <p>ID: <%= permission.getResourceSetId() %></p>
            <p>Scopes: <%= permission.getScopes() %></p>
        </li>
        <%
            }
        %>
    </ul>
</body>
</html>
