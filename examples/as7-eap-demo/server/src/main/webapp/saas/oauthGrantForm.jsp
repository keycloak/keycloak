<%@ page import="org.keycloak.services.models.*,org.keycloak.services.resources.*,javax.ws.rs.core.*,java.util.*" language="java" contentType="text/html; charset=ISO-8859-1"
 pageEncoding="ISO-8859-1"%>
<%
        RealmModel realm = (RealmModel)request.getAttribute(RealmModel.class.getName());
        String username = (String)request.getAttribute("username");
%>
<!doctype html>
<html lang="en">

<head>
    <meta charset="utf-8">
    <title>Keycloak</title>

    <link rel="stylesheet" href="<%=application.getContextPath()%>/saas/css/reset.css">
    <link rel="stylesheet" type="text/css" href="<%=application.getContextPath()%>/saas/css/base.css">
    <link rel="stylesheet" type="text/css" href="<%=application.getContextPath()%>/saas/css/forms.css">
    <link rel="stylesheet" type="text/css" href="<%=application.getContextPath()%>/saas/css/zocial/zocial.css">
    <link rel="stylesheet" type="text/css" href="<%=application.getContextPath()%>/saas/css/login-screen.css">
    <link rel="stylesheet" type="text/css" href='http://fonts.googleapis.com/css?family=Open+Sans:400,300,300italic,400italic,600,600italic,700,700italic,800,800italic'>
</head>
<%
    UserModel client = (UserModel)request.getAttribute("client");
    List<RoleModel> realmRolesRequested = (List<RoleModel>)request.getAttribute("realmRolesRequested");
    MultivaluedMap<String, RoleModel> resourceRolesRequested = (MultivaluedMap<String, RoleModel>)request.getAttribute("resourceRolesRequested");
%>

<body class="rcue-login-register register">
<h1><a href="#" title="Go to the home page"><img src="<%=application.getContextPath()%>/saas/img/red-hat-logo.png" alt="Red Hat logo"></a></h1>
<div class="content">
    <h2>Grant request for <strong><%=client.getLoginName()%></strong></h2>
    <div class="background-area">
        <div class="form-area social clearfix">
            <section class="info-area">
                <p>This app would like to:</p>
                <hr/>
                <ul>
                <%
                    if (realmRolesRequested.size() > 0) {
                        for (RoleModel role : realmRolesRequested) {
                            %> <li> <%
                            String desc = "Have " + role.getName() + " privileges.";
                            String roleDesc = role.getDescription();
                            if (roleDesc != null) {
                                desc = roleDesc;
                            }
                %>
                <p><%=desc%></p>
                </li>
                <%
                        }
                    }
                %>
                </ul>

                <%
                    for (String resource : resourceRolesRequested.keySet()) { %>
                <hr/>
                <%
                        List<RoleModel> roles = resourceRolesRequested.get(resource);
                        out.println("<p>For application " + resource + ":</p> ");
                        out.println("<ul>");
                        for (RoleModel role : roles) {
                            String desc = "Have " + role.getName() + " privileges.";
                            String roleDesc = role.getDescription();
                            if (roleDesc != null) {
                                desc = roleDesc;
                            }
                            out.println("<li>" + desc + "</li>");
                        }
                        %> </ul> <%
                    }
                %>
                <form action="<%=request.getAttribute("action")%>" method="POST">
                    <input type="hidden" name="code" value="<%=request.getAttribute("code")%>">
                    <input type="submit" name="accept" value="Accept">
                    <input type="submit" name="cancel" value="Cancel">
                </form>
            </section>
         </div>
    </div>
</div>
<footer>
    <p>Powered By Keycloak</p>
</footer>
</body>
</html>
