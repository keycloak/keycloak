<%@ page import="org.keycloak.common.util.KeycloakUriBuilder" %>
<%@ page import="org.keycloak.constants.ServiceUrlConstants" %>
<%
    String scheme = request.getScheme();
    String host = request.getServerName();
    int port = request.getServerPort();
    String contextPath = request.getContextPath();
    String redirectUri = scheme + "://" + host + ":" + port + contextPath;


    boolean isTLSEnabled = Boolean.parseBoolean(System.getProperty("auth.server.ssl.required", "true"));
    String authPort = isTLSEnabled ? System.getProperty("auth.server.https.port", "8543") : System.getProperty("auth.server.http.port", "8180");
    String authScheme = isTLSEnabled ? "https" : "http";
    String authHost = System.getProperty("auth.server.host", "localhost");
    String authUri = authScheme + "://" + authHost + ":" + authPort + "/auth";
%>
<h2>Click here <a href="<%= KeycloakUriBuilder.fromUri(authUri).path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH)
            .build("servlet-policy-enforcer-authz").toString()%>">Sign Out</a></h2>