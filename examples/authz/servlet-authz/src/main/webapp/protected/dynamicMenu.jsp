<%@page import="org.keycloak.AuthorizationContext" %>
<%@ page import="org.keycloak.common.util.KeycloakUriBuilder" %>
<%@ page import="org.keycloak.constants.ServiceUrlConstants" %>
<%@ page import="org.keycloak.KeycloakSecurityContext" %>

<%
    KeycloakSecurityContext keycloakSecurityContext = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
    AuthorizationContext authzContext = keycloakSecurityContext.getAuthorizationContext();
%>

<html>
<body>
<h2>Any authenticated user can access this page. Click <a href="<%= KeycloakUriBuilder.fromUri("/auth").path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH)
            .queryParam("redirect_uri", "/servlet-authz-app").build("servlet-authz").toString()%>">here</a> to logout.</h2>

<p>Here is a dynamic menu built from the permissions returned by the server:</p>

<ul>
    <%
        if (authzContext.hasResourcePermission("Protected Resource")) {
    %>
    <li>
        Do user thing
    </li>
    <%
        }
    %>

    <%
        if (authzContext.hasResourcePermission("Premium Resource")) {
    %>
    <li>
        Do  user premium thing
    </li>
    <%
        }
    %>

    <%
        if (authzContext.hasPermission("Admin Resource", "urn:servlet-authz:protected:admin:access")) {
    %>
    <li>
        Do administration thing
    </li>
    <%
        }
    %>
</ul>
</body>
</html>