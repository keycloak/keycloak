<%@ page import="org.keycloak.constants.ServiceUrlConstants" %>
<%@ page import="org.keycloak.common.util.KeycloakUriBuilder" %>
<html>
<body>
    <h2>Only Administrators can access this page. Click <a href="<%= KeycloakUriBuilder.fromUri("/auth").path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH)
            .queryParam("redirect_uri", "/servlet-authz-app").build("servlet-authz").toString()%>">here</a> to logout.</h2></h2>
</body>
</html>