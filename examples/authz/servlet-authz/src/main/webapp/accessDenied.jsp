<%@ page import="org.keycloak.constants.ServiceUrlConstants" %>
<%@ page import="org.keycloak.common.util.KeycloakUriBuilder" %>
<html>
    <body>
        <h2 style="color: red">You can not access this resource. Click <a href="<%= KeycloakUriBuilder.fromUri("/auth").path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH)
            .queryParam("redirect_uri", "/servlet-authz-app").build("servlet-authz").toString()%>">here</a> to log in as a different user.</h2>
    </body>
</html>