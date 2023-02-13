<%@page import="org.keycloak.AuthorizationContext" %>
<%@ page import="org.keycloak.KeycloakSecurityContext" %>

<%
    KeycloakSecurityContext keycloakSecurityContext = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
    AuthorizationContext authzContext = keycloakSecurityContext.getAuthorizationContext();
%>

<html>
<body>
<h2>Access granted: <%= authzContext.isGranted() %></h2>
<%@include file="../../logout-include.jsp"%>
</body>
</html>