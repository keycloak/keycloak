<%@ page import="org.keycloak.services.models.*,org.keycloak.services.resources.*,javax.ws.rs.core.*" language="java" contentType="text/html; charset=ISO-8859-1"
 pageEncoding="ISO-8859-1"%>
<%
        RealmModel realm = (RealmModel)request.getAttribute(RealmModel.class.getName());
        String username = (String)request.getAttribute("username");
%>
<!doctype html>
<html lang="en">

<head>
    <meta charset="utf-8">
    <title>Keycloak Realm Login Page</title>

    <link rel="shortcut icon" type="image/x-icon" href="<%=application.getContextPath()%>/img/favicon.ico">

    <link href="<%=application.getContextPath()%>/lib/bootstrap/css/bootstrap.css" rel="stylesheet">
    <link href="<%=application.getContextPath()%>/lib/font-awesome/css/font-awesome.css" rel="stylesheet">
    <link href="<%=application.getContextPath()%>/css/reset.css" rel="stylesheet">
    <link href="<%=application.getContextPath()%>/css/base.css" rel="stylesheet">
</head>

<body>
<div class="modal-body">

    <div id="facebook-login-box" onclick="loginFacebook()">
        <img id="facebook-sign" src="<%=application.getContextPath()%>/img/facebook.png" border="0"/>
    </div>

    <div id="twitter-login-box" onclick="loginTwitter()">
        <img id="twitter-sign" src="<%=application.getContextPath()%>/img/twitter.png" border="0"/>
    </div>

    <hr/>
    <% String errorMessage = (String)request.getAttribute("KEYCLOAK_LOGIN_ERROR_MESSAGE");
       if (errorMessage != null) { %>
    <div id="error-message" class="alert alert-block alert-error" style="block"><%=errorMessage%></div>
    <% } %>
    <form class="form-horizontal" name="loginForm" action="<%=request.getAttribute("KEYCLOAK_LOGIN_ACTION")%>" method="POST">
        <div class="control-group">
            <label class="control-label" for="username">User Name</label>

            <div class="controls">
            <% if (username != null) { %>
                <input type="text" name="username" value="<%=username%>">
            <% } else { %>
                <input type="text" name="username" placeholder="User Name">
            <% } %>
            </div>
        </div>
        <%
        for (RequiredCredentialModel credential : realm.getRequiredCredentials()) {
            %>
        <div class="control-group">
            <%
            if (!credential.isInput()) continue;
            %>
            <label class="control-label" for="<%=credential.getType()%>"><%=credential.getType()%></label>
            <%
            if (credential.isSecret()) {
                %>
            <div class="controls">
                <input type="password" name="<%=credential.getType()%>" placeholder="<%=credential.getType()%>">
            </div>
                <%
            } else {
                %>
             <div class="controls">
                 <input type="text" name="<%=credential.getType()%>" placeholder="<%=credential.getType()%>">
             </div>
                <%
            }
            %>
            </div>
            <%
        }
        %>
       <input type="hidden" name="client_id" value="<%=request.getAttribute("client_id")%>">");
       <%
       String scopeParam = (String)request.getAttribute("scope");
       if (scopeParam != null) { %>
       <input type="hidden" name="scope" value="<%=scopeParam%>">
       <% } %>
       <%
       String stateParam = (String)request.getAttribute("state");
       if (stateParam != null) { %>
       <input type="hidden" name="state" value="<%=stateParam%>">
       <% } %>
       <input type="hidden" name="redirect_uri" value="<%=request.getAttribute("redirect_uri")%>">
        <div class="control-group">
            <div class="controls">
                <button class="btn btn-primary">Login</button>
            </div>
        </div>
    </form>
</div>
<footer>
  <p>Powered By Keycloak</p>
</body>
</html>
