<%@ page import="org.keycloak.common.util.KeycloakUriBuilder" %>
<%@ page import="org.keycloak.constants.ServiceUrlConstants" %>
<%@ page import="org.keycloak.testsuite.servletauthz.WebAppHelper" %>
<%
    String scheme = request.getScheme();
    String host = request.getServerName();
    int port = request.getServerPort();
    String contextPath = request.getContextPath();
    String redirectUri = scheme + "://" + host + ":" + port + contextPath;
%>
<h2>Click here <a href="<%= KeycloakUriBuilder.fromUri(WebAppHelper.getAuthUri()).path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH)
            .build("servlet-authz").toString()%>">Sign Out</a></h2>