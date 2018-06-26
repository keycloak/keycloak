<%@page import="org.keycloak.AuthorizationContext" %>
<%@ page import="org.keycloak.KeycloakSecurityContext" %>
<%@ page import="org.keycloak.representations.idm.authorization.Permission" %>

<%
    KeycloakSecurityContext keycloakSecurityContext = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
    AuthorizationContext authzContext = keycloakSecurityContext.getAuthorizationContext();
%>

<html>
<body>
    <%@include file="logout-include.jsp"%>
    <h2>This is a public resource. Try to access one of these <i>protected</i> resources:</h2>

    <p><a href="protected/dynamicMenu.jsp">Dynamic Menu</a></p>
    <p><a href="protected/premium/onlyPremium.jsp">User Premium</a></p>
    <p><a href="protected/premium/pep-disabled.jsp">PEP Disabled</a></p>
    <p><a href="protected/admin/onlyAdmin.jsp">Administration</a></p>

    <h3>Your permissions are:</h3>

    <ul>
        <%
            for (Permission permission : authzContext.getPermissions()) {
        %>
        <li>
            <p>Resource: <%= permission.getResourceName() %></p>
            <p>ID: <%= permission.getResourceId() %></p>
            <p>Scopes: <%= permission.getScopes() %></p>
        </li>
        <%
            }
        %>
    </ul>
</body>
</html>
