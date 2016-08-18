<%@page import="org.keycloak.AuthorizationContext" %>
<%@ page import="org.keycloak.KeycloakSecurityContext" %>

<%
    KeycloakSecurityContext keycloakSecurityContext = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
    AuthorizationContext authzContext = keycloakSecurityContext.getAuthorizationContext();
%>

<html>
<body>
<h2>Any authenticated user can access this page.</h2>
<%@include file="../logout-include.jsp"%>

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