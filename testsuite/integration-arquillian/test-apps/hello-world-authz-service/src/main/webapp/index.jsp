<%--
  ~  Copyright 2016 Red Hat, Inc. and/or its affiliates
  ~  and other contributors as indicated by the @author tags.
  ~
  ~  Licensed under the Apache License, Version 2.0 (the "License");
  ~  you may not use this file except in compliance with the License.
  ~  You may obtain a copy of the License at
  ~
  ~  http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~  Unless required by applicable law or agreed to in writing, software
  ~  distributed under the License is distributed on an "AS IS" BASIS,
  ~  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~  See the License for the specific language governing permissions and
  ~  limitations under the License.
  ~
  --%>
<%@page import="org.keycloak.AuthorizationContext" %>
<%@ page import="org.keycloak.KeycloakSecurityContext" %>
<%@ page import="org.keycloak.common.util.KeycloakUriBuilder" %>
<%@ page import="org.keycloak.constants.ServiceUrlConstants" %>
<%@ page import="org.keycloak.representations.idm.authorization.Permission" %>

<%
    KeycloakSecurityContext keycloakSecurityContext = (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
    AuthorizationContext authzContext = keycloakSecurityContext.getAuthorizationContext();

    boolean isTLSEnabled = Boolean.parseBoolean(System.getProperty("auth.server.ssl.required", "true"));
    String authPort = isTLSEnabled ? System.getProperty("auth.server.https.port", "8543") : System.getProperty("auth.server.http.port", "8180");
    String authScheme = isTLSEnabled ? "https" : "http";
    String authUri = authScheme + "://localhost:" + authPort + "/auth";
%>
<html>
<body>
<h2>Welcome !</h2>
<h2><a href="<%= KeycloakUriBuilder.fromUri(authUri).path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH)
            .queryParam("redirect_uri", "http://localhost:8080/hello-world-authz-service").build("hello-world-authz").toString()%>">Logout</a></h2>

<h3>Your permissions are:</h3>

<ul>
    <%
        for (Permission permission : authzContext.getPermissions()) {
    %>
    <li>
        <p>Resource: <%= permission.getResourceName() %></p>
        <p>ID: <%= permission.getResourceId() %></p>
    </li>
    <%
        }
    %>
</ul>
</body>
</html>